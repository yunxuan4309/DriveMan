package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.TrainingRecord;
import com.homework.driveman.mapper.TrainingRecordMapper;
import com.homework.driveman.service.ITrainingRecordService;
import org.springframework.stereotype.Service;

/** 学时记录业务实现 */
@Service
public class TrainingRecordServiceImpl extends ServiceImpl<TrainingRecordMapper, TrainingRecord> implements ITrainingRecordService {
}
