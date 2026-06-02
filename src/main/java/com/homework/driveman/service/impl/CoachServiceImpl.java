package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.TrainingRecord;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.*;
import com.homework.driveman.service.ICoachService;
import com.homework.driveman.vo.CoachRatingVO;
import com.homework.driveman.vo.CoachWorkloadVO;
import com.homework.driveman.vo.StudentInfoVO;
import com.homework.driveman.web.ServiceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
/**
 * 教练业务实现
 * 推荐算法：准教车型匹配 → 按评分降序
 */
@Service
public class CoachServiceImpl extends ServiceImpl<CoachMapper, Coach> implements ICoachService {
    @Autowired
    private StudentCoachMapper studentCoachMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TrainingRecordMapper trainingRecordMapper;

    @Autowired
    private ExamRegistrationMapper examRegistrationMapper;

    @Override
    public List<Coach> recommend(String licenseType, int topN) {
        LambdaQueryWrapper<Coach> wrapper = new LambdaQueryWrapper<Coach>()
                // 准教车型包含学员报考车型（vehicle_type 字段逗号分隔，如 "C1,C2"）
                .apply("FIND_IN_SET({0}, vehicle_type)", licenseType)
                .orderByDesc(Coach::getRating)
                .last("LIMIT " + topN);
        return list(wrapper);
    }

    @Override
    public List<StudentInfoVO> getMyStudents(Integer coachId) {
        // 1. 查询该教练名下绑定的学员ID列表（student_coach表中status=1）
        List<Integer> studentIds = studentCoachMapper.findBoundStudentIds(coachId);
        if (studentIds == null || studentIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 查询学员基本信息（user表 role=1）
        List<User> students = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .in(User::getUserId, studentIds)
                        .eq(User::getRole, 1)
        );

        // 3. 统计每位学员的总学时（training_record表）
        return students.stream().map(student -> {
            List<TrainingRecord> records = trainingRecordMapper.selectList(
                    new LambdaQueryWrapper<TrainingRecord>()
                            .eq(TrainingRecord::getStudentId, student.getUserId())
                            .eq(TrainingRecord::getCoachId, coachId)
            );
            BigDecimal totalHours = records.stream()
                    .map(TrainingRecord::getDuration)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            return StudentInfoVO.builder()
                    .studentId(student.getUserId())
                    .realName(student.getRealName())
                    .phone(student.getPhone())
                    .totalHours(totalHours)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public void setAvailableTime(Integer coachId, String availableTime) {
        Coach coach = getById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练信息不存在");
        }
        coach.setAvailableTime(availableTime);
        updateById(coach);
    }

    @Override
    public CoachWorkloadVO getWorkload(Integer coachId) {
        // 1. 绑定学员数
        List<Integer> studentIds = studentCoachMapper.findBoundStudentIds(coachId);
        int totalStudents = studentIds != null ? studentIds.size() : 0;

        // 2. 总学时
        BigDecimal totalHours = trainingRecordMapper.sumDurationByCoach(coachId);
        if (totalHours == null) totalHours = BigDecimal.ZERO;

        // 3. 通过率：四科全通过的学员数 / 参加过考试的学员数
        List<Integer> examStudentIds = examRegistrationMapper.findExamStudentIds(coachId);
        int totalExamStudents = examStudentIds != null ? examStudentIds.size() : 0;
        int passedAllCount = 0;
        if (totalExamStudents > 0) {
            for (Integer studentId : examStudentIds) {
                Set<Integer> passedSubjects = examRegistrationMapper.findPassedSubjectsByStudent(studentId);
                if (passedSubjects.contains(1) && passedSubjects.contains(2) &&
                        passedSubjects.contains(3) && passedSubjects.contains(4)) {
                    passedAllCount++;
                }
            }
        }
        double passRate = totalExamStudents == 0 ? 0.0 : (passedAllCount * 100.0 / totalExamStudents);
        passRate = Math.round(passRate * 100) / 100.0; // 保留两位小数

        return CoachWorkloadVO.builder()
                .totalStudents(totalStudents)
                .totalHours(totalHours)
                .passRate(passRate)
                .build();
    }
    @Override
    public CoachRatingVO getRating(Integer coachId) {
        Coach coach = getById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练信息不存在");
        }
        return CoachRatingVO.builder()
                .coachId(coach.getCoachId())
                .rating(coach.getRating())
                .build();
    }

}
