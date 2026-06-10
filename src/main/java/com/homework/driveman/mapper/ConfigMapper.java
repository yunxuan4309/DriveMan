package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.Config;
import org.springframework.stereotype.Repository;

/** 系统配置表 Mapper — 继承 MyBatis-Plus BaseMapper 提供 CRUD */
@Repository
public interface ConfigMapper extends BaseMapper<Config> {
}
