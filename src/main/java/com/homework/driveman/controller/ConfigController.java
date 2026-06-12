package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.Config;
import com.homework.driveman.service.IConfigService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 系统配置管理控制器 — 管理员 CRUD 操作 config 表
 */
@Tag(name = "系统配置管理")
@RestController
@RequestMapping("/configs")
public class ConfigController {

    @Autowired
    private IConfigService configService;

    @RequireRole(3)
    @Operation(summary = "分页查询配置项",
            description = "支持关键字模糊匹配 config_key / config_value / description")
    @GetMapping
    public JsonResult<Page<Config>> list(@RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
                                          @RequestParam(defaultValue = "10") @Parameter(description = "每页条数") int size,
                                          @RequestParam(required = false) @Parameter(description = "关键字，模糊匹配键/值/说明") String keyword) {
        return JsonResult.ok(configService.pageSearch(new Page<>(page, size), keyword));
    }

    @RequireRole(3)
    @Operation(summary = "根据configKey查询配置")
    @GetMapping("/{configKey}")
    public JsonResult<Config> getByKey(@PathVariable String configKey) {
        return JsonResult.ok(configService.getById(configKey));
    }

    @RequireRole(3)
    @Operation(summary = "新增配置项")
    @PostMapping
    public JsonResult<Void> create(@RequestBody Config config) {
        configService.save(config);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改配置项")
    @PutMapping("/{configKey}")
    public JsonResult<Void> update(@PathVariable String configKey, @RequestBody Config config) {
        config.setConfigKey(configKey);
        configService.updateById(config);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除配置项")
    @DeleteMapping("/{configKey}")
    public JsonResult<Void> delete(@PathVariable String configKey) {
        configService.removeById(configKey);
        return JsonResult.ok();
    }
}
