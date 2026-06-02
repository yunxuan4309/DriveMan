package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.Appointment;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 约课表 Mapper */
@Repository
public interface AppointmentMapper extends BaseMapper<Appointment> {

    /** 统计各教练的约课数量（含教练姓名） */
    @Select("SELECT u.real_name AS name, COUNT(a.id) AS value " +
            "FROM appointment a " +
            "JOIN coach c ON a.coach_id = c.coach_id " +
            "JOIN user u ON c.user_id = u.user_id " +
            "WHERE a.is_deleted = 0 AND c.is_deleted = 0 AND u.is_deleted = 0 " +
            "AND a.status IN (1, 2) " +
            "GROUP BY a.coach_id " +
            "ORDER BY value DESC")
    List<Map<String, Object>> countAppointmentsPerCoach();
}
