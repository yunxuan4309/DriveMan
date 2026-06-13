package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.CoachSchedule;
import com.homework.driveman.entity.User;
import com.homework.driveman.entity.Venue;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.mapper.VenueMapper;
import com.homework.driveman.service.ICoachScheduleService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private CoachMapper coachMapper;

    @Autowired
    private UserMapper userMapper;

    private CurrentUser getCurrentUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute("currentUser");
    }

    // ==================== 学员端 ====================

    @RequireRole(1)
    @Operation(summary = "查询可约时段（学员端）",
            description = "自动查询学员绑定教练的已通过排班，仅返回名额未满且时间在未来的排班。可选按车型筛选")
    @GetMapping("/available")
    public JsonResult<List<Map<String, Object>>> listAvailable(HttpServletRequest request,
                                                                @RequestParam(required = false) String licenseType) {
        CurrentUser user = getCurrentUser(request);
        List<CoachSchedule> list = scheduleService.listAvailableForStudent(user.getUserId(), licenseType);
        List<Map<String, Object>> result = new ArrayList<>();
        for (CoachSchedule s : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("coachId", s.getCoachId());
            m.put("vehicleId", s.getVehicleId());
            m.put("venueId", s.getVenueId());
            m.put("licenseType", s.getLicenseType());
            m.put("subject", s.getSubject());
            m.put("startTime", s.getStartTime());
            m.put("endTime", s.getEndTime());
            m.put("maxStudents", s.getMaxStudents());
            m.put("bookedCount", s.getBookedCount());
            m.put("status", s.getStatus());
            // 查询场地名称
            Venue venue = venueMapper.selectById(s.getVenueId());
            m.put("venueName", venue != null ? venue.getName() : null);
            // 查询教练姓名
            Coach coach = coachMapper.selectById(s.getCoachId());
            if (coach != null) {
                User coachUser = userMapper.selectById(coach.getUserId());
                m.put("coachName", coachUser != null ? coachUser.getRealName() : null);
            }
            result.add(m);
        }
        return JsonResult.ok(result);
    }

    // ==================== 管理员端 ====================

    @RequireRole(3)
    @Operation(summary = "分页查询排班列表（管理员端）",
            description = "含教练姓名/车牌号/场地名称，支持多条件筛选：教练姓名、车牌号、场地、车型、状态、开始时间范围")
    @GetMapping
    public JsonResult<Page<Map<String, Object>>> listAll(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(required = false) @Parameter(description = "教练姓名关键字") String keyword,
                                                          @RequestParam(required = false) @Parameter(description = "车牌号关键字") String plateNumber,
                                                          @RequestParam(required = false) @Parameter(description = "场地名称关键字") String venueName,
                                                          @RequestParam(required = false) @Parameter(description = "培训车型") String licenseType,
                                                          @RequestParam(required = false) @Parameter(description = "排班状态") Integer status,
                                                          @RequestParam(required = false) @Parameter(description = "开始时间起 yyyy-MM-dd") String startDateStart,
                                                          @RequestParam(required = false) @Parameter(description = "开始时间止 yyyy-MM-dd") String startDateEnd) {
        LocalDateTime startStart = startDateStart != null ? LocalDateTime.parse(startDateStart + "T00:00:00") : null;
        LocalDateTime startEnd = startDateEnd != null ? LocalDateTime.parse(startDateEnd + "T23:59:59") : null;
        return JsonResult.ok(scheduleService.pageSearch(new Page<>(page, size),
                null, keyword, plateNumber, venueName, licenseType, status, startStart, startEnd));
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
