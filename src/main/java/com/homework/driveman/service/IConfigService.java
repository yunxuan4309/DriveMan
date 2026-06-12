package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.Config;

/**
 * 系统配置服务 — config 表的 CRUD + 便捷键值访问
 */
public interface IConfigService extends IService<Config> {

    /** 读取配置值，不存在返回 null */
    String getConfigValue(String key);

    /** 读取配置值，不存在返回默认值 */
    String getConfigValue(String key, String defaultValue);

    /** 写入配置值（存在则更新，不存在则新增） */
    void setConfigValue(String key, String value, String description);

    /** 分页查询配置项，关键字模糊匹配 config_key / config_value / description */
    Page<Config> pageSearch(Page<?> page, String keyword);
}
