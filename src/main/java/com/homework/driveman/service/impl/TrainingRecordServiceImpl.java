package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.constant.AppointmentStatus;
import com.homework.driveman.dto.RecordHoursDTO;
import com.homework.driveman.entity.Appointment;
import com.homework.driveman.entity.TrainingRecord;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.AppointmentMapper;
import com.homework.driveman.mapper.TrainingRecordMapper;
import com.homework.driveman.service.ITrainingRecordService;
import com.homework.driveman.web.ServiceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TrainingRecordServiceImpl extends ServiceImpl<TrainingRecordMapper, TrainingRecord> implements ITrainingRecordService {

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Override
    public BigDecimal getTotalHoursByStudentAndCoach(Integer studentId, Integer coachId) {
        List<TrainingRecord> records = baseMapper.selectList(
                new LambdaQueryWrapper<TrainingRecord>()
                        .eq(TrainingRecord::getStudentId, studentId)
                        .eq(TrainingRecord::getCoachId, coachId)
        );
        return records.stream()
                .map(TrainingRecord::getDuration)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional
    public void recordTrainingHours(Integer coachId, RecordHoursDTO dto) {
        // 1. 校验约课存在
        Appointment appointment = appointmentMapper.selectById(dto.getAppointmentId());
        if (appointment == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "约课记录不存在");
        }

        // 2. 校验约课属于该教练
        if (!appointment.getCoachId().equals(coachId)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "无权录入其他教练的约课时学");
        }

        // 3. 校验约课状态为“已确认”
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "只有已确认的约课才能录入学时");
        }

        // 4. 防止重复录入（同一约课只能录入一次学时）
        long count = baseMapper.selectCount(
                new LambdaQueryWrapper<TrainingRecord>()
                        .eq(TrainingRecord::getAppointmentId, dto.getAppointmentId())
        );
        if (count > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该约课已录入学时，不可重复录入");
        }

        // 5. 构建学时记录
        TrainingRecord record = new TrainingRecord();
        record.setStudentId(appointment.getStudentId());
        record.setCoachId(coachId);
        record.setAppointmentId(dto.getAppointmentId());
        record.setDuration(dto.getDuration());
        record.setSubjectType(dto.getSubjectType());  // 直接从DTO获取，必填
        record.setRemark(dto.getRemark());

        baseMapper.insert(record);
    }
}