package com.homework.driveman.service.impl;

import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.ExamRegistration;
import com.homework.driveman.entity.StudentCoach;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.ExamRegistrationMapper;
import com.homework.driveman.mapper.StudentCoachMapper;
import com.homework.driveman.mapper.TrainingRecordMapper;
import com.homework.driveman.service.ICoachPortalService;
import com.homework.driveman.web.ServiceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
}