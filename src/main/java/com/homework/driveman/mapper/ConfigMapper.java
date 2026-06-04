package com.homework.driveman.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/** 系统配置表 Mapper — 读取/写入 config 表中的键值对 */
@Repository
public interface ConfigMapper {

    /** 根据 config_key 读取配置值 */
    @Select("SELECT config_value FROM config WHERE config_key = #{key}")
    String getConfigValue(@Param("key") String key);

    /** 写入配置值（存在则更新，不存在则插入） */
    @Insert("REPLACE INTO config(config_key, config_value, description) VALUES(#{key}, #{value}, #{description})")
    void setConfigValue(@Param("key") String key, @Param("value") String value, @Param("description") String description);
}
