package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.CoachApplication;
import com.homework.driveman.entity.StudentCoach;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.CoachApplicationMapper;
import com.homework.driveman.mapper.StudentCoachMapper;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教练申请审核控制器
 * 学员申请 → 管理员审核 → 通过后写入 student_coach 绑定关系
 */
@Tag(name = "教练申请审核")
@RestController
@RequestMapping("/coach-applications")
public class CoachApplicationController {

    @Autowired
    private CoachApplicationMapper coachApplicationMapper;

    @Autowired
    private StudentCoachMapper studentCoachMapper;

    @RequireRole(1)
    @Operation(summary = "学员提交教练选择申请")
    @PostMapping
    public JsonResult<Void> apply(@RequestParam Integer studentId,
                                  @RequestParam Integer coachId) {
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
            description = "pass=true 审核通过（自动写入 student_coach 绑定），pass=false 拒绝并填写原因")
    @Transactional
    @PutMapping("/{id}/audit")
    public JsonResult<Void> audit(@PathVariable Integer id,
                                  @RequestParam boolean pass,
                                  @RequestParam(required = false) String reason) {
        CoachApplication application = coachApplicationMapper.selectById(id);
        if (application == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "申请记录不存在");
        }

        if (pass) {
            // 审核通过 → 绑定学员与教练
            application.setStatus(1);
            application.setAuditTime(LocalDateTime.now());

            StudentCoach sc = new StudentCoach();
            sc.setStudentId(application.getStudentId());
            sc.setCoachId(application.getCoachId());
            sc.setBindTime(LocalDateTime.now());
            sc.setStatus(1);
            studentCoachMapper.insert(sc);
        } else {
            // 拒绝
            application.setStatus(2);
            application.setAuditTime(LocalDateTime.now());
            application.setAuditReason(reason);
        }
        coachApplicationMapper.updateById(application);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "查询所有待审核的教练申请")
    @GetMapping("/pending")
    public JsonResult<List<CoachApplication>> listPending() {
        List<CoachApplication> list = coachApplicationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CoachApplication>()
                        .eq(CoachApplication::getStatus, 0)
                        .orderByDesc(CoachApplication::getCreateTime));
        return JsonResult.ok(list);
    }

    @Operation(summary = "查询某个学员的所有申请记录")
    @GetMapping("/student/{studentId}")
    public JsonResult<List<CoachApplication>> listByStudent(@PathVariable Integer studentId) {
        List<CoachApplication> list = coachApplicationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CoachApplication>()
                        .eq(CoachApplication::getStudentId, studentId)
                        .orderByDesc(CoachApplication::getCreateTime));
        return JsonResult.ok(list);
    }
}
