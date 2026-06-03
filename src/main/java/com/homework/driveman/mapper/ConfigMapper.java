package com.homework.driveman.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/** 系统配置表 Mapper — 读取 config 表中的键值对 */
@Repository
public interface ConfigMapper {

    /** 根据 config_key 读取配置值 */
    @Select("SELECT config_value FROM config WHERE config_key = #{key}")
    String getConfigValue(@Param("key") String key);
}
