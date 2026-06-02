package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.TrainingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.math.BigDecimal;

@Mapper
public interface TrainingRecordMapper extends BaseMapper<TrainingRecord> {

    @Select("SELECT SUM(duration) FROM training_record WHERE coach_id = #{coachId} AND is_deleted = 0")
    BigDecimal sumDurationByCoach(@Param("coachId") Integer coachId);
}