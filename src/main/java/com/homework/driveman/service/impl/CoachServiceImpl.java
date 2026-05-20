package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.service.ICoachService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 教练业务实现
 * 推荐算法：准教车型匹配 → 按评分降序
 */
@Service
public class CoachServiceImpl extends ServiceImpl<CoachMapper, Coach> implements ICoachService {

    @Override
    public List<Coach> recommend(String licenseType, int topN) {
        LambdaQueryWrapper<Coach> wrapper = new LambdaQueryWrapper<Coach>()
                // 准教车型包含学员报考车型（vehicle_type 字段逗号分隔，如 "C1,C2"）
                .apply("FIND_IN_SET({0}, vehicle_type)", licenseType)
                .orderByDesc(Coach::getRating)
                .last("LIMIT " + topN);
        return list(wrapper);
    }
}
