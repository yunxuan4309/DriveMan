package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.service.ICoachVehicleApplicationService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 教练准教车型变更审核控制器 — 管理员审核教练的车型变更申请
 */
@Tag(name = "教练准教车型变更管理")
@RestController
@RequestMapping("/coach-vehicle-applications")
public class CoachVehicleApplicationController {

    @Autowired
    private ICoachVehicleApplicationService coachVehicleApplicationService;

    @RequireRole(3)
    @Operation(summary = "分页查询待审核的车型变更申请",
            description = "含教练姓名，支持教练姓名/当前车型/申请车型/申请时间范围搜索")
    @GetMapping("/pending")
    public JsonResult<Page<Map<String, Object>>> listPending(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页条数") int size,
            @RequestParam(required = false) @Parameter(description = "教练姓名关键字") String keyword,
            @RequestParam(required = false) @Parameter(description = "当前车型") String currentVehicleType,
            @RequestParam(required = false) @Parameter(description = "申请车型") String requestedVehicleType,
            @RequestParam(required = false) @Parameter(description = "申请时间起 yyyy-MM-dd") String applyTimeStart,
            @RequestParam(required = false) @Parameter(description = "申请时间止 yyyy-MM-dd") String applyTimeEnd) {
        LocalDateTime start = applyTimeStart != null ? LocalDateTime.parse(applyTimeStart + "T00:00:00") : null;
        LocalDateTime end = applyTimeEnd != null ? LocalDateTime.parse(applyTimeEnd + "T23:59:59") : null;
        return JsonResult.ok(coachVehicleApplicationService.pagePending(
                new Page<>(page, size), keyword, currentVehicleType, requestedVehicleType, start, end));
    }

    @RequireRole(3)
    @Operation(summary = "分页查询全部车型变更申请记录",
            description = "支持教练姓名/车型/状态/审核时间范围搜索")
    @GetMapping
    public JsonResult<Page<Map<String, Object>>> listAll(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页条数") int size,
            @RequestParam(required = false) @Parameter(description = "教练姓名关键字") String keyword,
            @RequestParam(required = false) @Parameter(description = "申请车型") String vehicleType,
            @RequestParam(required = false) @Parameter(description = "状态：0-待审核, 1-通过, 2-拒绝") Integer status,
            @RequestParam(required = false) @Parameter(description = "审核时间起 yyyy-MM-dd") String auditTimeStart,
            @RequestParam(required = false) @Parameter(description = "审核时间止 yyyy-MM-dd") String auditTimeEnd) {
        LocalDateTime auditStart = auditTimeStart != null ? LocalDateTime.parse(auditTimeStart + "T00:00:00") : null;
        LocalDateTime auditEnd = auditTimeEnd != null ? LocalDateTime.parse(auditTimeEnd + "T23:59:59") : null;
        return JsonResult.ok(coachVehicleApplicationService.pageAll(
                new Page<>(page, size), keyword, vehicleType, status, auditStart, auditEnd));
    }

    @RequireRole(3)
    @Operation(summary = "审核车型变更申请",
            description = "通过(pass=true)时自动更新教练的 vehicle_type；拒绝(pass=false)时须填写 auditReason")
    @PutMapping("/{id}/audit")
    public JsonResult<Void> audit(@PathVariable Integer id,
                                  @RequestParam boolean pass,
                                  @RequestParam(required = false) String auditReason) {
        coachVehicleApplicationService.audit(id, pass, auditReason);
        return JsonResult.ok();
    }
}
