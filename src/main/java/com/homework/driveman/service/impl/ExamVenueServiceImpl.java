package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.ExamVenue;
import com.homework.driveman.mapper.ExamVenueMapper;
import com.homework.driveman.service.IExamVenueService;
import org.springframework.stereotype.Service;

/** 考场信息业务实现 */
@Service
public class ExamVenueServiceImpl extends ServiceImpl<ExamVenueMapper, ExamVenue> implements IExamVenueService {
}
