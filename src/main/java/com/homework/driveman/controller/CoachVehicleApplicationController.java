package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.CoachVehicleApplication;
import com.homework.driveman.service.ICoachVehicleApplicationService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    @Operation(summary = "查询待审核的车型变更申请",
            description = "返回所有 status=0 的申请，含教练姓名")
    @GetMapping("/pending")
    public JsonResult<List<Map<String, Object>>> listPending() {
        return JsonResult.ok(coachVehicleApplicationService.listPending());
    }

    @RequireRole(3)
    @Operation(summary = "分页查询所有车型变更申请记录",
            description = "可查看全部历史记录，含教练姓名")
    @GetMapping
    public JsonResult<Page<Map<String, Object>>> listAll(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return JsonResult.ok(coachVehicleApplicationService.listAll(new Page<>(page, size)));
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
