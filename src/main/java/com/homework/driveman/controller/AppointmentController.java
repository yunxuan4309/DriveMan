package com.homework.driveman.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.Appointment;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.service.IAppointmentService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 约课管理控制器 — 学员预约/取消课程接口
 */
@Tag(name = "约课管理")
@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    @Autowired
    private IAppointmentService appointmentService;

    @Operation(summary = "分页查询约课记录",
            description = "可按学员ID或教练ID筛选")
    @GetMapping
    public JsonResult<Page<Appointment>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) Integer studentId,
                                              @RequestParam(required = false) Integer coachId) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<Appointment>()
                .eq(studentId != null, Appointment::getStudentId, studentId)
                .eq(coachId != null, Appointment::getCoachId, coachId)
                .orderByDesc(Appointment::getCreateTime);
        return JsonResult.ok(appointmentService.page(new Page<>(page, size), wrapper));
    }

    @Operation(summary = "根据ID查询约课")
    @GetMapping("/{id}")
    public JsonResult<Appointment> getById(@PathVariable Integer id) {
        Appointment appointment = appointmentService.getById(id);
        return JsonResult.ok(appointment);
    }

    @RequireRole(1)
    @Operation(summary = "新增约课")
    @PostMapping
    public JsonResult<Void> add(@RequestBody Appointment appointment) {
        appointmentService.save(appointment);
        return JsonResult.ok();
    }

    @RequireRole({1, 3})
    @Operation(summary = "取消约课", description = "将约课状态置为已取消，可选填取消原因；学员只能取消自己的约课")
    @PutMapping("/{id}/cancel")
    public JsonResult<Void> cancel(@PathVariable Integer id,
                                   @RequestParam(required = false) String reason,
                                   HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        Appointment appointment = appointmentService.getById(id);
        if (appointment == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "约课记录不存在");
        }
        // 学员只能取消自己的约课，管理员可取消任何约课
        if (currentUser.getRole() == 1 && !appointment.getStudentId().equals(currentUser.getUserId())) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "无权取消他人的约课");
        }
        appointment.setStatus(3);
        appointment.setCancelReason(reason);
        appointmentService.updateById(appointment);
        return JsonResult.ok();
    }
}
