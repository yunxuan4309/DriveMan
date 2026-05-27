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
}
