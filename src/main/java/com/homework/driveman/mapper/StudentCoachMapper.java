package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.StudentCoach;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/** 学员-教练关联表 Mapper */
@Repository
public interface StudentCoachMapper extends BaseMapper<StudentCoach> {
    @Select("SELECT student_id FROM student_coach WHERE coach_id = #{coachId} AND status = 1 AND is_deleted = 0")
    List<Integer> findBoundStudentIds(@Param("coachId") Integer coachId);
}
