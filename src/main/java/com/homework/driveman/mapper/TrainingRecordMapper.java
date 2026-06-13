package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.TrainingRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

/** 学时记录表 Mapper */
@Repository
public interface TrainingRecordMapper extends BaseMapper<TrainingRecord> {

    /** 统计某学员某车型某科目的累计学时 */
    @Select("SELECT COALESCE(SUM(duration), 0) FROM training_record " +
            "WHERE student_id = #{studentId} AND license_type = #{licenseType} " +
            "AND subject_type = #{subject} AND is_deleted = 0")
    BigDecimal sumTrainingHours(@Param("studentId") Integer studentId,
                                @Param("licenseType") String licenseType,
                                @Param("subject") Integer subject);

    /** 统计某个教练名下的学员总学时（所有学员所有科目总和） */
    @Select("SELECT COALESCE(SUM(duration), 0) FROM training_record " +
            "WHERE coach_id = #{coachId} AND is_deleted = 0")
    BigDecimal sumHoursByCoach(@Param("coachId") Integer coachId);

    /** 统计某个教练名下的学员人数（去重） */
    @Select("SELECT COUNT(DISTINCT student_id) FROM training_record " +
            "WHERE coach_id = #{coachId} AND is_deleted = 0")
    Integer countDistinctStudentsByCoach(@Param("coachId") Integer coachId);

    /** 统计某个教练名下某科目累计学时 */
    @Select("SELECT COALESCE(SUM(duration), 0) FROM training_record " +
            "WHERE coach_id = #{coachId} AND subject_type = #{subject} AND is_deleted = 0")
    BigDecimal sumHoursByCoachAndSubject(@Param("coachId") Integer coachId,
                                         @Param("subject") Integer subject);

    /** 统计某个约课已录入的累计学时 */
    @Select("SELECT COALESCE(SUM(duration), 0) FROM training_record " +
            "WHERE appointment_id = #{appointmentId} AND is_deleted = 0")
    BigDecimal sumDurationByAppointmentId(@Param("appointmentId") Integer appointmentId);
}
