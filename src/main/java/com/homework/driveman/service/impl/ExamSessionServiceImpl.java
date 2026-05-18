package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.ExamSession;
import com.homework.driveman.mapper.ExamSessionMapper;
import com.homework.driveman.service.IExamSessionService;
import org.springframework.stereotype.Service;

/** 考试场次业务实现 */
@Service
public class ExamSessionServiceImpl extends ServiceImpl<ExamSessionMapper, ExamSession> implements IExamSessionService {
}
