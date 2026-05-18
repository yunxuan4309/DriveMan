package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.Appointment;
import com.homework.driveman.mapper.AppointmentMapper;
import com.homework.driveman.service.IAppointmentService;
import org.springframework.stereotype.Service;

/** 约课业务实现 */
@Service
public class AppointmentServiceImpl extends ServiceImpl<AppointmentMapper, Appointment> implements IAppointmentService {
}
