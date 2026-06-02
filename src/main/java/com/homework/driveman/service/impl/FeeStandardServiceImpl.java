package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.FeeStandard;
import com.homework.driveman.mapper.FeeStandardMapper;
import com.homework.driveman.service.IFeeStandardService;
import org.springframework.stereotype.Service;

/**
 * 费用标准业务实现
 * 继承 MyBatis-Plus ServiceImpl，提供基础 CRUD
 */
@Service
public class FeeStandardServiceImpl extends ServiceImpl<FeeStandardMapper, FeeStandard>
        implements IFeeStandardService {
}