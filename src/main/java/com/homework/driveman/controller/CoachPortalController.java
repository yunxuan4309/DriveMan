package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.dto.UpdateTimeSlotsDTO;
import com.homework.driveman.entity.Appointment;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.CoachSchedule;
import com.homework.driveman.entity.CoachApplication;
import com.homework.driveman.entity.StudentCoach;
import com.homework.driveman.entity.TrainingRecord;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.CoachApplicationMapper;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.StudentCoachMapper;
import com.homework.driveman.mapper.TrainingRecordMapper;
import com.homework.driveman.service.IAppointmentService;
import com.homework.driveman.service.ICoachPortalService;
import com.homework.driveman.service.ICoachVehicleApplicationService;
import com.homework.driveman.service.ICoachScheduleService;
import com.homework.driveman.service.IRetakeTrainingService;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.homework.driveman.dto.TimeSlotDTO;
import com.homework.driveman.dto.CoachProfileUpdateDTO;
import com.homework.driveman.dto.ChangePasswordDTO;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教练工作台控制器 — 教练端的全部业务操作入口
 * 所有接口均要求登录用户角色为教练（role=2）
 * 通过 JWT 中的 userId 自动识别对应的教练身份
 */
@Tag(name = "教练工作台")
@RestController
@RequestMapping("/coach-portal")
public class CoachPortalController {

    @Autowired
    private ICoachPortalService coachPortalService;

    @Autowired
    private CoachMapper coachMapper;

    @Autowired
    private StudentCoachMapper studentCoachMapper;

    @Autowired
    private TrainingRecordMapper trainingRecordMapper;

    @Autowired
    private IAppointmentService appointmentService;

    @Autowired
    private IUserService userService;

    @Autowired
    private ICoachVehicleApplicationService coachVehicleApplicationService;

    @Autowired
    private CoachApplicationMapper coachApplicationMapper;

    @Autowired
    private IRetakeTrainingService retakeTrainingService;

    @Autowired
    private ICoachScheduleService coachScheduleService;

    /**
     * 从当前登录用户中提取 coachId（coach 表主键）
     * 通过 JWT 中的 userId 反向查找 coach 记录
     */
    private Integer resolveCoachId(HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        if (currentUser == null || currentUser.getRole() != 2) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "仅教练可执行此操作");
        }
        Coach coach = coachMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Coach>()
                        .eq(Coach::getUserId, currentUser.getUserId()));
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练信息不存在，请联系管理员");
        }
        return coach.getCoachId();
    }

    // ==================== 1. 查看名下学员列表 ====================

    @RequireRole(2)
    @Operation(summary = "查看名下学员列表", description = "返回当前教练所有正常绑定的学员基本信息")
    @GetMapping("/students")
    public JsonResult<List<Map<String, Object>>> listStudents(HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);

        // 查询正常绑定的学员-教练关系
        List<StudentCoach> bindings = studentCoachMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getCoachId, coachId)
                        .eq(StudentCoach::getStatus, 1));

        // 逐条关联 user 表获取学员详细信息
        List<Map<String, Object>> result = bindings.stream().map(sc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("bindId", sc.getId());
            map.put("studentId", sc.getStudentId());
            map.put("bindTime", sc.getBindTime());

            User student = userService.getById(sc.getStudentId());
            if (student != null) {
                map.put("realName", student.getRealName());
                map.put("phone", student.getPhone());
                map.put("licenseType", student.getLicenseType());
                map.put("status", student.getStatus());
                map.put("idCard", student.getIdCard());
            }
            return map;
        }).collect(Collectors.toList());

        return JsonResult.ok(result);
    }

    // ==================== 2. 查看名下学员考试报名 ====================

    @RequireRole(2)
    @Operation(summary = "查看名下学员考试报名",
            description = "返回当前教练名下所有学员的考试报名记录，包含学员姓名、场次信息、审核状态（不可审核）")
    @GetMapping("/exam-registrations")
    public JsonResult<List<Map<String, Object>>> getStudentExamRegistrations(HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        return JsonResult.ok(coachPortalService.getStudentExamRegistrations(coachId));
    }

    // ==================== 3. 录入学时 ====================

    @RequireRole(2)
    @Operation(summary = "录入学时", description = "教练为名下学员录入学时记录（需指定学员ID、科目类型、学时数等）")
    @PostMapping("/training-records")
    public JsonResult<Void> createTrainingRecord(HttpServletRequest request,
                                                 @RequestBody TrainingRecord record) {
        Integer coachId = resolveCoachId(request);
        record.setCoachId(coachId);

        // 校验该学员是否是该教练名下的绑定学员
        Long bindingCount = studentCoachMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getStudentId, record.getStudentId())
                        .eq(StudentCoach::getCoachId, coachId)
                        .eq(StudentCoach::getStatus, 1));
        if (bindingCount == 0) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "该学员不是您名下的学员");
        }

        trainingRecordMapper.insert(record);
        return JsonResult.ok();
    }

    // ==================== 4. 约课确认/拒绝 ====================

    @RequireRole(2)
    @Operation(summary = "确认约课", description = "教练确认学员的约课请求，将状态从「待确认」更新为「已确认」")
    @PutMapping("/appointments/{id}/confirm")
    public JsonResult<Void> confirmAppointment(HttpServletRequest request,
                                               @PathVariable Integer id) {
        Integer coachId = resolveCoachId(request);
        Appointment appointment = appointmentService.getById(id);
        if (appointment == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "约课记录不存在");
        }
        // 校验该约课是否属于当前教练
        if (!appointment.getCoachId().equals(coachId)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "该约课不属于您");
        }
        if (appointment.getStatus() != 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该约课已处理，无法重复操作");
        }
        appointment.setStatus(1); // 已确认
        appointmentService.updateById(appointment);
        return JsonResult.ok();
    }

    @RequireRole(2)
    @Operation(summary = "拒绝约课", description = "教练拒绝学员的约课请求，将状态更新为「已取消」，需填写拒绝原因")
    @PutMapping("/appointments/{id}/reject")
    public JsonResult<Void> rejectAppointment(HttpServletRequest request,
                                              @PathVariable Integer id,
                                              @RequestParam String reason) {
        Integer coachId = resolveCoachId(request);
        Appointment appointment = appointmentService.getById(id);
        if (appointment == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "约课记录不存在");
        }
        if (!appointment.getCoachId().equals(coachId)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "该约课不属于您");
        }
        if (appointment.getStatus() != 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该约课已处理，无法重复操作");
        }
        appointment.setStatus(3); // 已取消
        appointment.setCancelReason(reason);
        appointmentService.updateById(appointment);
        return JsonResult.ok();
    }

    // ==================== 5. 设置空闲时间（更新 JSON 字段） ====================

    @RequireRole(2)
    @Operation(summary = "设置空闲时间", description = "教练设置/更新自己的空闲时间，以 JSON 格式存入 available_time 字段")
    @PutMapping("/available-time")
    public JsonResult<Void> setAvailableTime(HttpServletRequest request,
                                             @RequestBody Map<String, Object> availableTime) {
        Integer coachId = resolveCoachId(request);
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练信息不存在");
        }
        try {
            coach.setAvailableTime(new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(availableTime));
        } catch (Exception e) {
            throw new ServiceException(ServiceCode.ERROR_INSERT, "JSON 序列化失败");
        }
        coachMapper.updateById(coach);
        return JsonResult.ok();
    }

    @RequireRole(2)
    @Operation(summary = "查看空闲时间", description = "获取当前教练的空闲时间 JSON 数据")
    @GetMapping("/available-time")
    public JsonResult<Map<String, Object>> getAvailableTime(HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练信息不存在");
        }
        String json = coach.getAvailableTime();
        Map<String, Object> result = new HashMap<>();
        if (json != null && !json.isEmpty()) {
            try {
                result = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, HashMap.class);
            } catch (Exception e) {
                result.put("raw", json);
            }
        }
        return JsonResult.ok(result);
    }

    // ==================== 6. 查看个人工作量统计 ====================

    @RequireRole(2)
    @Operation(summary = "查看个人工作量统计", description = "返回教练的工作量统计：学员数、总学时、各科目学时、通过率")
    @GetMapping("/statistics")
    public JsonResult<Map<String, Object>> getStatistics(HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        Map<String, Object> stats = coachPortalService.getStatistics(coachId);
        return JsonResult.ok(stats);
    }

    // ==================== 7. 查看个人评分 ====================

    @RequireRole(2)
    @Operation(summary = "查看个人评分", description = "返回当前教练的综合评分、执教年限和准教车型信息")
    @GetMapping("/rating")
    public JsonResult<Map<String, Object>> getRating(HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        Map<String, Object> rating = coachPortalService.getRating(coachId);
        return JsonResult.ok(rating);
    }

    // ==================== 8. 提交/查看准教车型变更申请 ====================

    @RequireRole(2)
    @Operation(summary = "提交准教车型变更申请",
            description = "教练申请增加可教车型，提交后由管理员审核。不能与当前车型相同，且不能有未处理的申请。")
    @PostMapping("/vehicle-applications")
    public JsonResult<Void> submitVehicleApplication(HttpServletRequest request,
                                                      @RequestParam String requestedVehicleType,
                                                      @RequestParam(required = false) String applyReason) {
        Integer coachId = resolveCoachId(request);
        coachVehicleApplicationService.submitApplication(coachId, requestedVehicleType, applyReason);
        return JsonResult.ok();
    }

    @RequireRole(2)
    @Operation(summary = "查看本人的准教车型变更申请记录",
            description = "返回当前教练所有提交过申请的历史记录")
    @GetMapping("/vehicle-applications")
    public JsonResult<List<Map<String, Object>>> listMyVehicleApplications(HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        List<com.homework.driveman.entity.CoachVehicleApplication> list =
                coachVehicleApplicationService.lambdaQuery()
                        .eq(com.homework.driveman.entity.CoachVehicleApplication::getCoachId, coachId)
                        .orderByDesc(com.homework.driveman.entity.CoachVehicleApplication::getApplyTime)
                        .list();
        List<Map<String, Object>> result = list.stream().map(app -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", app.getId());
            map.put("currentVehicleType", app.getCurrentVehicleType());
            map.put("requestedVehicleType", app.getRequestedVehicleType());
            map.put("applyReason", app.getApplyReason());
            map.put("status", app.getStatus());
            map.put("auditReason", app.getAuditReason());
            map.put("applyTime", app.getApplyTime());
            map.put("auditTime", app.getAuditTime());
            return map;
        }).collect(Collectors.toList());
        return JsonResult.ok(result);
    }

    // ==================== 9. 查看待确认的约课列表 ====================

    @RequireRole(2)
    @Operation(summary = "查看待确认约课", description = "列出当前教练所有状态为「待确认」的约课请求")
    @GetMapping("/appointments/pending")
    public JsonResult<List<Appointment>> listPendingAppointments(HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        List<Appointment> list = appointmentService.lambdaQuery()
                .eq(Appointment::getCoachId, coachId)
                .eq(Appointment::getStatus, 0)
                .orderByDesc(Appointment::getCreateTime)
                .list();
        return JsonResult.ok(list);
    }

    // ==================== 9. 查看教练的约课日历（按日期范围筛选） ====================

    @RequireRole(2)
    @Operation(summary = "查看约课日历", description = "按日期范围查看该教练的所有已确认约课，用于规划时间")
    @GetMapping("/appointments/calendar")
    public JsonResult<List<Appointment>> listAppointmentsByDateRange(
            HttpServletRequest request,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Integer coachId = resolveCoachId(request);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getCoachId, coachId)
                .in(Appointment::getStatus, 1, 2) // 已确认或已完成
                .orderByAsc(Appointment::getStartTime);

        if (startDate != null) {
            wrapper.ge(Appointment::getStartTime, java.time.LocalDateTime.parse(startDate + "T00:00:00"));
        }
        if (endDate != null) {
            wrapper.le(Appointment::getStartTime, java.time.LocalDateTime.parse(endDate + "T23:59:59"));
        }

        List<Appointment> list = appointmentService.list(wrapper);
        return JsonResult.ok(list);
    }

    // ==================== 10. 教练申请移交学员（含审核流程） ====================

    @RequireRole(2)
    @Operation(summary = "教练申请移交学员",
            description = "教练因故无法继续带教学员，发起将某学员移交给另一位指定教练的申请，由管理员审核")
    @PostMapping("/student-transfers")
    public JsonResult<Void> submitStudentTransfer(
            HttpServletRequest request,
            @RequestParam Integer studentId,
            @RequestParam Integer targetCoachId,
            @RequestParam String reason) {
        Integer sourceCoachId = resolveCoachId(request);

        // 校验该学员是否是该教练名下的绑定学员
        Long bindingCount = studentCoachMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getStudentId, studentId)
                        .eq(StudentCoach::getCoachId, sourceCoachId)
                        .eq(StudentCoach::getStatus, 1));
        if (bindingCount == 0) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "该学员不是您名下的学员");
        }

        // 校验是否已有待审核的移交申请
        Long pendingCount = coachApplicationMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CoachApplication>()
                        .eq(CoachApplication::getStudentId, studentId)
                        .eq(CoachApplication::getSourceCoachId, sourceCoachId)
                        .eq(CoachApplication::getStatus, 0));
        if (pendingCount > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该学员的移交申请已提交，请等待管理员处理");
        }

        CoachApplication application = new CoachApplication();
        application.setStudentId(studentId);
        application.setCoachId(targetCoachId);
        application.setSourceCoachId(sourceCoachId);
        application.setTransferReason(reason);
        application.setStatus(0);
        application.setApplyTime(LocalDateTime.now());
        coachApplicationMapper.insert(application);
        return JsonResult.ok();
    }

    @RequireRole(2)
    @Operation(summary = "查看本人的学员移交申请记录",
            description = "返回当前教练发起的所有学员移交申请历史")
    @GetMapping("/student-transfers")
    public JsonResult<List<Map<String, Object>>> listStudentTransfers(HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        List<CoachApplication> list = coachApplicationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CoachApplication>()
                        .eq(CoachApplication::getSourceCoachId, coachId)
                        .orderByDesc(CoachApplication::getApplyTime));

        List<Map<String, Object>> result = list.stream().map(app -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", app.getId());
            map.put("studentId", app.getStudentId());
            map.put("targetCoachId", app.getCoachId());
            map.put("transferReason", app.getTransferReason());
            map.put("status", app.getStatus());
            map.put("auditReason", app.getAuditReason());
            map.put("applyTime", app.getApplyTime());
            map.put("auditTime", app.getAuditTime());

            User student = userService.getById(app.getStudentId());
            map.put("studentName", student != null ? student.getRealName() : "未知");

            return map;
        }).collect(Collectors.toList());
        return JsonResult.ok(result);
    }

    // ==================== 11. 查看名下学员二次培训记录（只读） ====================

    @RequireRole(2)
    @Operation(summary = "查看名下学员二次培训记录",
            description = "教练查看名下学员的二次培训（补考培训）申请记录，仅可查看不可审核。全包学员免缴费，非全包学员需缴费。")
    @GetMapping("/retake-trainings")
    public JsonResult<List<Map<String, Object>>> getRetakeTrainings(HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        return JsonResult.ok(retakeTrainingService.listByCoach(coachId));
    }

    // ==================== 12. 排班管理（教练端） ====================

    @RequireRole(2)
    @Operation(summary = "提交排班申请", description = "教练提交排班申请（含冲突检测），提交后由管理员审核。")
    @PostMapping("/schedules")
    public JsonResult<Void> applySchedule(HttpServletRequest request,
                                          @RequestBody CoachSchedule schedule) {
        Integer coachId = resolveCoachId(request);
        schedule.setCoachId(coachId);
        coachScheduleService.apply(schedule);
        return JsonResult.ok();
    }

    @RequireRole(2)
    @Operation(summary = "查看本人的排班记录（分页+多条件）",
            description = "返回当前教练的排班申请记录，含车牌号/场地名称，支持分页及多条件筛选")
    @GetMapping("/schedules")
    public JsonResult<Page<Map<String, Object>>> listMySchedules(HttpServletRequest request,
                                                                  @RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "10") int size,
                                                                  @RequestParam(required = false) @Parameter(description = "培训车型") String licenseType,
                                                                  @RequestParam(required = false) @Parameter(description = "排班状态") Integer status,
                                                                  @RequestParam(required = false) @Parameter(description = "开始时间起 yyyy-MM-dd") String startDateStart,
                                                                  @RequestParam(required = false) @Parameter(description = "开始时间止 yyyy-MM-dd") String startDateEnd) {
        Integer coachId = resolveCoachId(request);
        LocalDateTime startStart = startDateStart != null ? LocalDateTime.parse(startDateStart + "T00:00:00") : null;
        LocalDateTime startEnd = startDateEnd != null ? LocalDateTime.parse(startDateEnd + "T23:59:59") : null;
        return JsonResult.ok(coachScheduleService.pageSearch(new Page<>(page, size),
                coachId, null, null, null, licenseType, status, startStart, startEnd));
    }

    @RequireRole(2)
    @Operation(summary = "取消排班", description = "教练取消自己的排班申请（待审核或已通过状态可取消）")
    @PutMapping("/schedules/{id}/cancel")
    public JsonResult<Void> cancelSchedule(@PathVariable Integer id, HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        coachScheduleService.cancel(id, coachId);
        return JsonResult.ok();
    }

    // ==================== 13. 可预约时间段结构化管理 ====================

    @Operation(summary = "获取可预约时间段列表")
    @GetMapping("/time-slots")
    public JsonResult<List<TimeSlotDTO>> getTimeSlots(HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        return JsonResult.ok(coachPortalService.getTimeSlots(coachId));
    }

    @Operation(summary = "批量设置可预约时间段（全量替换）")
    @PutMapping("/time-slots")
    public JsonResult<Void> setTimeSlots(@RequestBody @Valid UpdateTimeSlotsDTO dto,
                                         HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        coachPortalService.setTimeSlots(coachId, dto.getTimeSlots());
        return JsonResult.ok();
    }

    @Operation(summary = "添加一个可预约时间段")
    @PostMapping("/time-slots")
    public JsonResult<Void> addTimeSlot(@RequestBody @Valid TimeSlotDTO slot,
                                        HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        coachPortalService.addTimeSlot(coachId, slot);
        return JsonResult.ok();
    }

    @Operation(summary = "删除一个可预约时间段")
    @DeleteMapping("/time-slots")
    public JsonResult<Void> removeTimeSlot(@RequestBody @Valid TimeSlotDTO slot,
                                           HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        coachPortalService.removeTimeSlot(coachId, slot);
        return JsonResult.ok();
    }

    // ==================== 13. 个人信息管理 ====================

    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public JsonResult<Map<String, Object>> getProfile(HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        return JsonResult.ok(coachPortalService.getProfile(coachId));
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public JsonResult<Void> updateProfile(@RequestBody @Valid CoachProfileUpdateDTO dto,
                                          HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        coachPortalService.updateProfile(coachId, dto);
        return JsonResult.ok();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public JsonResult<Void> changePassword(@RequestBody @Valid ChangePasswordDTO dto,
                                           HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        coachPortalService.changePassword(coachId, dto);
        return JsonResult.ok();
    }
}