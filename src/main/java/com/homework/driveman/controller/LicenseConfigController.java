package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.LicenseConfig;
import com.homework.driveman.service.ILicenseConfigService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 车型配置管理控制器 — 管理员维护各车型的科目学时和考试项目
 */
@Tag(name = "车型配置管理")
@RestController
@RequestMapping("/license-configs")
public class LicenseConfigController {

    @Autowired
    private ILicenseConfigService licenseConfigService;

    @RequireRole(3)
    @Operation(summary = "查询所有车型配置")
    @GetMapping
    public JsonResult<List<LicenseConfig>> list() {
        return JsonResult.ok(licenseConfigService.list());
    }

    @RequireRole(3)
    @Operation(summary = "根据ID查询配置")
    @GetMapping("/{id}")
    public JsonResult<LicenseConfig> getById(@PathVariable Integer id) {
        return JsonResult.ok(licenseConfigService.getById(id));
    }

    @RequireRole(3)
    @Operation(summary = "按车型查询配置",
            description = "查询指定车型（如 C1/B1）的完整科目配置")
    @GetMapping("/type/{licenseType}")
    public JsonResult<List<LicenseConfig>> getByLicenseType(@PathVariable String licenseType) {
        List<LicenseConfig> list = licenseConfigService.lambdaQuery()
                .eq(LicenseConfig::getLicenseType, licenseType)
                .orderByAsc(LicenseConfig::getSortOrder)
                .list();
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "新增车型配置")
    @PostMapping
    public JsonResult<Void> create(@RequestBody LicenseConfig config) {
        licenseConfigService.save(config);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改车型配置")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody LicenseConfig config) {
        config.setId(id);
        licenseConfigService.updateById(config);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除车型配置")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        licenseConfigService.removeById(id);
        return JsonResult.ok();
    }
}
