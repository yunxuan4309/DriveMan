package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.LicenseConfig;
import com.homework.driveman.mapper.LicenseConfigMapper;
import com.homework.driveman.service.ILicenseConfigService;
import org.springframework.stereotype.Service;

/** 车型配置业务实现 */
@Service
public class LicenseConfigServiceImpl extends ServiceImpl<LicenseConfigMapper, LicenseConfig>
        implements ILicenseConfigService {

    @Override
    public Page<LicenseConfig> pageWithDetails(Page<LicenseConfig> page, String licenseType) {
        LambdaQueryWrapper<LicenseConfig> wrapper = new LambdaQueryWrapper<>();
        if (licenseType != null && !licenseType.isEmpty()) {
            wrapper.eq(LicenseConfig::getLicenseType, licenseType);
        }
        wrapper.orderByAsc(LicenseConfig::getSortOrder);
        return baseMapper.selectPage(page, wrapper);
    }
}
