package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.Coach;

import java.util.List;

/** 教练业务接口 */
public interface ICoachService extends IService<Coach> {

    /**
     * 为学员推荐教练（推荐算法）
     * 匹配规则: 准教车型包含学员报考车型 → 按评分降序
     * @param licenseType 学员报考车型 (C1/C2/...)
     * @param topN        返回前 N 条
     * @return 推荐教练列表（只包含 vehicleType 匹配的）
     */
    List<Coach> recommend(String licenseType, int topN);
}
