package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.FeeStandard;
import com.homework.driveman.service.IFeeStandardService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 费用标准管理控制器 — 管理员维护各车型各科目的收费标准
 * 管理员可以新增/修改/删除/查询费用标准
 */
@Tag(name = "费用标准管理")
@RestController
@RequestMapping("/fee-standards")
public class FeeStandardController {

    @Autowired
    private IFeeStandardService feeStandardService;

    @RequireRole(3)
    @Operation(summary = "分页查询费用标准", description = "支持按车型筛选，分页返回费用标准列表")
    @GetMapping
    public JsonResult<Page<FeeStandard>> list(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页条数") int size,
            @RequestParam(required = false) @Parameter(description = "车型筛选（C1/C2/...）") String licenseType) {
        return JsonResult.ok(feeStandardService.pageWithDetails(new Page<>(page, size), licenseType));
    }

    @RequireRole(3)
    @Operation(summary = "根据ID查询费用标准")
    @GetMapping("/{id}")
    public JsonResult<FeeStandard> getById(@PathVariable Integer id) {
        FeeStandard feeStandard = feeStandardService.getById(id);
        return JsonResult.ok(feeStandard);
    }

    @RequireRole(3)
    @Operation(summary = "按车型查询费用标准", description = "查询指定车型（如 C1/C2）的所有费用记录（含全包套餐和各科目费用）")
    @GetMapping("/type/{licenseType}")
    public JsonResult<java.util.List<FeeStandard>> getByLicenseType(@PathVariable String licenseType) {
        java.util.List<FeeStandard> list = feeStandardService.lambdaQuery()
                .eq(FeeStandard::getLicenseType, licenseType)
                .orderByAsc(FeeStandard::getSubject)
                .list();
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "新增费用标准", description = "添加某车型的费用条目（全包套餐 subject 传 null，各科目费用传对应值 1-4）")
    @PostMapping
    public JsonResult<Void> create(@RequestBody FeeStandard feeStandard) {
        feeStandardService.save(feeStandard);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改费用标准", description = "修改某条费用标准的金额或说明")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody FeeStandard feeStandard) {
        feeStandard.setId(id);
        feeStandardService.updateById(feeStandard);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除费用标准", description = "删除某条费用标准记录")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        feeStandardService.removeById(id);
        return JsonResult.ok();
    }
}
