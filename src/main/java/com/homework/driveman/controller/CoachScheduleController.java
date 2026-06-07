package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.CoachSchedule;
import com.homework.driveman.service.ICoachScheduleService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 排班管理控制器 — 管理员审核 + 学员查询可约时段
 * 教练端的申请/查看端点位于 CoachPortalController
 */
@Tag(name = "排班管理")
@RestController
@RequestMapping("/schedules")
public class CoachScheduleController {

    @Autowired
    private ICoachScheduleService scheduleService;

    private CurrentUser getCurrentUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute("currentUser");
    }

    // ==================== 学员端 ====================

    @RequireRole(1)
    @Operation(summary = "查询可约时段（学员端）",
            description = "自动查询学员绑定教练的已通过排班，仅返回名额未满且时间在未来的排班。可选按车型筛选")
    @GetMapping("/available")
    public JsonResult<List<CoachSchedule>> listAvailable(HttpServletRequest request,
                                                          @RequestParam(required = false) String licenseType) {
        CurrentUser user = getCurrentUser(request);
        List<CoachSchedule> list = scheduleService.listAvailableForStudent(user.getUserId(), licenseType);
        return JsonResult.ok(list);
    }

    // ==================== 管理员端 ====================

    @RequireRole(3)
    @Operation(summary = "查询所有排班（管理员端）",
            description = "支持按教练/车辆/状态/日期范围筛选")
    @GetMapping
    public JsonResult<Page<CoachSchedule>> listAll(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size,
                                                    @RequestParam(required = false) Integer coachId,
                                                    @RequestParam(required = false) Integer vehicleId,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) String startDate,
                                                    @RequestParam(required = false) String endDate) {
        var wrapper = scheduleService.lambdaQuery()
                .eq(coachId != null, CoachSchedule::getCoachId, coachId)
                .eq(vehicleId != null, CoachSchedule::getVehicleId, vehicleId)
                .eq(status != null, CoachSchedule::getStatus, status)
                .orderByDesc(CoachSchedule::getStartTime);

        if (startDate != null) {
            wrapper.ge(CoachSchedule::getStartTime, java.time.LocalDateTime.parse(startDate + "T00:00:00"));
        }
        if (endDate != null) {
            wrapper.le(CoachSchedule::getStartTime, java.time.LocalDateTime.parse(endDate + "T23:59:59"));
        }

        return JsonResult.ok(scheduleService.page(new Page<>(page, size), wrapper));
    }

    @RequireRole(3)
    @Operation(summary = "查看待审核排班列表")
    @GetMapping("/pending")
    public JsonResult<List<CoachSchedule>> listPending() {
        List<CoachSchedule> list = scheduleService.lambdaQuery()
                .eq(CoachSchedule::getStatus, 0)
                .orderByAsc(CoachSchedule::getStartTime)
                .list();
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "根据ID查询排班详情")
    @GetMapping("/{id}")
    public JsonResult<CoachSchedule> getById(@PathVariable Integer id) {
        return JsonResult.ok(scheduleService.getById(id));
    }

    @RequireRole(3)
    @Operation(summary = "审核排班", description = "status=1 通过, status=2 拒绝，需填写审核备注")
    @PutMapping("/{id}/audit")
    public JsonResult<Void> audit(@PathVariable Integer id,
                                   @RequestParam Integer status,
                                   @RequestParam(required = false) String remark) {
        scheduleService.audit(id, status, remark);
        return JsonResult.ok();
    }
}
