package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.Appointment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 约课表 Mapper */
@Repository
public interface AppointmentMapper extends BaseMapper<Appointment> {

    /** 查询约课详情（含学员姓名、教练姓名、排班科目信息） */
    @Select("SELECT a.*, " +
            "  s.real_name AS student_name, s.phone AS student_phone, s.license_type AS student_license_type, " +
            "  u.real_name AS coach_name, " +
            "  cs.subject AS schedule_subject, cs.license_type AS schedule_license_type " +
            "FROM appointment a " +
            "JOIN user s ON a.student_id = s.user_id " +
            "LEFT JOIN coach c ON a.coach_id = c.coach_id " +
            "LEFT JOIN user u ON c.user_id = u.user_id " +
            "LEFT JOIN coach_schedule cs ON a.schedule_id = cs.id AND cs.is_deleted = 0 " +
            "WHERE a.is_deleted = 0 " +
            "ORDER BY a.create_time DESC")
    List<Map<String, Object>> selectListWithDetails();

    /** 根据ID查询约课详情（含学员姓名、教练姓名、排班科目信息） */
    @Select("SELECT a.*, " +
            "  s.real_name AS student_name, s.phone AS student_phone, s.license_type AS student_license_type, " +
            "  u.real_name AS coach_name, " +
            "  cs.subject AS schedule_subject, cs.license_type AS schedule_license_type " +
            "FROM appointment a " +
            "JOIN user s ON a.student_id = s.user_id " +
            "LEFT JOIN coach c ON a.coach_id = c.coach_id " +
            "LEFT JOIN user u ON c.user_id = u.user_id " +
            "LEFT JOIN coach_schedule cs ON a.schedule_id = cs.id AND cs.is_deleted = 0 " +
            "WHERE a.id = #{id} AND a.is_deleted = 0")
    Map<String, Object> selectByIdWithDetails(@Param("id") Integer id);
}
