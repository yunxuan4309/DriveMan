package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.StudentCoach;
import org.springframework.stereotype.Repository;

/** 学员-教练关联表 Mapper */
@Repository
public interface StudentCoachMapper extends BaseMapper<StudentCoach> {
}
