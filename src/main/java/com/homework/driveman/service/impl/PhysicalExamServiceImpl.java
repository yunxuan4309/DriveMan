package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.PhysicalExam;
import com.homework.driveman.entity.StudentCoach;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.PhysicalExamMapper;
import com.homework.driveman.mapper.StudentCoachMapper;
import com.homework.driveman.service.IPhysicalExamService;
import com.homework.driveman.web.ServiceCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 体检申请服务实现
 */
@Slf4j
@Service
public class PhysicalExamServiceImpl extends ServiceImpl<PhysicalExamMapper, PhysicalExam> implements IPhysicalExamService {

    @Autowired
    private CoachMapper coachMapper;

    @Autowired
    private StudentCoachMapper studentCoachMapper;
    @Override
    public PhysicalExam apply(Integer studentId, String location, String examDate) {
        // 检查是否已有待审核或已通过未完成的申请
        Long count = lambdaQuery()
                .eq(PhysicalExam::getStudentId, studentId)
                .in(PhysicalExam::getStatus, 0, 1)
                .count();
        if (count > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "您已有进行中的体检申请，请勿重复提交");
        }

        PhysicalExam exam = new PhysicalExam();
        exam.setStudentId(studentId);
        exam.setLocation(location);
        exam.setExamDate(LocalDate.parse(examDate));
        exam.setStatus(0); // 待审核
        save(exam);

        log.info("体检申请提交成功: studentId={}, location={}", studentId, location);
        return exam;
    }

    @Override
    public List<PhysicalExam> listByStudent(Integer studentId) {
        return lambdaQuery()
                .eq(PhysicalExam::getStudentId, studentId)
                .orderByDesc(PhysicalExam::getCreateTime)
                .list();
    }

    @Override
    public void audit(Integer id, Integer status, String remark) {
        PhysicalExam exam = getById(id);
        if (exam == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "体检申请不存在");
        }
        if (exam.getStatus() != 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该申请已处理，无法重复审核");
        }

        exam.setStatus(status);
        exam.setRemark(remark);
        updateById(exam);

        log.info("体检申请审核完成: id={}, status={}", id, status);
    }

    @Override
    public void uploadResult(Integer id, Integer fileId, Integer result) {
        PhysicalExam exam = getById(id);
        if (exam == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "体检申请不存在");
        }

        exam.setFileId(fileId);
        exam.setResult(result);
        if (result != null) {
            exam.setStatus(3); // 已完成
        }
        updateById(exam);

        log.info("体检结果上传成功: id={}, result={}", id, result);
    }

    @Override
    public List<PhysicalExam> listByCoach(Integer userId) {
        // 1. 通过userId查找教练信息
        Coach coach = coachMapper.selectOne(
                new LambdaQueryWrapper<Coach>().eq(Coach::getUserId, userId));
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练不存在");
        }

        // 2. 查询该教练名下所有正常绑定的学员ID
        List<StudentCoach> bindings = studentCoachMapper.selectList(
                new LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getCoachId, coach.getCoachId())
                        .eq(StudentCoach::getStatus, 1));

        if (bindings.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> studentIds = bindings.stream()
                .map(StudentCoach::getStudentId)
                .collect(Collectors.toList());

        // 3. 查询这些学员的体检申请记录，按创建时间降序排列
        return lambdaQuery()
                .in(PhysicalExam::getStudentId, studentIds)
                .orderByDesc(PhysicalExam::getCreateTime)
                .list();
    }
}
