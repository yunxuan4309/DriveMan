package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.SpecialExamRecord;
import com.homework.driveman.mapper.SpecialExamRecordMapper;
import com.homework.driveman.service.ISpecialExamRecordService;
import org.springframework.stereotype.Service;

/** 特种车辆考试记录业务实现 */
@Service
public class SpecialExamRecordServiceImpl extends ServiceImpl<SpecialExamRecordMapper, SpecialExamRecord>
        implements ISpecialExamRecordService {
}
