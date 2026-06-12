package com.homework.driveman.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.constant.AppointmentStatus;
import com.homework.driveman.entity.Appointment;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.CoachApplication;
import com.homework.driveman.entity.CoachSchedule;
import com.homework.driveman.entity.StudentCoach;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.AppointmentMapper;
import com.homework.driveman.mapper.CoachApplicationMapper;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.StudentCoachMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.ICoachApplicationService;
import com.homework.driveman.service.ICoachScheduleService;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 教练申请审核控制器
 * 学员申请 → 管理员审核 → 通过后写入 student_coach 绑定关系
 */
@Tag(name = "教练申请审核")
@RestController
@RequestMapping("/coach-applications")
public class  CoachApplicationController {

    @Autowired
    private CoachApplicationMapper coachApplicationMapper;

    @Autowired
    private StudentCoachMapper studentCoachMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CoachMapper coachMapper;

    @Autowired
    private ICoachApplicationService coachApplicationService;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Autowired
    private ICoachScheduleService scheduleService;

    @RequireRole(1)
    @Operation(summary = "学员提交教练选择申请（含更换教练）",
            description = "已有教练的学员提交后视为更换申请，由管理员审核。同一学员不能有多个待审核申请。")
    @PostMapping
    public JsonResult<Void> apply(@RequestParam Integer studentId,
                                  @RequestParam Integer coachId) {
        // 校验是否已有待审核的申请
        Long pendingCount = coachApplicationMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CoachApplication>()
                        .eq(CoachApplication::getStudentId, studentId)
                        .eq(CoachApplication::getStatus, 0));
        if (pendingCount > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "您已有待审核的申请，请等待管理员处理");
        }

        // 校验是否已经绑定该教练
        Long boundCount = studentCoachMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getStudentId, studentId)
                        .eq(StudentCoach::getCoachId, coachId)
                        .eq(StudentCoach::getStatus, 1));
        if (boundCount > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "您已绑定该教练，无需重复申请");
        }

        CoachApplication application = new CoachApplication();
        application.setStudentId(studentId);
        application.setCoachId(coachId);
        application.setStatus(0);
        application.setApplyTime(LocalDateTime.now());
        coachApplicationMapper.insert(application);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "审核教练申请",
            description = "pass=true 审核通过：自动取消学员与旧教练的进行中约课并释放排班名额，然后绑定新教练。pass=false 拒绝。")
    @Transactional
    @PutMapping("/{id}/audit")
    public JsonResult<Map<String, Object>> audit(@PathVariable Integer id,
                                                   @RequestParam boolean pass,
                                                   @RequestParam(required = false) String reason) {
        CoachApplication application = coachApplicationMapper.selectById(id);
        if (application == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "申请记录不存在");
        }

        if (pass) {
            // 校验发起教练是否仍与学员有绑定关系（移交场景）
            if (application.getSourceCoachId() != null) {
                Long bindingCount = studentCoachMapper.selectCount(
                        new LambdaQueryWrapper<StudentCoach>()
                                .eq(StudentCoach::getStudentId, application.getStudentId())
                                .eq(StudentCoach::getCoachId, application.getSourceCoachId())
                                .eq(StudentCoach::getStatus, 1));
                if (bindingCount == 0) {
                    throw new ServiceException(ServiceCode.ERROR_CONFLICT,
                            "该学员已不是发起移交的教练名下的学员，请核实");
                }
            }

            // 找到学员当前的绑定教练
            StudentCoach oldBinding = studentCoachMapper.selectOne(
                    new LambdaQueryWrapper<StudentCoach>()
                            .eq(StudentCoach::getStudentId, application.getStudentId())
                            .eq(StudentCoach::getStatus, 1));
            Integer oldCoachId = oldBinding != null ? oldBinding.getCoachId() : null;

            // 清理该学员与旧教练的进行中约课（pending/confirmed）
            List<Map<String, Object>> cancelledDetails = new ArrayList<>();
            String oldCoachName = "未知";
            if (oldCoachId != null) {
                Coach oldCoach = coachMapper.selectById(oldCoachId);
                if (oldCoach != null) {
                    User oldCoachUser = userMapper.selectById(oldCoach.getUserId());
                    oldCoachName = oldCoachUser != null ? oldCoachUser.getRealName() : "未知";
                }

                List<Appointment> oldAppointments = appointmentMapper.selectList(
                        new LambdaQueryWrapper<Appointment>()
                                .eq(Appointment::getStudentId, application.getStudentId())
                                .eq(Appointment::getCoachId, oldCoachId)
                                .in(Appointment::getStatus, AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED));

                for (Appointment apt : oldAppointments) {
                    // 释放排班名额
                    if (apt.getScheduleId() != null) {
                        CoachSchedule schedule = scheduleService.getById(apt.getScheduleId());
                        if (schedule != null && schedule.getBookedCount() > 0) {
                            schedule.setBookedCount(schedule.getBookedCount() - 1);
                            scheduleService.updateById(schedule);
                        }
                    }
                    // 标记为取消
                    apt.setStatus(AppointmentStatus.CANCELLED);
                    apt.setCancelReason("学员更换教练，系统自动取消");
                    appointmentMapper.updateById(apt);

                    Map<String, Object> detail = new LinkedHashMap<>();
                    detail.put("appointmentId", apt.getId());
                    detail.put("startTime", apt.getStartTime().toString());
                    detail.put("endTime", apt.getEndTime().toString());
                    cancelledDetails.add(detail);
                }
            }

            // 解绑旧教练
            studentCoachMapper.update(null,
                    new LambdaUpdateWrapper<StudentCoach>()
                            .eq(StudentCoach::getStudentId, application.getStudentId())
                            .eq(StudentCoach::getStatus, 1)
                            .set(StudentCoach::getStatus, 0));

            // 创建新绑定
            StudentCoach sc = new StudentCoach();
            sc.setStudentId(application.getStudentId());
            sc.setCoachId(application.getCoachId());
            sc.setBindTime(LocalDateTime.now());
            sc.setStatus(1);
            studentCoachMapper.insert(sc);

            application.setStatus(1);
            application.setAuditTime(LocalDateTime.now());
            coachApplicationMapper.updateById(application);

            // 返回取消详情
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("cancelledCount", cancelledDetails.size());
            result.put("cancelledAppointments", cancelledDetails);
            result.put("oldCoachName", oldCoachName);

            // 新教练姓名
            Coach newCoach = coachMapper.selectById(application.getCoachId());
            String newCoachName = "未知";
            if (newCoach != null) {
                User newCoachUser = userMapper.selectById(newCoach.getUserId());
                newCoachName = newCoachUser != null ? newCoachUser.getRealName() : "未知";
            }
            result.put("newCoachName", newCoachName);

            return JsonResult.ok(result);
        } else {
            application.setStatus(2);
            application.setAuditTime(LocalDateTime.now());
            application.setAuditReason(reason);
            coachApplicationMapper.updateById(application);
        }
        return JsonResult.ok(null);
    }

    @RequireRole(3)
    @Operation(summary = "查询所有待审核的教练申请（含学员/教练详情）")
    @GetMapping("/pending")
    public JsonResult<List<Map<String, Object>>> listPending() {
        List<CoachApplication> list = coachApplicationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CoachApplication>()
                        .eq(CoachApplication::getStatus, 0)
                        .orderByDesc(CoachApplication::getCreateTime));

        List<Map<String, Object>> result = list.stream().map(app -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", app.getId());
            map.put("studentId", app.getStudentId());
            map.put("coachId", app.getCoachId());
            map.put("sourceCoachId", app.getSourceCoachId());
            map.put("transferReason", app.getTransferReason());
            map.put("status", app.getStatus());
            map.put("applyTime", app.getApplyTime());

            // 学员姓名
            User student = userMapper.selectById(app.getStudentId());
            map.put("studentName", student != null ? student.getRealName() : "未知");

            // 申请/目标教练姓名
            Coach targetCoach = coachMapper.selectById(app.getCoachId());
            if (targetCoach != null) {
                User targetUser = userMapper.selectById(targetCoach.getUserId());
                map.put("coachName", targetUser != null ? targetUser.getRealName() : "未知");
            } else {
                map.put("coachName", "未知");
            }

            // 如果是教练移交申请，显示发起教练姓名
            if (app.getSourceCoachId() != null) {
                Coach sourceCoach = coachMapper.selectById(app.getSourceCoachId());
                if (sourceCoach != null) {
                    User sourceUser = userMapper.selectById(sourceCoach.getUserId());
                    map.put("sourceCoachName", sourceUser != null ? sourceUser.getRealName() : "未知");
                } else {
                    map.put("sourceCoachName", "未知");
                }
                map.put("applyType", "教练移交");
            } else {
                map.put("sourceCoachName", null);
                map.put("applyType", "学员申请");
            }

            return map;
        }).collect(Collectors.toList());
        return JsonResult.ok(result);
    }

    @RequireRole(3)
    @Operation(summary = "分页查询教练申请记录",
            description = "支持按 status 和学员姓名搜索，返回申请记录及学员/教练详情")
    @GetMapping
    public JsonResult<Page<Map<String, Object>>> list(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(required = false) String keyword) {
        return JsonResult.ok(coachApplicationService.pageWithDetails(new Page<>(page, size), status, keyword));
    }

    @Operation(summary = "查询某个学员的所有申请记录",
            description = "返回该学员的申请/移交记录，含教练姓名、申请类型等信息")
    @GetMapping("/student/{studentId}")
    public JsonResult<List<Map<String, Object>>> listByStudent(@PathVariable Integer studentId) {
        List<CoachApplication> list = coachApplicationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CoachApplication>()
                        .eq(CoachApplication::getStudentId, studentId)
                        .orderByDesc(CoachApplication::getCreateTime));

        List<Map<String, Object>> result = list.stream().map(app -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", app.getId());
            map.put("studentId", app.getStudentId());
            map.put("coachId", app.getCoachId());
            map.put("sourceCoachId", app.getSourceCoachId());
            map.put("transferReason", app.getTransferReason());
            map.put("status", app.getStatus());
            map.put("applyTime", app.getApplyTime());
            map.put("auditTime", app.getAuditTime());
            map.put("auditReason", app.getAuditReason());

            // 目标教练姓名
            Coach targetCoach = coachMapper.selectById(app.getCoachId());
            if (targetCoach != null) {
                User targetUser = userMapper.selectById(targetCoach.getUserId());
                map.put("coachName", targetUser != null ? targetUser.getRealName() : "未知");
            } else {
                map.put("coachName", "未知");
            }

            // 如果是教练移交，显示发起教练
            if (app.getSourceCoachId() != null) {
                Coach sourceCoach = coachMapper.selectById(app.getSourceCoachId());
                if (sourceCoach != null) {
                    User sourceUser = userMapper.selectById(sourceCoach.getUserId());
                    map.put("sourceCoachName", sourceUser != null ? sourceUser.getRealName() : "未知");
                } else {
                    map.put("sourceCoachName", "未知");
                }
                map.put("applyType", "教练移交");
            } else {
                map.put("sourceCoachName", null);
                map.put("applyType", "学员申请");
            }

            return map;
        }).collect(Collectors.toList());
        return JsonResult.ok(result);
    }
}
