package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.ExamRegistration;
import com.homework.driveman.entity.ExamSession;
import com.homework.driveman.entity.User;
import com.homework.driveman.mapper.ExamRegistrationMapper;
import com.homework.driveman.mapper.ExamSessionMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.IExamRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 考试报名业务实现 */
@Service
public class ExamRegistrationServiceImpl extends ServiceImpl<ExamRegistrationMapper, ExamRegistration>
        implements IExamRegistrationService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ExamSessionMapper examSessionMapper;

    @Override
    public Page<Map<String, Object>> pageWithDetails(Page<ExamRegistration> page, Integer status, String keyword) {
        LambdaQueryWrapper<ExamRegistration> wrapper = new LambdaQueryWrapper<>();

        // 按状态筛选
        if (status != null) {
            wrapper.eq(ExamRegistration::getStatus, status);
        }

        // 按学员姓名模糊搜索
        if (keyword != null && !keyword.isEmpty()) {
            List<Integer> matchedUserIds = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .like(User::getRealName, keyword)
                            .select(User::getUserId)
            ).stream().map(User::getUserId).toList();
            if (matchedUserIds.isEmpty()) {
                // 没有匹配的学员，返回空页
                return new Page<>(page.getCurrent(), page.getSize(), 0);
            }
            wrapper.in(ExamRegistration::getStudentId, matchedUserIds);
        }

        wrapper.orderByDesc(ExamRegistration::getApplyTime);
        Page<ExamRegistration> rawPage = baseMapper.selectPage(page, wrapper);

        List<ExamRegistration> records = rawPage.getRecords();
        if (records.isEmpty()) {
            return new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        }

        // 批量加载学员姓名
        Set<Integer> studentIds = records.stream()
                .map(ExamRegistration::getStudentId)
                .collect(Collectors.toSet());
        Map<Integer, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        // 批量加载场次信息
        Set<Integer> sessionIds = records.stream()
                .map(ExamRegistration::getSessionId)
                .collect(Collectors.toSet());
        Map<Integer, ExamSession> sessionMap = examSessionMapper.selectBatchIds(sessionIds).stream()
                .collect(Collectors.toMap(ExamSession::getId, s -> s, (a, b) -> a));

        // 组装
        List<Map<String, Object>> result = records.stream().map(reg -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", reg.getId());
            map.put("studentId", reg.getStudentId());

            User student = userMap.get(reg.getStudentId());
            map.put("studentName", student != null ? student.getRealName() : null);

            map.put("sessionId", reg.getSessionId());
            map.put("subject", reg.getSubject());
            map.put("status", reg.getStatus());

            ExamSession session = sessionMap.get(reg.getSessionId());
            if (session != null) {
                map.put("examDate", session.getExamDate());
                map.put("location", session.getLocation());
                map.put("licenseType", session.getLicenseType());
            }

            map.put("score", reg.getScore());
            map.put("passStatus", reg.getPassStatus());
            map.put("retakeCount", reg.getRetakeCount());
            map.put("isRetake", reg.getIsRetake());
            map.put("applyTime", reg.getApplyTime());
            map.put("auditTime", reg.getAuditTime());
            return map;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> resultPage = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        resultPage.setRecords(result);
        return resultPage;
    }
}
