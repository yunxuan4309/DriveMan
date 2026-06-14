package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.ExamRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 考试报名表 Mapper */
@Mapper
public interface ExamRegistrationMapper extends BaseMapper<ExamRegistration> {

    /**
     * 获取某教练名下所有参加过考试的学员ID列表（去重）
     */
    @Select("SELECT DISTINCT student_id FROM exam_registration " +
            "WHERE student_id IN (SELECT student_id FROM student_coach WHERE coach_id = #{coachId} AND status = 1) " +
            "AND is_deleted = 0")
    List<Integer> findExamStudentIds(@Param("coachId") Integer coachId);

    /**
     * 获取某学员已合格的科目集合（pass_status = 1 视为合格）
     * 可选按车型过滤，避免增驾后旧车型的通过记录污染新车型的考试报名
     *
     * @param studentId   学员ID
     * @param licenseType 可选车型过滤（传 null 则返回所有车型的合格科目）
     */
    @Select("<script>" +
            "SELECT DISTINCT er.subject FROM exam_registration er " +
            "<if test='licenseType != null and licenseType != \"\"'>" +
            "  JOIN exam_session es ON er.session_id = es.id AND es.is_deleted = 0 " +
            "</if>" +
            "WHERE er.student_id = #{studentId} AND er.pass_status = 1 AND er.is_deleted = 0 " +
            "<if test='licenseType != null and licenseType != \"\"'>" +
            "  AND es.license_type = #{licenseType} " +
            "</if>" +
            "</script>")
    Set<Integer> findPassedSubjectsByStudent(@Param("studentId") Integer studentId,
                                              @Param("licenseType") String licenseType);

    /** 各科目月度考试通过率趋势（支持按年份和科目筛选） */
    @Select("<script>" +
            "SELECT " +
            "  DATE_FORMAT(s.exam_date, '%Y-%m') AS month, " +
            "  s.subject, " +
            "  COUNT(*) AS total, " +
            "  SUM(CASE WHEN er.pass_status = 1 THEN 1 ELSE 0 END) AS pass_count, " +
            "  ROUND(SUM(CASE WHEN er.pass_status = 1 THEN 1.0 ELSE 0.0 END) / COUNT(*) * 100, 1) AS pass_rate " +
            "FROM exam_registration er " +
            "JOIN exam_session s ON er.session_id = s.id AND s.is_deleted = 0 " +
            "WHERE er.pass_status IS NOT NULL AND er.is_deleted = 0 " +
            "  <if test='year != null'>AND YEAR(s.exam_date) = #{year}</if> " +
            "  <if test='subject != null'>AND s.subject = #{subject}</if> " +
            "GROUP BY DATE_FORMAT(s.exam_date, '%Y-%m'), s.subject " +
            "ORDER BY month ASC, s.subject ASC" +
            "</script>")
    List<Map<String, Object>> selectMonthlyPassRate(@Param("year") Integer year,
                                                     @Param("subject") Integer subject);
}