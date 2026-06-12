package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.entity.CoachVehicleApplication;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;

/** 教练准教车型变更申请表 Mapper */
@Repository
public interface CoachVehicleApplicationMapper extends BaseMapper<CoachVehicleApplication> {

    /**
     * 分页查询待审核申请（含教练姓名），支持多条件搜索
     */
    @Select("<script>" +
            "SELECT a.id, a.coach_id AS coachId, a.current_vehicle_type AS currentVehicleType, " +
            "       a.requested_vehicle_type AS requestedVehicleType, a.apply_reason AS applyReason, " +
            "       a.status, a.audit_reason AS auditReason, " +
            "       a.apply_time AS applyTime, a.audit_time AS auditTime, " +
            "       u.real_name AS coachName " +
            "FROM coach_vehicle_application a " +
            "JOIN coach c ON a.coach_id = c.coach_id " +
            "JOIN user u ON c.user_id = u.user_id " +
            "WHERE a.status = 0 AND a.is_deleted = 0 " +
            "  <if test='keyword != null and keyword != \"\"'>AND u.real_name LIKE CONCAT('%', #{keyword}, '%')</if> " +
            "  <if test='currentVehicleType != null and currentVehicleType != \"\"'>AND a.current_vehicle_type = #{currentVehicleType}</if> " +
            "  <if test='requestedVehicleType != null and requestedVehicleType != \"\"'>AND a.requested_vehicle_type = #{requestedVehicleType}</if> " +
            "  <if test='applyTimeStart != null'>AND a.apply_time &gt;= #{applyTimeStart}</if> " +
            "  <if test='applyTimeEnd != null'>AND a.apply_time &lt;= #{applyTimeEnd}</if> " +
            "ORDER BY a.apply_time DESC" +
            "</script>")
    Page<Map<String, Object>> selectPagePending(Page<?> page,
                                                 @Param("keyword") String keyword,
                                                 @Param("currentVehicleType") String currentVehicleType,
                                                 @Param("requestedVehicleType") String requestedVehicleType,
                                                 @Param("applyTimeStart") LocalDateTime applyTimeStart,
                                                 @Param("applyTimeEnd") LocalDateTime applyTimeEnd);

    /**
     * 分页查询全部申请记录（含教练姓名），支持多条件搜索 + 审核时间范围
     */
    @Select("<script>" +
            "SELECT a.id, a.coach_id AS coachId, a.current_vehicle_type AS currentVehicleType, " +
            "       a.requested_vehicle_type AS requestedVehicleType, a.apply_reason AS applyReason, " +
            "       a.status, a.audit_reason AS auditReason, " +
            "       a.apply_time AS applyTime, a.audit_time AS auditTime, " +
            "       u.real_name AS coachName " +
            "FROM coach_vehicle_application a " +
            "JOIN coach c ON a.coach_id = c.coach_id " +
            "JOIN user u ON c.user_id = u.user_id " +
            "WHERE a.is_deleted = 0 " +
            "  <if test='keyword != null and keyword != \"\"'>AND u.real_name LIKE CONCAT('%', #{keyword}, '%')</if> " +
            "  <if test='vehicleType != null and vehicleType != \"\"'>AND a.requested_vehicle_type = #{vehicleType}</if> " +
            "  <if test='status != null'>AND a.status = #{status}</if> " +
            "  <if test='auditTimeStart != null'>AND a.audit_time &gt;= #{auditTimeStart}</if> " +
            "  <if test='auditTimeEnd != null'>AND a.audit_time &lt;= #{auditTimeEnd}</if> " +
            "ORDER BY a.apply_time DESC" +
            "</script>")
    Page<Map<String, Object>> selectPageAll(Page<?> page,
                                            @Param("keyword") String keyword,
                                            @Param("vehicleType") String vehicleType,
                                            @Param("status") Integer status,
                                            @Param("auditTimeStart") LocalDateTime auditTimeStart,
                                            @Param("auditTimeEnd") LocalDateTime auditTimeEnd);
}
