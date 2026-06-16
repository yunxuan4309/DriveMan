package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.DisabilityInfo;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.DisabilityInfoMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.IDisabilityInfoService;
import com.homework.driveman.web.ServiceCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 残疾人信息服务实现（简化版）
 */
@Slf4j
@Service
public class DisabilityInfoServiceImpl extends ServiceImpl<DisabilityInfoMapper, DisabilityInfo> implements IDisabilityInfoService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public DisabilityInfo submit(Integer userId, Integer disabilityType, String certificateNo,
                                 Integer certificateFileId) {
        // 校验参数
        if (disabilityType == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "残疾类型不能为空");
        }
        if (certificateNo == null || certificateNo.isEmpty()) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "残疾人证号不能为空");
        }
        if (certificateFileId == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "请上传残疾人证扫描件");
        }

        // 检查是否已提交过
        DisabilityInfo existing = getByUserId(userId);
        if (existing != null) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "您已提交过残疾信息，请勿重复提交");
        }

        DisabilityInfo info = new DisabilityInfo();
        info.setUserId(userId);
        info.setDisabilityType(disabilityType);
        info.setCertificateNo(certificateNo);
        info.setCertificateFileId(certificateFileId);
        info.setAuditStatus(0); // 待审核
        save(info);

        log.info("残疾信息提交成功: userId={}, disabilityType={}", userId, disabilityType);
        return info;
    }

    @Override
    public DisabilityInfo getByUserId(Integer userId) {
        return lambdaQuery()
                .eq(DisabilityInfo::getUserId, userId)
                .one();
    }

    @Override
    public void audit(Integer id, Integer auditStatus, String auditRemark) {
        if (auditStatus == null || (auditStatus != 1 && auditStatus != 2)) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "审核状态只能为 1(通过) 或 2(不通过)");
        }
        DisabilityInfo info = getById(id);
        if (info == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "残疾信息不存在");
        }
        if (info.getAuditStatus() != 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该记录已审核，请勿重复操作");
        }

        info.setAuditStatus(auditStatus);
        info.setAuditRemark(auditRemark);
        info.setAuditTime(LocalDateTime.now());
        updateById(info);

        log.info("残疾信息审核完成: id={}, auditStatus={}", id, auditStatus);
    }

    @Override
    public List<DisabilityInfo> listPending() {
        return lambdaQuery()
                .eq(DisabilityInfo::getAuditStatus, 0)
                .orderByDesc(DisabilityInfo::getCreateTime)
                .list();
    }

    @Override
    public boolean isAuditPassed(Integer userId) {
        DisabilityInfo info = getByUserId(userId);
        return info != null && info.getAuditStatus() != null && info.getAuditStatus() == 1;
    }

    @Override
    public Page<Map<String, Object>> pageWithDetails(Page<DisabilityInfo> page, Integer auditStatus, String keyword) {
        LambdaQueryWrapper<DisabilityInfo> wrapper = new LambdaQueryWrapper<DisabilityInfo>()
                .eq(auditStatus != null, DisabilityInfo::getAuditStatus, auditStatus)
                .orderByDesc(DisabilityInfo::getCreateTime);

        // 按学员姓名搜索
        if (keyword != null && !keyword.isEmpty()) {
            List<Integer> matchedUserIds = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .like(User::getRealName, keyword)
                            .select(User::getUserId)
            ).stream().map(User::getUserId).collect(Collectors.toList());
            if (matchedUserIds.isEmpty()) {
                Page<Map<String, Object>> empty = new Page<>(page.getCurrent(), page.getSize(), 0);
                empty.setRecords(Collections.emptyList());
                return empty;
            }
            wrapper.in(DisabilityInfo::getUserId, matchedUserIds);
        }

        Page<DisabilityInfo> rawPage = baseMapper.selectPage(page, wrapper);
        List<DisabilityInfo> records = rawPage.getRecords();
        if (records.isEmpty()) {
            Page<Map<String, Object>> empty = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        // 批量加载学员姓名
        Set<Integer> userIds = records.stream().map(DisabilityInfo::getUserId).collect(Collectors.toSet());
        Map<Integer, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        List<Map<String, Object>> enriched = records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("userId", r.getUserId());
            User u = userMap.get(r.getUserId());
            m.put("studentName", u != null ? u.getRealName() : null);
            m.put("studentPhone", u != null ? u.getPhone() : null);
            m.put("disabilityType", r.getDisabilityType());
            m.put("certificateNo", r.getCertificateNo());
            m.put("certificateFileId", r.getCertificateFileId());
            m.put("auditStatus", r.getAuditStatus());
            m.put("auditRemark", r.getAuditRemark());
            m.put("auditTime", r.getAuditTime());
            m.put("createTime", r.getCreateTime());
            return m;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> result = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        result.setRecords(enriched);
        return result;
    }
}
