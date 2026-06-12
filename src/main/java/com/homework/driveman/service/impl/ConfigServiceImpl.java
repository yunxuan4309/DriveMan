package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.Config;
import com.homework.driveman.mapper.ConfigMapper;
import com.homework.driveman.service.IConfigService;
import org.springframework.stereotype.Service;

/**
 * 系统配置服务实现
 */
@Service
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, Config> implements IConfigService {

    @Override
    public String getConfigValue(String key) {
        Config config = getById(key);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public String getConfigValue(String key, String defaultValue) {
        String val = getConfigValue(key);
        return val != null ? val : defaultValue;
    }

    @Override
    public void setConfigValue(String key, String value, String description) {
        Config config = new Config();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDescription(description);
        saveOrUpdate(config);
    }

    @Override
    public Page<Config> pageSearch(Page<?> page, String keyword) {
        return baseMapper.selectPageWithKeyword(page, keyword);
    }
}
