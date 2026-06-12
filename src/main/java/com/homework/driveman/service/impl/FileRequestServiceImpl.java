package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.FileRequest;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.FileRequestMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.IFileRequestService;
import com.homework.driveman.web.ServiceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileRequestServiceImpl extends ServiceImpl<FileRequestMapper, FileRequest>
        implements IFileRequestService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public FileRequest create(Integer requesterId, Integer targetUserId, String title,
                              String description, String bizType, Integer bizId,
                              String fileType, String remark, String deadline) {
        // 防重复：同一业务关联不允许重复创建待提交请求
        if (bizType != null && bizId != null) {
            Long count = lambdaQuery()
                    .eq(FileRequest::getBizType, bizType)
                    .eq(FileRequest::getBizId, bizId)
                    .eq(FileRequest::getStatus, 0)
                    .count();
            if (count > 0) {
                throw new ServiceException(ServiceCode.ERROR_CONFLICT,
                        "该业务已有待提交的文件请求，请勿重复创建");
            }
        }

        FileRequest request = new FileRequest();
        request.setRequesterId(requesterId);
        request.setTargetUserId(targetUserId);
        request.setTitle(title);
        request.setDescription(description);
        request.setBizType(bizType);
        request.setBizId(bizId);
        request.setFileType(fileType != null ? fileType : "general");
        request.setStatus(0);
        request.setRemark(remark);
        if (deadline != null && !deadline.isEmpty()) {
            request.setDeadline(LocalDate.parse(deadline));
        }
        save(request);
        return request;
    }

    @Override
    public void markCompleted(Integer requestId) {
        FileRequest request = getById(requestId);
        if (request == null) return;
        if (request.getStatus() == 0) {
            request.setStatus(1);
            updateById(request);
        }
    }

    @Override
    public void cancel(Integer requestId) {
        FileRequest request = getById(requestId);
        if (request == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "请求不存在");
        }
        request.setStatus(2);
        updateById(request);
    }

    @Override
    public Page<Map<String, Object>> pageByTargetUser(Page<FileRequest> page, Integer targetUserId) {
        LambdaQueryWrapper<FileRequest> wrapper = new LambdaQueryWrapper<FileRequest>()
                .eq(FileRequest::getTargetUserId, targetUserId)
                .orderByDesc(FileRequest::getStatus)
                .orderByDesc(FileRequest::getCreateTime);
        return toPageResult(page(page, wrapper));
    }

    @Override
    public Page<Map<String, Object>> pageAll(Page<FileRequest> page, String targetName, Integer status) {
        LambdaQueryWrapper<FileRequest> wrapper = new LambdaQueryWrapper<>();

        if (targetName != null && !targetName.isEmpty()) {
            List<Integer> userIds = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .like(User::getRealName, targetName)
                            .select(User::getUserId)
            ).stream().map(User::getUserId).toList();
            if (userIds.isEmpty()) {
                return new Page<>(page.getCurrent(), page.getSize(), 0);
            }
            wrapper.in(FileRequest::getTargetUserId, userIds);
        }
        if (status != null) {
            wrapper.eq(FileRequest::getStatus, status);
        }
        wrapper.orderByAsc(FileRequest::getStatus)
               .orderByDesc(FileRequest::getCreateTime);
        return toPageResult(page(page, wrapper));
    }

    @Override
    public int countPending(Integer targetUserId) {
        Long result = lambdaQuery()
                .eq(FileRequest::getTargetUserId, targetUserId)
                .eq(FileRequest::getStatus, 0)
                .count();
        return result != null ? result.intValue() : 0;
    }

    private Page<Map<String, Object>> toPageResult(Page<FileRequest> rawPage) {
        List<FileRequest> records = rawPage.getRecords();
        if (records.isEmpty()) {
            return new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        }

        Set<Integer> userIds = new HashSet<>();
        records.forEach(r -> {
            userIds.add(r.getRequesterId());
            userIds.add(r.getTargetUserId());
        });
        Map<Integer, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        List<Map<String, Object>> result = records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("requesterId", r.getRequesterId());
            m.put("targetUserId", r.getTargetUserId());
            m.put("title", r.getTitle());
            m.put("description", r.getDescription());
            m.put("bizType", r.getBizType());
            m.put("bizId", r.getBizId());
            m.put("fileType", r.getFileType());
            m.put("status", r.getStatus());
            m.put("remark", r.getRemark());
            m.put("deadline", r.getDeadline());
            m.put("createTime", r.getCreateTime());
            User requester = userMap.get(r.getRequesterId());
            m.put("requesterName", requester != null ? requester.getRealName() : "未知");
            User target = userMap.get(r.getTargetUserId());
            m.put("targetUserName", target != null ? target.getRealName() : "未知");
            return m;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> resultPage = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        resultPage.setRecords(result);
        return resultPage;
    }
}
