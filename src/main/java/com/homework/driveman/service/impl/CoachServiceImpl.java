package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.service.ICoachService;
import org.springframework.stereotype.Service;

/** 教练业务实现 */
@Service
public class CoachServiceImpl extends ServiceImpl<CoachMapper, Coach> implements ICoachService {
}
