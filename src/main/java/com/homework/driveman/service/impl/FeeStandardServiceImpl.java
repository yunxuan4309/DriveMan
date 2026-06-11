package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    @Override
    public Page<FeeStandard> pageWithDetails(Page<FeeStandard> page, String licenseType) {
        LambdaQueryWrapper<FeeStandard> wrapper = new LambdaQueryWrapper<>();
        if (licenseType != null && !licenseType.isEmpty()) {
            wrapper.eq(FeeStandard::getLicenseType, licenseType);
        }
        wrapper.orderByAsc(FeeStandard::getLicenseType)
                .orderByAsc(FeeStandard::getSubject);
        return baseMapper.selectPage(page, wrapper);
    }
}
