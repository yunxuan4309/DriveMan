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
import com.homework.driveman.entity.Vehicle;
import com.homework.driveman.entity.Venue;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.CoachApplicationMapper;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.StudentCoachMapper;
import com.homework.driveman.mapper.TrainingRecordMapper;
import com.homework.driveman.mapper.VehicleMapper;
import com.homework.driveman.mapper.VenueMapper;
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

    @Autowired
    private VehicleMapper vehicleMapper;

    @Autowired
    private VenueMapper venueMapper;

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

    // ==================== 4. 约课确认/拒绝/完成 + 分页列表 ====================

    @RequireRole(2)
    @Operation(summary = "确认约课", description = "教练确认学员的约课请求，将状态从「待确认」更新为「已确认」")
    @PutMapping("/appointments/{id}/confirm")
    public JsonResult<Void> confirmAppointment(HttpServletRequest request,
                                               @PathVariable Integer id) {
        Integer coachId = resolveCoachId(request);
        appointmentService.confirmAppointment(id, coachId);
        return JsonResult.ok();
    }

    @RequireRole(2)
    @Operation(summary = "拒绝约课", description = "教练拒绝学员的约课请求，将状态更新为「已拒绝」，需填写拒绝原因")
    @PutMapping("/appointments/{id}/reject")
    public JsonResult<Void> rejectAppointment(HttpServletRequest request,
                                              @PathVariable Integer id,
                                              @RequestParam String reason) {
        Integer coachId = resolveCoachId(request);
        appointmentService.rejectAppointment(id, coachId, reason);
        return JsonResult.ok();
    }

    @RequireRole(2)
    @Operation(summary = "完成课程", description = "教练将已确认的约课标记为「已完成」")
    @PutMapping("/appointments/{id}/complete")
    public JsonResult<Void> completeAppointment(HttpServletRequest request,
                                                @PathVariable Integer id) {
        Integer coachId = resolveCoachId(request);
        appointmentService.completeAppointment(id, coachId);
        return JsonResult.ok();
    }

    @RequireRole(2)
    @Operation(summary = "分页查询约课列表", description = "返回当前教练的全部约课记录（含学员姓名），支持按状态筛选")
    @GetMapping("/appointments")
    public JsonResult<Page<Map<String, Object>>> listMyAppointments(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        Integer coachId = resolveCoachId(request);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getCoachId, coachId)
                .eq(status != null, Appointment::getStatus, status)
                .orderByDesc(Appointment::getCreateTime);
        Page<Appointment> rawPage = appointmentService.page(new Page<>(page, size), wrapper);
        // 补充学员姓名
        List<Map<String, Object>> enriched = rawPage.getRecords().stream().map(a -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("studentId", a.getStudentId());
            map.put("coachId", a.getCoachId());
            map.put("scheduleId", a.getScheduleId());
            map.put("startTime", a.getStartTime());
            map.put("endTime", a.getEndTime());
            map.put("status", a.getStatus());
            map.put("cancelReason", a.getCancelReason());
            map.put("createTime", a.getCreateTime());
            User student = userService.getById(a.getStudentId());
            map.put("studentName", student != null ? student.getRealName() : null);
            return map;
        }).collect(Collectors.toList());
        Page<Map<String, Object>> result = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        result.setRecords(enriched);
        return JsonResult.ok(result);
    }

    // ==================== 5. 设置常规空闲时段（参考） ====================

    @RequireRole(2)
    @Operation(summary = "设置常规空闲时段", description = "教练设置/更新自己的常规空闲时段（仅供学员参考，实际约课以排班为准），以 JSON 格式存入 available_time 字段")
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
    @Operation(summary = "查看常规空闲时段", description = "获取当前教练的常规空闲时段 JSON 数据（仅供学员参考）")
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
    @Operation(summary = "查看待确认约课", description = "列出当前教练所有状态为「待确认」的约课请求，含学员姓名")
    @GetMapping("/appointments/pending")
    public JsonResult<List<Map<String, Object>>> listPendingAppointments(HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        List<Appointment> list = appointmentService.lambdaQuery()
                .eq(Appointment::getCoachId, coachId)
                .eq(Appointment::getStatus, 0)
                .orderByDesc(Appointment::getCreateTime)
                .list();
        List<Map<String, Object>> result = list.stream().map(a -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", a.getId());
            map.put("studentId", a.getStudentId());
            map.put("coachId", a.getCoachId());
            map.put("scheduleId", a.getScheduleId());
            map.put("startTime", a.getStartTime());
            map.put("endTime", a.getEndTime());
            map.put("status", a.getStatus());
            map.put("cancelReason", a.getCancelReason());
            map.put("createTime", a.getCreateTime());
            User student = userService.getById(a.getStudentId());
            map.put("studentName", student != null ? student.getRealName() : null);
            return map;
        }).collect(Collectors.toList());
        return JsonResult.ok(result);
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
    @Operation(summary = "教练申请移交学员（支持批量）",
            description = "教练因故无法继续带教学员，发起将一个或多个学员移交给另一位指定教练的申请，由管理员审核。studentIds 逗号分隔。")
    @PostMapping("/student-transfers")
    public JsonResult<Void> submitStudentTransfer(
            HttpServletRequest request,
            @RequestParam String studentIds,
            @RequestParam Integer targetCoachId,
            @RequestParam String reason) {
        Integer sourceCoachId = resolveCoachId(request);

        String[] ids = studentIds.split(",");
        for (String idStr : ids) {
            Integer studentId = Integer.parseInt(idStr.trim());

            Long bindingCount = studentCoachMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                            .eq(StudentCoach::getStudentId, studentId)
                            .eq(StudentCoach::getCoachId, sourceCoachId)
                            .eq(StudentCoach::getStatus, 1));
            if (bindingCount == 0) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                        "学员 ID=" + studentId + " 不是您名下的学员");
            }

            Long pendingCount = coachApplicationMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CoachApplication>()
                            .eq(CoachApplication::getStudentId, studentId)
                            .eq(CoachApplication::getSourceCoachId, sourceCoachId)
                            .eq(CoachApplication::getStatus, 0));
            if (pendingCount > 0) {
                throw new ServiceException(ServiceCode.ERROR_CONFLICT,
                        "学员 ID=" + studentId + " 的移交申请已提交，请等待管理员处理");
            }

            CoachApplication application = new CoachApplication();
            application.setStudentId(studentId);
            application.setCoachId(targetCoachId);
            application.setSourceCoachId(sourceCoachId);
            application.setTransferReason(reason);
            application.setStatus(0);
            application.setApplyTime(LocalDateTime.now());
            coachApplicationMapper.insert(application);
        }
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

            // 查询目标教练姓名
            Coach targetCoach = coachMapper.selectById(app.getCoachId());
            if (targetCoach != null) {
                User targetUser = userService.getById(targetCoach.getUserId());
                map.put("targetCoachName", targetUser != null ? targetUser.getRealName() : "未知");
            } else {
                map.put("targetCoachName", "未知");
            }

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

    @RequireRole(2)
    @Operation(summary = "查看已通过排班及预约概览",
            description = "返回当前教练所有已审核通过的排班，及每个排班的学员预约情况（含学员姓名、预约状态、剩余名额）")
    @GetMapping("/schedules/approved")
    public JsonResult<List<Map<String, Object>>> listApprovedSchedules(HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);

        List<CoachSchedule> schedules = coachScheduleService.lambdaQuery()
                .eq(CoachSchedule::getCoachId, coachId)
                .eq(CoachSchedule::getStatus, 1)
                .orderByDesc(CoachSchedule::getStartTime)
                .list();

        List<Map<String, Object>> result = schedules.stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("scheduleId", s.getId());
            map.put("licenseType", s.getLicenseType());
            map.put("startTime", s.getStartTime());
            map.put("endTime", s.getEndTime());
            map.put("maxStudents", s.getMaxStudents());
            map.put("bookedCount", s.getBookedCount());

            Vehicle vehicle = vehicleMapper.selectById(s.getVehicleId());
            map.put("plateNumber", vehicle != null ? vehicle.getPlateNumber() : null);

            Venue venue = venueMapper.selectById(s.getVenueId());
            map.put("venueName", venue != null ? venue.getName() : null);

            List<Appointment> appointments = appointmentService.lambdaQuery()
                    .eq(Appointment::getScheduleId, s.getId())
                    .list();
            List<Map<String, Object>> bookingList = appointments.stream().map(a -> {
                Map<String, Object> bm = new LinkedHashMap<>();
                bm.put("appointmentId", a.getId());
                bm.put("status", a.getStatus());
                User student = userService.getById(a.getStudentId());
                bm.put("studentName", student != null ? student.getRealName() : null);
                return bm;
            }).collect(Collectors.toList());
            map.put("appointments", bookingList);
            return map;
        }).collect(Collectors.toList());

        return JsonResult.ok(result);
    }

    // ==================== 13. 常规空闲时段结构化管理（仅供学员参考） ====================

    @Operation(summary = "获取常规空闲时段列表（参考）")
    @GetMapping("/time-slots")
    public JsonResult<List<TimeSlotDTO>> getTimeSlots(HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        return JsonResult.ok(coachPortalService.getTimeSlots(coachId));
    }

    @Operation(summary = "批量设置常规空闲时段（全量替换）")
    @PutMapping("/time-slots")
    public JsonResult<Void> setTimeSlots(@RequestBody @Valid UpdateTimeSlotsDTO dto,
                                         HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        coachPortalService.setTimeSlots(coachId, dto.getTimeSlots());
        return JsonResult.ok();
    }

    @Operation(summary = "添加一个常规空闲时段")
    @PostMapping("/time-slots")
    public JsonResult<Void> addTimeSlot(@RequestBody @Valid TimeSlotDTO slot,
                                        HttpServletRequest request) {
        Integer coachId = resolveCoachId(request);
        coachPortalService.addTimeSlot(coachId, slot);
        return JsonResult.ok();
    }

    @Operation(summary = "删除一个常规空闲时段")
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