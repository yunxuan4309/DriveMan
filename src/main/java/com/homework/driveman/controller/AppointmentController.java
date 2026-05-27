package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.Appointment;
import com.homework.driveman.service.IAppointmentService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 约课管理控制器 — 学员预约/取消课程接口
 */
@Tag(name = "约课管理")
@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    @Autowired
    private IAppointmentService appointmentService;

    @Operation(summary = "查询所有约课记录")
    @GetMapping
    public JsonResult<List<Appointment>> list() {
        List<Appointment> list = appointmentService.list();
        return JsonResult.ok(list);
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
    @Operation(summary = "取消约课", description = "将约课状态置为已取消，可选填取消原因")
    @PutMapping("/{id}/cancel")
    public JsonResult<Void> cancel(@PathVariable Integer id,
                                   @RequestParam(required = false) String reason) {
        Appointment appointment = appointmentService.getById(id);
        if (appointment != null) {
            appointment.setStatus(3);
            appointment.setCancelReason(reason);
            appointmentService.updateById(appointment);
        }
        return JsonResult.ok();
    }
}
