package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.ExamRegistration;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
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
     * 获取某学员已合格的科目集合（成绩 >= 90 视为合格）
     */
    @Select("SELECT DISTINCT subject FROM exam_registration " +
            "WHERE student_id = #{studentId} AND score >= 90 AND is_deleted = 0")
    Set<Integer> findPassedSubjectsByStudent(@Param("studentId") Integer studentId);
}