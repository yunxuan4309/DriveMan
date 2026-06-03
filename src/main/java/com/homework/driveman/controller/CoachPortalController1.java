package com.homework.driveman.controller;

import com.homework.driveman.dto.AppointmentActionDTO;
import com.homework.driveman.dto.AvailableTimeDTO;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.service.IAppointmentService;
import com.homework.driveman.service.ICoachService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.utils.JwtUtils;
import com.homework.driveman.vo.CoachRatingVO;
import com.homework.driveman.vo.CoachWorkloadVO;
import com.homework.driveman.vo.StudentInfoVO;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.homework.driveman.dto.RecordHoursDTO;
import com.homework.driveman.service.ITrainingRecordService;

import java.util.List;

/**
 * 教练端个人业务控制器（已禁用）
 * 功能已合并至 CoachPortalController (/coach-portal)
 * 保留代码备用，如需启用，取消下方 @Tag/@RestController/@RequestMapping 注释即可
 */
//@Tag(name = "教练端业务")
//@RestController
//@RequestMapping("/coach")
public class CoachPortalController1 {

    @Autowired
    private ICoachService coachService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ITrainingRecordService trainingRecordService;

    @Autowired
    private IAppointmentService appointmentService;



    /**
     * 从请求头解析 Token 获取当前登录教练的 coachId
     */
    private Integer getCurrentCoachId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("未提供有效的 Authorization 头");
        }
        String token = authHeader.substring(7);
        CurrentUser currentUser = jwtUtils.parseToken(token);
        if (currentUser == null) {
            throw new RuntimeException("Token 无效或已过期");
        }
        if (currentUser.getRole() != 2) {
            throw new RuntimeException("当前用户不是教练，无权访问");
        }
        // 根据 userId 查询 coachId
        Coach coach = coachService.lambdaQuery()
                .eq(Coach::getUserId, currentUser.getUserId())
                .one();
        if (coach == null) {
            throw new RuntimeException("教练信息不存在");
        }
        return coach.getCoachId();
    }

    @Operation(summary = "查看名下学员列表")
    @GetMapping("/students")
    public JsonResult<List<StudentInfoVO>> listMyStudents(HttpServletRequest request) {
        Integer coachId = getCurrentCoachId(request);
        return JsonResult.ok(coachService.getMyStudents(coachId));
    }

    @Operation(summary = "录入学时")
    @PostMapping("/training/record")
    public JsonResult<Void> recordTrainingHours(@RequestBody @Valid RecordHoursDTO dto,
                                                HttpServletRequest request) {
        Integer coachId = getCurrentCoachId(request);
        trainingRecordService.recordTrainingHours(coachId, dto);
        return JsonResult.ok();
    }

    @Operation(summary = "确认约课")
    @PutMapping("/appointment/confirm")
    public JsonResult<Void> confirmAppointment(@RequestBody @Valid AppointmentActionDTO dto,
                                               HttpServletRequest request) {
        Integer coachId = getCurrentCoachId(request);
        appointmentService.confirmAppointment(dto.getAppointmentId(), coachId);
        return JsonResult.ok();
    }

    @Operation(summary = "拒绝约课")
    @PutMapping("/appointment/reject")
    public JsonResult<Void> rejectAppointment(@RequestBody @Valid AppointmentActionDTO dto,
                                              HttpServletRequest request) {
        Integer coachId = getCurrentCoachId(request);
        appointmentService.rejectAppointment(dto.getAppointmentId(), coachId, dto.getRejectReason());
        return JsonResult.ok();
    }

    @Operation(summary = "设置空闲时间")
    @PutMapping("/available-time")
    public JsonResult<Void> setAvailableTime(@RequestBody @Valid AvailableTimeDTO dto,
                                             HttpServletRequest request) {
        Integer coachId = getCurrentCoachId(request);
        coachService.setAvailableTime(coachId, dto.getAvailableTime());
        return JsonResult.ok();
    }

    @Operation(summary = "查看个人工作量统计")
    @GetMapping("/workload")
    public JsonResult<CoachWorkloadVO> getWorkload(HttpServletRequest request) {
        Integer coachId = getCurrentCoachId(request);
        return JsonResult.ok(coachService.getWorkload(coachId));
    }

    @Operation(summary = "查看个人评分")
    @GetMapping("/rating")
    public JsonResult<CoachRatingVO> getRating(HttpServletRequest request) {
        Integer coachId = getCurrentCoachId(request);
        return JsonResult.ok(coachService.getRating(coachId));
    }
}