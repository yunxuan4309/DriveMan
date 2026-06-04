package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.PhysicalExam;
import com.homework.driveman.entity.Venue;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.PhysicalExamMapper;
import com.homework.driveman.mapper.VenueMapper;
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
    private VenueMapper venueMapper;

    @Override
    public PhysicalExam apply(Integer studentId, Integer venueId, String examDate) {
        // 校验体检地点是否合法
        Venue venue = venueMapper.selectById(venueId);
        if (venue == null || venue.getVenueType() != 3) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "无效的体检地点，请从可选列表中选择");
        }
        if (venue.getStatus() != 1) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "该体检地点已停用，请选择其他地点");
        }

        // 检查是否已有待审核或已通过未完成的申请
        Long count = lambdaQuery()
                .eq(PhysicalExam::getStudentId, studentId)
                .in(PhysicalExam::getStatus, 0, 1)
                .count();
        if (count > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "您已有进行中的体检申请，请勿重复提交");
        }

        // 检查上次体检是否为已合格状态（合格后不可再申请）
        Long passedCount = lambdaQuery()
                .eq(PhysicalExam::getStudentId, studentId)
                .eq(PhysicalExam::getResult, 1)
                .eq(PhysicalExam::getStatus, 3)
                .count();
        if (passedCount > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "您已体检合格，无需再次提交");
        }

        PhysicalExam exam = new PhysicalExam();
        exam.setStudentId(studentId);
        exam.setVenueId(venueId);
        exam.setLocation(venue.getName()); // 从 venue 表同步名称
        try {
            exam.setExamDate(LocalDate.parse(examDate));
        } catch (Exception e) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "日期格式错误，请使用 yyyy-MM-dd 格式");
        }
        exam.setStatus(0); // 待审核
        save(exam);

        log.info("体检申请提交成功: studentId={}, venueId={}", studentId, venueId);
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
        if (status == null || (status != 1 && status != 2)) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "审核状态只能为 1(通过) 或 2(不通过)");
        }
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
        // 只有审核通过的申请才能上传体检结果
        if (exam.getStatus() != 1) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "该申请未通过审核，无法录入体检结果");
        }
        // 防止重复录入
        if (exam.getResult() != null) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该申请已录入体检结果，无法重复录入");
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
    public List<String> getLocations() {
        List<Venue> venues = venueMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Venue>()
                        .eq(Venue::getVenueType, 3)
                        .eq(Venue::getStatus, 1)
                        .orderByAsc(Venue::getName)
        );
        if (venues.isEmpty()) {
            return Collections.emptyList();
        }
        return venues.stream().map(Venue::getName).collect(Collectors.toList());
    }
}
