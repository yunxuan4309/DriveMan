package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.LicenseUpgrade;
import com.homework.driveman.service.ILicenseUpgradeService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @Operation(summary = "提交增驾申请", description = "学员申请增驾（同级增驾/升级增驾）。升级增驾需先上传驾驶证材料，再提交申请")
    @PostMapping("/apply")
    public JsonResult<LicenseUpgrade> apply(HttpServletRequest request,
                                            @RequestParam String targetLicense,
                                            @RequestParam Integer upgradeType,
                                            @RequestParam(required = false) Integer licenseFileId) {
        CurrentUser user = getCurrentUser(request);
        LicenseUpgrade upgrade = licenseUpgradeService.apply(user.getUserId(), targetLicense, upgradeType, licenseFileId);
        return JsonResult.ok(upgrade);
    }

    @RequireRole(1)
    @Operation(summary = "查看我的增驾申请", description = "学员查看自己的增驾申请记录")
    @GetMapping("/my")
    public JsonResult<List<LicenseUpgrade>> listMyUpgrades(HttpServletRequest request) {
        CurrentUser user = getCurrentUser(request);
        List<LicenseUpgrade> list = licenseUpgradeService.listByStudent(user.getUserId());
        return JsonResult.ok(list);
    }

    // ==================== 管理员端接口 ====================

    @RequireRole(3)
    @Operation(summary = "查询所有增驾申请", description = "管理员查看所有增驾申请")
    @GetMapping
    public JsonResult<List<LicenseUpgrade>> listAll() {
        List<LicenseUpgrade> list = licenseUpgradeService.lambdaQuery()
                .orderByDesc(LicenseUpgrade::getCreateTime)
                .list();
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "审核增驾申请", description = "管理员审核增驾申请（通过/不通过）")
    @PutMapping("/{id}/audit")
    public JsonResult<Void> audit(@PathVariable Integer id,
                                  @RequestParam Integer status,
                                  @RequestParam(required = false) String remark) {
        licenseUpgradeService.audit(id, status, remark);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "录入增驾考试成绩", description = "管理员录入增驾考试结果（通过/不通过），通过后更新学员车型")
    @PutMapping("/{id}/exam-result")
    public JsonResult<Void> recordExamResult(@PathVariable Integer id,
                                             @RequestParam Integer examStatus,
                                             @RequestParam(required = false) String examRemark) {
        licenseUpgradeService.recordExamResult(id, examStatus, examRemark);
        return JsonResult.ok();
    }
}
