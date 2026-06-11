package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.FeeStandard;

/**
 * 费用标准业务接口
 * 提供费用标准的 CRUD 操作
 */
public interface IFeeStandardService extends IService<FeeStandard> {

    /**
     * 分页查询费用标准，支持按车型筛选
     * @param page        分页参数
     * @param licenseType 可选，车型筛选（C1/C2/...）
     */
    Page<FeeStandard> pageWithDetails(Page<FeeStandard> page, String licenseType);
}