package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.FamiliarizationRecord;
import com.homework.driveman.service.IFamiliarizationRecordService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 合场管理控制器 — 学员申请、支付、管理员安排
 */
@Tag(name = "合场管理")
@RestController
@RequestMapping("/familiarizations")
public class FamiliarizationController {

    @Autowired
    private IFamiliarizationRecordService familiarizationRecordService;

    // ==================== 学员端接口 ====================

    @RequireRole(1)
    @Operation(summary = "申请合场",
            description = "学员选择考试场次和用车类型（1-教练车/2-考试车），系统自动按 fee_standard 定价并生成待支付账单")
    @PostMapping("/apply")
    public JsonResult<FamiliarizationRecord> apply(@RequestParam Integer examSessionId,
                                                    @RequestParam Integer carType,
                                                    HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        return JsonResult.ok(familiarizationRecordService.apply(currentUser.getUserId(), examSessionId, carType));
    }

    @RequireRole(1)
    @Operation(summary = "支付合场", description = "学员支付自己的待支付合场记录（模拟支付）")
    @PutMapping("/{id}/pay")
    public JsonResult<FamiliarizationRecord> pay(@PathVariable Integer id, HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        return JsonResult.ok(familiarizationRecordService.pay(id, currentUser.getUserId()));
    }

    @RequireRole(1)
    @Operation(summary = "我的合场记录", description = "查看自己的合场记录列表（含场次信息、教练姓名）")
    @GetMapping("/my")
    public JsonResult<List<Map<String, Object>>> getMyRecords(HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        return JsonResult.ok(familiarizationRecordService.listMyRecords(currentUser.getUserId()));
    }

    // ==================== 管理员端接口 ====================

    @RequireRole(3)
    @Operation(summary = "合场记录列表", description = "所有合场记录，含场次信息、教练姓名")
    @GetMapping
    public JsonResult<List<Map<String, Object>>> listAll() {
        return JsonResult.ok(familiarizationRecordService.listAll());
    }

    @RequireRole(3)
    @Operation(summary = "安排合场时间", description = "管理员为已支付的合场记录安排具体时间，格式 yyyy-MM-dd HH:mm:ss")
    @PutMapping("/{id}/schedule")
    public JsonResult<FamiliarizationRecord> schedule(@PathVariable Integer id,
                                                      @RequestParam String scheduledTime) {
        return JsonResult.ok(familiarizationRecordService.schedule(id, scheduledTime));
    }

    @RequireRole(3)
    @Operation(summary = "合场完成", description = "标记合场已完成")
    @PutMapping("/{id}/complete")
    public JsonResult<FamiliarizationRecord> complete(@PathVariable Integer id) {
        return JsonResult.ok(familiarizationRecordService.complete(id));
    }

    @RequireRole(3)
    @Operation(summary = "取消合场", description = "取消合场记录（已完成的不允许取消）")
    @PutMapping("/{id}/cancel")
    public JsonResult<FamiliarizationRecord> cancel(@PathVariable Integer id) {
        return JsonResult.ok(familiarizationRecordService.cancel(id));
    }
}
