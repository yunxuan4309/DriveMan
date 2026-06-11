package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.LicenseConfig;

/** 车型配置业务接口 */
public interface ILicenseConfigService extends IService<LicenseConfig> {

    /**
     * 分页查询车型配置，支持按车型筛选
     * @param page        分页参数
     * @param licenseType 可选，车型筛选（C1/C2/...）
     */
    Page<LicenseConfig> pageWithDetails(Page<LicenseConfig> page, String licenseType);
}
