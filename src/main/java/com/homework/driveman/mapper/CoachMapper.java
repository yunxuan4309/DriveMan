package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.Coach;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 教练扩展表 Mapper */
@Repository
public interface CoachMapper extends BaseMapper<Coach> {

    /** 教练效能：带教学员数 + 学员考试通过率 + 评分 + 执教年限（支持按车型和 TopN 筛选） */
    @Select("<script>" +
            "SELECT " +
            "  u.real_name AS coach_name, " +
            "  c.rating, " +
            "  c.coach_years, " +
            "  COUNT(DISTINCT sc.student_id) AS student_count, " +
            "  COUNT(CASE WHEN er.pass_status IS NOT NULL THEN er.id END) AS exam_count, " +
            "  SUM(CASE WHEN er.pass_status = 1 THEN 1 ELSE 0 END) AS pass_count, " +
            "  ROUND( " +
            "    SUM(CASE WHEN er.pass_status = 1 THEN 1.0 ELSE 0.0 END) " +
            "    / NULLIF(COUNT(CASE WHEN er.pass_status IS NOT NULL THEN er.id END), 0) * 100, 1 " +
            "  ) AS pass_rate " +
            "FROM coach c " +
            "JOIN user u ON c.user_id = u.user_id AND u.is_deleted = 0 " +
            "LEFT JOIN student_coach sc ON c.coach_id = sc.coach_id AND sc.status = 1 AND sc.is_deleted = 0 " +
            "LEFT JOIN exam_registration er ON sc.student_id = er.student_id AND er.is_deleted = 0 " +
            "WHERE c.is_deleted = 0 " +
            "  <if test='licenseType != null and licenseType != \"\"'>AND FIND_IN_SET(#{licenseType}, c.vehicle_type)</if> " +
            "GROUP BY c.coach_id " +
            "ORDER BY pass_rate DESC " +
            "  <if test='topN != null'>LIMIT #{topN}</if> " +
            "</script>")
    List<Map<String, Object>> selectCoachEffectiveness(@Param("licenseType") String licenseType,
                                                        @Param("topN") Integer topN);
}
