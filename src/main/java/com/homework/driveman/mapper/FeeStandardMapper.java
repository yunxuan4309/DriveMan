package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.FeeStandard;
import org.springframework.stereotype.Repository;

/**
 * 费用标准表 Mapper
 * 提供费用标准的增删改查基础操作
 */
@Repository
public interface FeeStandardMapper extends BaseMapper<FeeStandard> {
}