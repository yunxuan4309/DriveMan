package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.TrainingRecord;
import org.springframework.stereotype.Repository;

/** 学时记录表 Mapper */
@Repository
public interface TrainingRecordMapper extends BaseMapper<TrainingRecord> {
}
