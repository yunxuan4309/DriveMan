package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.LicenseUpgrade;
import com.homework.driveman.service.ILicenseUpgradeService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 增驾申请控制器 — 学员端提交增驾申请，管理员审核
 */
@Tag(name = "增驾管理")
@RestController
@RequestMapping("/license-upgrades")
public class LicenseUpgradeController {

    @Autowired
    private ILicenseUpgradeService licenseUpgradeService;

    /**
     * 从请求中获取当前登录用户
     */
    private CurrentUser getCurrentUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute("currentUser");
    }

    // ==================== 学员端接口 ====================

    @RequireRole(1)
    @Operation(summary = "提交增驾申请", description = "学员申请增驾（同级增驾/升级增驾）。升级增驾需先上传驾驶证材料。请求体: { targetLicense, upgradeType, licenseFileId?, skipAgeCheck? }，skipAgeCheck=true 跳过年龄/路径/驾龄校验（演示用）")
    @PostMapping("/apply")
    public JsonResult<LicenseUpgrade> apply(HttpServletRequest request,
                                            @RequestBody Map<String, Object> body) {
        CurrentUser user = getCurrentUser(request);
        String targetLicense = (String) body.get("targetLicense");
        Integer upgradeType = body.get("upgradeType") != null
                ? ((Number) body.get("upgradeType")).intValue() : null;
        // licenseFileId 支持：单值 (123)、逗号分隔字符串 ("12,15")、数组 ([12,15])
        String licenseFileId = null;
        Object fileIdObj = body.get("licenseFileId");
        if (fileIdObj instanceof Number) {
            licenseFileId = String.valueOf(((Number) fileIdObj).intValue());
        } else if (fileIdObj instanceof String && !((String) fileIdObj).isEmpty()) {
            licenseFileId = (String) fileIdObj;
        } else if (fileIdObj instanceof java.util.List) {
            licenseFileId = ((java.util.List<?>) fileIdObj).stream()
                    .map(Object::toString).collect(java.util.stream.Collectors.joining(","));
        }
        boolean skipAgeCheck = body.get("skipAgeCheck") != null
                && Boolean.TRUE.equals(body.get("skipAgeCheck"));
        LicenseUpgrade upgrade = licenseUpgradeService.apply(
                user.getUserId(), targetLicense, upgradeType, licenseFileId, skipAgeCheck);
        return JsonResult.ok(upgrade);
    }

    @RequireRole(1)
    @Operation(summary = "查看我的增驾申请",
            description = "学员分页查看自己的增驾申请记录，支持按状态/目标车型筛选")
    @GetMapping("/my")
    public JsonResult<Page<LicenseUpgrade>> listMyUpgrades(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "5") @Parameter(description = "每页条数") int size,
            @RequestParam(required = false) @Parameter(description = "审核状态: 0-待审核,1-通过,2-不通过") Integer status,
            @RequestParam(required = false) @Parameter(description = "目标准驾车型") String targetLicense) {
        CurrentUser user = getCurrentUser(request);
        Page<LicenseUpgrade> result = licenseUpgradeService.pageMyUpgrades(
                new Page<>(page, size), user.getUserId(), status, targetLicense);
        return JsonResult.ok(result);
    }

    // ==================== 管理员端接口 ====================

    @RequireRole(3)
    @Operation(summary = "分页查询增驾申请",
            description = "含学员姓名，支持学员姓名/原车型/目标车型/审核状态/考试状态/提交时间范围搜索")
    @GetMapping
    public JsonResult<Page<Map<String, Object>>> listAll(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页条数") int size,
            @RequestParam(required = false) @Parameter(description = "学员姓名关键字") String keyword,
            @RequestParam(required = false) @Parameter(description = "原车型") String originalLicense,
            @RequestParam(required = false) @Parameter(description = "目标车型") String targetLicense,
            @RequestParam(required = false) @Parameter(description = "审核状态：0-待审核,1-通过,2-不通过") Integer status,
            @RequestParam(required = false) @Parameter(description = "考试状态：0-待考试,1-通过,2-不通过") Integer examStatus,
            @RequestParam(required = false) @Parameter(description = "提交时间起 yyyy-MM-dd") String createTimeStart,
            @RequestParam(required = false) @Parameter(description = "提交时间止 yyyy-MM-dd") String createTimeEnd) {
        LocalDateTime start = createTimeStart != null ? LocalDateTime.parse(createTimeStart + "T00:00:00") : null;
        LocalDateTime end = createTimeEnd != null ? LocalDateTime.parse(createTimeEnd + "T23:59:59") : null;
        return JsonResult.ok(licenseUpgradeService.pageSearch(new Page<>(page, size),
                keyword, originalLicense, targetLicense, status, examStatus, start, end));
    }

    @RequireRole(3)
    @Operation(summary = "审核增驾申请", description = "管理员审核增驾申请（通过/不通过）。skipSubjects 为跳过的科目编号(逗号分隔如1,3)，适用于同级增驾等无需全科考试的场景，不能跳过全部科目")
    @PutMapping("/{id}/audit")
    public JsonResult<Void> audit(@PathVariable Integer id,
                                  @RequestParam Integer status,
                                  @RequestParam(required = false) String remark,
                                  @RequestParam(required = false) String skipSubjects) {
        licenseUpgradeService.audit(id, status, remark, skipSubjects);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "查询增驾进度", description = "查看某增驾申请的支付状态、免考科目、已通过/待考科目")
    @GetMapping("/{id}/progress")
    public JsonResult<Map<String, Object>> progress(@PathVariable Integer id) {
        return JsonResult.ok(licenseUpgradeService.getProgress(id));
    }

    @RequireRole(3)
    @Operation(summary = "完成增驾",
            description = "检查增驾费已支付 + 所有未免考科目已通过考试，满足条件后更新学员车型。不满足时返回错误提示")
    @PostMapping("/{id}/complete")
    public JsonResult<Void> complete(@PathVariable Integer id) {
        licenseUpgradeService.completeUpgrade(id);
        return JsonResult.ok();
    }
}
