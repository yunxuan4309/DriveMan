package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.constant.AppointmentStatus;
import com.homework.driveman.entity.Appointment;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.AppointmentMapper;
import com.homework.driveman.service.IAppointmentService;
import com.homework.driveman.web.ServiceCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements IAppointmentService {

    @Override
    @Transactional
    public void confirmAppointment(Integer appointmentId, Integer coachId) {
        Appointment appointment = getById(appointmentId);
        if (appointment == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "约课记录不存在");
        }
        if (!appointment.getCoachId().equals(coachId)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "无权操作其他教练的约课");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "该约课已被处理，当前状态：" + appointment.getStatus());
        }
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        updateById(appointment);
    }

    @Override
    @Transactional
    public void rejectAppointment(Integer appointmentId, Integer coachId, String reason) {
        Appointment appointment = getById(appointmentId);
        if (appointment == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "约课记录不存在");
        }
        if (!appointment.getCoachId().equals(coachId)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "无权操作其他教练的约课");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "该约课已被处理，当前状态：" + appointment.getStatus());
        }
        appointment.setStatus(AppointmentStatus.REJECTED);
        if (reason != null && !reason.isEmpty()) {
            appointment.setCancelReason(reason);  // 使用实体中的 cancelReason 字段记录拒绝原因
        }
        updateById(appointment);
    }
}