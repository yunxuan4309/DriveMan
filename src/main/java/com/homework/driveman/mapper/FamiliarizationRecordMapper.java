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

    /** 分页查询合场记录（含场次信息、教练姓名、学员姓名），支持按状态筛选 */
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
            "  <if test='status != null'>AND fr.status = #{status}</if> " +
            "ORDER BY fr.create_time DESC" +
            "</script>")
    Page<Map<String, Object>> selectPageWithDetails(Page<?> page, @Param("status") Integer status);

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
}
