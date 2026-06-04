package com.homework.driveman.service.impl;

import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.ExamRegistration;
import com.homework.driveman.entity.ExamSession;
import com.homework.driveman.entity.StudentCoach;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.ExamRegistrationMapper;
import com.homework.driveman.mapper.ExamSessionMapper;
import com.homework.driveman.mapper.StudentCoachMapper;
import com.homework.driveman.mapper.TrainingRecordMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.ICoachPortalService;
import com.homework.driveman.web.ServiceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教练工作台业务实现
 * 包含工作量统计、评分查询等需要跨表聚合的复杂逻辑
 */
@Service
public class CoachPortalServiceImpl implements ICoachPortalService {

    @Autowired
    private CoachMapper coachMapper;

    @Autowired
    private TrainingRecordMapper trainingRecordMapper;

    @Autowired
    private StudentCoachMapper studentCoachMapper;

    @Autowired
    private ExamRegistrationMapper examRegistrationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ExamSessionMapper examSessionMapper;

    @Override
    public Map<String, Object> getStatistics(Integer coachId) {
        // 校验教练是否存在
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练不存在");
        }

        // 1. 查询名下学员数（通过 student_coach 表统计正常绑定的学员）
        Long studentCount = studentCoachMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getCoachId, coachId)
                        .eq(StudentCoach::getStatus, 1));

        // 2. 查询总学时
        BigDecimal totalHours = trainingRecordMapper.sumHoursByCoach(coachId);

        // 3. 查询各科目学时明细
        Map<Integer, BigDecimal> hoursBySubject = new HashMap<>();
        for (int subj = 1; subj <= 4; subj++) {
            BigDecimal h = trainingRecordMapper.sumHoursByCoachAndSubject(coachId, subj);
            hoursBySubject.put(subj, h);
        }

        // 4. 通过率计算：查出该教练名下所有学员的考试记录
        // 先查出该教练名下所有正常绑定的学员ID
        List<StudentCoach> bindings = studentCoachMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getCoachId, coachId)
                        .eq(StudentCoach::getStatus, 1));
        List<Integer> studentIds = bindings.stream()
                .map(StudentCoach::getStudentId)
                .collect(Collectors.toList());

        double passRate = 0.0;
        if (!studentIds.isEmpty()) {
            // 查询这些学员所有有成绩的考试记录
            List<ExamRegistration> exams = examRegistrationMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamRegistration>()
                            .in(ExamRegistration::getStudentId, studentIds)
                            .isNotNull(ExamRegistration::getPassStatus));
            long total = exams.size();
            long passed = exams.stream()
                    .filter(r -> r.getPassStatus() != null && r.getPassStatus() == 1)
                    .count();
            passRate = total > 0 ? (double) passed / total * 100 : 0.0;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("coachId", coachId);
        result.put("studentCount", studentCount);
        result.put("totalTrainingHours", totalHours);
        result.put("hoursBySubject", hoursBySubject);
        result.put("examTotal", 0); // 下面覆盖
        result.put("examPassed", 0);
        result.put("passRate", String.format("%.1f%%", passRate));
        return result;
    }

    @Override
    public Map<String, Object> getRating(Integer coachId) {
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("coachId", coachId);
        result.put("rating", coach.getRating());
        result.put("coachYears", coach.getCoachYears());
        result.put("vehicleType", coach.getVehicleType());
        return result;
    }

    @Override
    public List<Map<String, Object>> getStudentExamRegistrations(Integer coachId) {
        // 校验教练是否存在
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练不存在");
        }

        // 查出所有正常绑定的学员
        List<StudentCoach> bindings = studentCoachMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getCoachId, coachId)
                        .eq(StudentCoach::getStatus, 1));
        if (bindings.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> studentIds = bindings.stream()
                .map(StudentCoach::getStudentId)
                .collect(Collectors.toList());

        // 查这些学员的考试报名记录
        List<ExamRegistration> registrations = examRegistrationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamRegistration>()
                        .in(ExamRegistration::getStudentId, studentIds)
                        .orderByDesc(ExamRegistration::getApplyTime));
        if (registrations.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量加载学员姓名
        Map<Integer, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        // 批量加载场次信息
        Set<Integer> sessionIds = registrations.stream()
                .map(ExamRegistration::getSessionId)
                .collect(Collectors.toSet());
        Map<Integer, ExamSession> sessionMap = examSessionMapper.selectBatchIds(sessionIds).stream()
                .collect(Collectors.toMap(ExamSession::getId, s -> s, (a, b) -> a));

        // 组装结果
        return registrations.stream().map(reg -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", reg.getId());
            map.put("studentId", reg.getStudentId());

            User student = userMap.get(reg.getStudentId());
            map.put("studentName", student != null ? student.getRealName() : null);

            map.put("sessionId", reg.getSessionId());
            map.put("subject", reg.getSubject());
            map.put("status", reg.getStatus());

            // 状态中文描述
            String statusDesc = switch (reg.getStatus()) {
                case 0 -> "待审核";
                case 1 -> "审核通过";
                case 2 -> "审核不通过";
                case 3 -> "已考试";
                default -> "未知";
            };
            map.put("statusDesc", statusDesc);

            map.put("score", reg.getScore());
            map.put("passStatus", reg.getPassStatus());
            map.put("retakeCount", reg.getRetakeCount());
            map.put("isRetake", reg.getIsRetake());
            map.put("applyTime", reg.getApplyTime());
            map.put("auditTime", reg.getAuditTime());

            ExamSession session = sessionMap.get(reg.getSessionId());
            if (session != null) {
                map.put("examDate", session.getExamDate());
                map.put("startTime", session.getStartTime());
                map.put("location", session.getLocation());
                map.put("licenseType", session.getLicenseType());
            }

            return map;
        }).collect(Collectors.toList());
    }
}