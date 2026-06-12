package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.entity.CoachSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 排班表 Mapper
 */
@Mapper
public interface CoachScheduleMapper extends BaseMapper<CoachSchedule> {

    /**
     * 分页查询排班列表（含教练姓名、车牌号、场地名称）
     * @param coachId 可选，指定教练ID时只查该教练的排班
     */
    @Select("<script>" +
            "SELECT cs.*, u.real_name AS coach_name, v.plate_number, vn.name AS venue_name " +
            "FROM coach_schedule cs " +
            "LEFT JOIN coach c ON cs.coach_id = c.coach_id " +
            "LEFT JOIN user u ON c.user_id = u.user_id " +
            "LEFT JOIN vehicle v ON cs.vehicle_id = v.id " +
            "LEFT JOIN venue vn ON cs.venue_id = vn.id " +
            "WHERE cs.is_deleted = 0 " +
            "  <if test='coachId != null'>AND cs.coach_id = #{coachId}</if> " +
            "  <if test='keyword != null and keyword != \"\"'>AND u.real_name LIKE CONCAT('%', #{keyword}, '%')</if> " +
            "  <if test='plateNumber != null and plateNumber != \"\"'>AND v.plate_number LIKE CONCAT('%', #{plateNumber}, '%')</if> " +
            "  <if test='venueName != null and venueName != \"\"'>AND vn.name LIKE CONCAT('%', #{venueName}, '%')</if> " +
            "  <if test='licenseType != null and licenseType != \"\"'>AND cs.license_type = #{licenseType}</if> " +
            "  <if test='status != null'>AND cs.status = #{status}</if> " +
            "  <if test='startDateStart != null'>AND cs.start_time &gt;= #{startDateStart}</if> " +
            "  <if test='startDateEnd != null'>AND cs.start_time &lt;= #{startDateEnd}</if> " +
            "ORDER BY cs.start_time DESC" +
            "</script>")
    Page<Map<String, Object>> selectPageWithDetails(Page<?> page,
                                                     @Param("coachId") Integer coachId,
                                                     @Param("keyword") String keyword,
                                                     @Param("plateNumber") String plateNumber,
                                                     @Param("venueName") String venueName,
                                                     @Param("licenseType") String licenseType,
                                                     @Param("status") Integer status,
                                                     @Param("startDateStart") LocalDateTime startDateStart,
                                                     @Param("startDateEnd") LocalDateTime startDateEnd);
}
