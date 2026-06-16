package com.homework.driveman.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.constant.AppointmentStatus;
import com.homework.driveman.entity.Appointment;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.CoachSchedule;
import com.homework.driveman.entity.StudentCoach;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.AppointmentMapper;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.StudentCoachMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.IAppointmentService;
import com.homework.driveman.service.ICoachScheduleService;
import com.homework.driveman.service.IPhysicalExamService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 约课管理控制器 — 学员预约/取消课程 + 管理员查询管理
 */
@Tag(name = "约课管理")
@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    @Autowired
    private IAppointmentService appointmentService;

    @Autowired
    private ICoachScheduleService scheduleService;

    @Autowired
    private StudentCoachMapper studentCoachMapper;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CoachMapper coachMapper;

    @Autowired
    private IPhysicalExamService physicalExamService;

    @Autowired
    private com.homework.driveman.mapper.LicenseUpgradeMapper licenseUpgradeMapper;

    @Autowired
    private com.homework.driveman.mapper.ExamRegistrationMapper examRegistrationMapper;

    private CurrentUser getCurrentUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute("currentUser");
    }

    // ==================== 学员端 ====================

    @RequireRole(1)
    @Operation(summary = "查询我的约课（学员端）", description = "学员只能查看自己的约课记录，包含学员姓名和教练姓名")
    @GetMapping("/my")
    public JsonResult<Page<Map<String, Object>>> listMy(HttpServletRequest request,
                                                         @RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) Integer status) {
        CurrentUser user = getCurrentUser(request);
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getStudentId, user.getUserId())
                .eq(status != null, Appointment::getStatus, status)
                .orderByDesc(Appointment::getCreateTime);

        Page<Appointment> rawPage = appointmentService.page(new Page<>(page, size), wrapper);
        Page<Map<String, Object>> enrichedPage = enrichAppointmentPage(rawPage);
        return JsonResult.ok(enrichedPage);
    }

    @RequireRole(1)
    @Operation(summary = "学员新增约课",
            description = "必须在教练已批准的排班时段内约课，关联排班 scheduleId 为必填。studentId 从 JWT 自动获取。")
    @PostMapping
    public JsonResult<Map<String, Object>> create(HttpServletRequest request,
                                                   @RequestBody Appointment appointment) {
        CurrentUser user = getCurrentUser(request);

        // 强制使用 JWT 中的 userId 作为 studentId，防止伪造
        appointment.setStudentId(user.getUserId());
        appointment.setStatus(AppointmentStatus.PENDING);

        // 基本校验
        if (appointment.getStartTime() == null || appointment.getEndTime() == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "开始时间和结束时间不能为空");
        }
        if (!appointment.getStartTime().isBefore(appointment.getEndTime())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "开始时间必须早于结束时间");
        }
        if (appointment.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "约课时间必须在未来");
        }

        // 检查体检是否不合格
        physicalExamService.checkPassed(user.getUserId());

        // 强制要求关联排班
        Integer scheduleId = appointment.getScheduleId();
        if (scheduleId == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "请选择教练的排班时段进行约课");
        }

        CoachSchedule schedule = scheduleService.getById(scheduleId);
        if (schedule == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "排班记录不存在");
        }
        if (schedule.getStatus() != 1) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "该排班时段不可约");
        }

        // 校验学员车型与排班车型是否匹配
        User student = userMapper.selectById(user.getUserId());
        if (student != null && student.getLicenseType() != null
                && schedule.getLicenseType() != null
                && !student.getLicenseType().equals(schedule.getLicenseType())) {
            // 例外：学员有该目标车型进行中的增驾申请时允许跨车型约课
            Long upgradeCount = licenseUpgradeMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.homework.driveman.entity.LicenseUpgrade>()
                            .eq(com.homework.driveman.entity.LicenseUpgrade::getStudentId, user.getUserId())
                            .eq(com.homework.driveman.entity.LicenseUpgrade::getTargetLicense, schedule.getLicenseType())
                            .eq(com.homework.driveman.entity.LicenseUpgrade::getStatus, 1)
                            .ne(com.homework.driveman.entity.LicenseUpgrade::getExamStatus, 1));
            if (upgradeCount == 0) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                        "您的车型为 " + student.getLicenseType() + "，该排班仅限 " + schedule.getLicenseType() + " 车型");
            }
        }

        // 校验学员-教练绑定关系
        StudentCoach binding = studentCoachMapper.selectOne(
                new LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getStudentId, user.getUserId())
                        .eq(StudentCoach::getStatus, 1));
        if (binding == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "您还没有绑定教练，请先申请分配教练");
        }
        if (!schedule.getCoachId().equals(binding.getCoachId())) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "只能预约您教练的排班时段");
        }

        // 约课时间必须在排班时间范围内
        if (appointment.getStartTime().isBefore(schedule.getStartTime())
                || appointment.getEndTime().isAfter(schedule.getEndTime())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "约课时间超出教练排班时段范围");
        }

        // 检查科目前置条件
        if (schedule.getSubject() != null && schedule.getSubject() >= 2) {
            Set<Integer> passedSubjects = examRegistrationMapper.findPassedSubjectsByStudent(
                    user.getUserId(), schedule.getLicenseType());
            if (schedule.getSubject() == 2 && !passedSubjects.contains(1)) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                        "科目一未通过，暂不能预约科目二课程");
            }
            if (schedule.getSubject() == 3 && (!passedSubjects.contains(1) || !passedSubjects.contains(2))) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                        "前置科目未通过，暂不能预约科目三课程");
            }
        }

        // 名额校验
        if (schedule.getBookedCount() >= schedule.getMaxStudents()) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该时段已约满");
        }

        // 扣减名额
        schedule.setBookedCount(schedule.getBookedCount() + 1);
        scheduleService.updateById(schedule);

        // 检查学员自身时间冲突
        long conflictCount = appointmentService.lambdaQuery()
                .eq(Appointment::getStudentId, user.getUserId())
                .in(Appointment::getStatus, AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)
                .lt(Appointment::getStartTime, appointment.getEndTime())
                .gt(Appointment::getEndTime, appointment.getStartTime())
                .count();
        if (conflictCount > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "您在该时段已有其他约课");
        }

        appointmentService.save(appointment);
        return JsonResult.ok(appointmentMapper.selectByIdWithDetails(appointment.getId()));
    }

    @RequireRole(1)
    @Operation(summary = "学员取消约课", description = "学员只能取消自己的约课，且只能取消待确认或已确认状态的")
    @PutMapping("/{id}/cancel")
    public JsonResult<Map<String, Object>> cancel(HttpServletRequest request,
                                                   @PathVariable Integer id,
                                                   @RequestParam(required = false) String reason) {
        CurrentUser user = getCurrentUser(request);
        Appointment appointment = appointmentService.getById(id);
        if (appointment == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "约课记录不存在");
        }
        if (!appointment.getStudentId().equals(user.getUserId())) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "无权取消他人的约课");
        }
        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "当前状态不允许取消");
        }

        if (appointment.getScheduleId() != null) {
            CoachSchedule schedule = scheduleService.getById(appointment.getScheduleId());
            if (schedule != null && schedule.getBookedCount() > 0) {
                schedule.setBookedCount(schedule.getBookedCount() - 1);
                scheduleService.updateById(schedule);
            }
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(reason);
        appointmentService.updateById(appointment);
        return JsonResult.ok(appointmentMapper.selectByIdWithDetails(id));
    }

    // ==================== 通用查询 ====================

    @Operation(summary = "根据ID查询约课详情", description = "返回约课记录，含学员姓名和教练姓名")
    @GetMapping("/{id}")
    public JsonResult<Map<String, Object>> getById(HttpServletRequest request, @PathVariable Integer id) {
        Map<String, Object> detail = appointmentMapper.selectByIdWithDetails(id);
        if (detail == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "约课记录不存在");
        }
        // 学员只能查看自己的
        CurrentUser user = getCurrentUser(request);
        if (user.getRole() == 1) {
            Object sid = detail.get("student_id");
            if (sid != null && ((Number) sid).intValue() != user.getUserId()) {
                throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "无权查看他人的约课");
            }
        }
        return JsonResult.ok(detail);
    }

    // ==================== 管理员端 ====================

    @RequireRole(3)
    @Operation(summary = "分页查询所有约课（管理员端）", description = "可按学员ID、教练ID、状态筛选，含学员姓名和教练姓名")
    @GetMapping
    public JsonResult<Page<Map<String, Object>>> listAll(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size,
                                                          @RequestParam(required = false) Integer studentId,
                                                          @RequestParam(required = false) Integer coachId,
                                                          @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<Appointment>()
                .eq(studentId != null, Appointment::getStudentId, studentId)
                .eq(coachId != null, Appointment::getCoachId, coachId)
                .eq(status != null, Appointment::getStatus, status)
                .orderByDesc(Appointment::getCreateTime);

        Page<Appointment> rawPage = appointmentService.page(new Page<>(page, size), wrapper);
        Page<Map<String, Object>> enrichedPage = enrichAppointmentPage(rawPage);
        return JsonResult.ok(enrichedPage);
    }

    @RequireRole(3)
    @Operation(summary = "管理员取消约课", description = "管理员可取消任何约课")
    @PutMapping("/{id}/admin-cancel")
    public JsonResult<Map<String, Object>> adminCancel(@PathVariable Integer id,
                                                        @RequestParam(required = false) String reason) {
        Appointment appointment = appointmentService.getById(id);
        if (appointment == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "约课记录不存在");
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED
                || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "当前状态不允许取消");
        }
        if (appointment.getScheduleId() != null) {
            CoachSchedule schedule = scheduleService.getById(appointment.getScheduleId());
            if (schedule != null && schedule.getBookedCount() > 0) {
                schedule.setBookedCount(schedule.getBookedCount() - 1);
                scheduleService.updateById(schedule);
            }
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelReason(reason);
        appointmentService.updateById(appointment);
        return JsonResult.ok(appointmentMapper.selectByIdWithDetails(id));
    }

    // ==================== 工具方法 ====================

    /**
     * 将分页的 Appointment 实体转换为含姓名的 Map 分页
     */
    private Page<Map<String, Object>> enrichAppointmentPage(Page<Appointment> rawPage) {
        List<Appointment> records = rawPage.getRecords();
        if (records.isEmpty()) {
            Page<Map<String, Object>> empty = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        // 收集所有学员ID和教练ID
        Set<Integer> studentIds = records.stream().map(Appointment::getStudentId).collect(Collectors.toSet());
        Set<Integer> coachIds = records.stream().map(Appointment::getCoachId).filter(Objects::nonNull).collect(Collectors.toSet());

        // 批量查询学员姓名
        Map<Integer, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        // 批量查询教练姓名（coach表 → user表）
        Map<Integer, String> coachNameMap = new HashMap<>();
        if (!coachIds.isEmpty()) {
            List<Coach> coaches = coachMapper.selectBatchIds(coachIds);
            Set<Integer> coachUserIds = coaches.stream().map(Coach::getUserId).collect(Collectors.toSet());
            Map<Integer, User> coachUserMap = userMapper.selectBatchIds(coachUserIds).stream()
                    .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));
            for (Coach c : coaches) {
                User cu = coachUserMap.get(c.getUserId());
                coachNameMap.put(c.getCoachId(), cu != null ? cu.getRealName() : null);
            }
        }

        // 批量查询排班信息（科目、车型）
        List<Integer> scheduleIds = records.stream()
                .map(Appointment::getScheduleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Integer, CoachSchedule> scheduleMap = new HashMap<>();
        if (!scheduleIds.isEmpty()) {
            for (CoachSchedule s : scheduleService.listByIds(scheduleIds)) {
                scheduleMap.put(s.getId(), s);
            }
        }

        // 构建 enriched records
        List<Map<String, Object>> enriched = records.stream().map(a -> {
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
            map.put("updateTime", a.getUpdateTime());

            User student = userMap.get(a.getStudentId());
            map.put("studentName", student != null ? student.getRealName() : null);
            map.put("studentPhone", student != null ? student.getPhone() : null);
            map.put("studentLicenseType", student != null ? student.getLicenseType() : null);

            map.put("coachName", coachNameMap.getOrDefault(a.getCoachId(), null));

            // 从排班信息获取科目
            if (a.getScheduleId() != null) {
                CoachSchedule cs = scheduleMap.get(a.getScheduleId());
                if (cs != null) {
                    map.put("subject", cs.getSubject());
                    map.put("scheduleLicenseType", cs.getLicenseType());
                }
            }
            return map;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> enrichedPage = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        enrichedPage.setRecords(enriched);
        return enrichedPage;
    }
}
