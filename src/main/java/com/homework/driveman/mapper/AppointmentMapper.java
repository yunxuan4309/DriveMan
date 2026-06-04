package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.Appointment;
import org.springframework.stereotype.Repository;

/** 约课表 Mapper */
@Repository
public interface AppointmentMapper extends BaseMapper<Appointment> {
}
