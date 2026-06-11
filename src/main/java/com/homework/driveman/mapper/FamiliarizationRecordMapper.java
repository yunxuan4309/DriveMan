package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.entity.FamiliarizationRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/** 合场记录表 Mapper */
@Repository
public interface FamiliarizationRecordMapper extends BaseMapper<FamiliarizationRecord> {

    /** 查询合场记录列表（含场次信息、教练姓名、学员姓名） */
    @Select("SELECT " +
            "  fr.*, " +
            "  s.exam_date, s.start_time, s.location AS venue_name, s.license_type, " +
            "  stu.real_name AS student_name, " +
            "  u.real_name AS coach_name " +
            "FROM familiarization_record fr " +
            "JOIN exam_session s ON fr.exam_session_id = s.id " +
            "LEFT JOIN user stu ON fr.student_id = stu.user_id " +
            "LEFT JOIN coach c ON fr.coach_id = c.coach_id " +
            "LEFT JOIN user u ON c.user_id = u.user_id " +
            "WHERE fr.is_deleted = 0 AND s.is_deleted = 0 " +
            "ORDER BY fr.create_time DESC")
    List<Map<String, Object>> selectListWithDetails();

    /** 分页查询合场记录（含场次信息、教练姓名、学员姓名），支持多条件筛选 */
    @Select("<script>" +
            "SELECT " +
            "  fr.*, " +
            "  s.exam_date, s.start_time, s.location AS venue_name, s.license_type, " +
            "  stu.real_name AS student_name, " +
            "  u.real_name AS coach_name " +
            "FROM familiarization_record fr " +
            "JOIN exam_session s ON fr.exam_session_id = s.id " +
            "LEFT JOIN user stu ON fr.student_id = stu.user_id " +
            "LEFT JOIN coach c ON fr.coach_id = c.coach_id " +
            "LEFT JOIN user u ON c.user_id = u.user_id " +
            "LEFT JOIN venue v ON s.venue_id = v.id " +
            "WHERE fr.is_deleted = 0 AND s.is_deleted = 0 " +
            "  <if test='status != null'>AND fr.status = #{status}</if> " +
            "  <if test='keyword != null and keyword != \"\"'>AND (stu.real_name LIKE CONCAT('%', #{keyword}, '%') OR s.location LIKE CONCAT('%', #{keyword}, '%') OR v.name LIKE CONCAT('%', #{keyword}, '%'))</if> " +
            "  <if test='subject != null'>AND fr.subject = #{subject}</if> " +
            "  <if test='carType != null'>AND fr.car_type = #{carType}</if> " +
            "  <if test='examDateStart != null'>AND s.exam_date &gt;= #{examDateStart}</if> " +
            "  <if test='examDateEnd != null'>AND s.exam_date &lt;= #{examDateEnd}</if> " +
            "  <if test='coachName != null and coachName != \"\"'>AND u.real_name LIKE CONCAT('%', #{coachName}, '%')</if> " +
            "ORDER BY fr.create_time DESC" +
            "</script>")
    Page<Map<String, Object>> selectPageWithDetails(Page<?> page,
                                                     @Param("status") Integer status,
                                                     @Param("keyword") String keyword,
                                                     @Param("subject") Integer subject,
                                                     @Param("carType") Integer carType,
                                                     @Param("examDateStart") String examDateStart,
                                                     @Param("examDateEnd") String examDateEnd,
                                                     @Param("coachName") String coachName);

    /** 查询单条合场记录详情（含场次信息、教练姓名、学员姓名） */
    @Select("SELECT " +
            "  fr.*, " +
            "  s.exam_date, s.start_time, s.location AS venue_name, s.license_type, " +
            "  stu.real_name AS student_name, " +
            "  u.real_name AS coach_name " +
            "FROM familiarization_record fr " +
            "JOIN exam_session s ON fr.exam_session_id = s.id " +
            "LEFT JOIN user stu ON fr.student_id = stu.user_id " +
            "LEFT JOIN coach c ON fr.coach_id = c.coach_id " +
            "LEFT JOIN user u ON c.user_id = u.user_id " +
            "WHERE fr.id = #{id} AND fr.is_deleted = 0")
    Map<String, Object> selectByIdWithDetails(@Param("id") Integer id);

    /** 分页查询学员本人的合场记录，支持按状态、创建时间范围筛选 */
    @Select("<script>" +
            "SELECT " +
            "  fr.*, " +
            "  s.exam_date, s.start_time, s.location AS venue_name, s.license_type, " +
            "  stu.real_name AS student_name, " +
            "  u.real_name AS coach_name " +
            "FROM familiarization_record fr " +
            "JOIN exam_session s ON fr.exam_session_id = s.id " +
            "LEFT JOIN user stu ON fr.student_id = stu.user_id " +
            "LEFT JOIN coach c ON fr.coach_id = c.coach_id " +
            "LEFT JOIN user u ON c.user_id = u.user_id " +
            "WHERE fr.is_deleted = 0 AND s.is_deleted = 0 " +
            "  AND fr.student_id = #{studentId} " +
            "  <if test='status != null'>AND fr.status = #{status}</if> " +
            "  <if test='startDate != null'>AND fr.create_time &gt;= #{startDate}</if> " +
            "  <if test='endDate != null'>AND fr.create_time &lt;= #{endDate}</if> " +
            "ORDER BY fr.create_time DESC" +
            "</script>")
    Page<Map<String, Object>> selectMyPageWithDetails(Page<?> page,
                                                       @Param("studentId") Integer studentId,
                                                       @Param("status") Integer status,
                                                       @Param("startDate") String startDate,
                                                       @Param("endDate") String endDate);
}
