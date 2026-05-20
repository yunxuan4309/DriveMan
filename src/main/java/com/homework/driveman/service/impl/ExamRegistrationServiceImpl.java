package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.ExamRegistration;
import com.homework.driveman.mapper.ExamRegistrationMapper;
import com.homework.driveman.service.IExamRegistrationService;
import org.springframework.stereotype.Service;

/** 考试报名业务实现 */
@Service
public class ExamRegistrationServiceImpl extends ServiceImpl<ExamRegistrationMapper, ExamRegistration>
        implements IExamRegistrationService {
}
