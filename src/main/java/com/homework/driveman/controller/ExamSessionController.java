package com.homework.driveman.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.ExamSession;
import com.homework.driveman.service.IExamSessionService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 考试场次管理控制器 — 发布/修改/查询/删除考试场次
 */
@Tag(name = "考试场次管理")
@RestController
@RequestMapping("/exam-sessions")
public class ExamSessionController {

    @Autowired
    private IExamSessionService examSessionService;

    @Operation(summary = "分页查询考试场次",
            description = "可按科目或车型筛选")
    @GetMapping
    public JsonResult<Page<ExamSession>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) Integer subject,
                                              @RequestParam(required = false) String licenseType) {
        LambdaQueryWrapper<ExamSession> wrapper = new LambdaQueryWrapper<ExamSession>()
                .eq(subject != null, ExamSession::getSubject, subject)
                .eq(licenseType != null, ExamSession::getLicenseType, licenseType)
                .orderByAsc(ExamSession::getExamDate);
        return JsonResult.ok(examSessionService.page(new Page<>(page, size), wrapper));
    }

    @Operation(summary = "根据ID查询考试场次")
    @GetMapping("/{id}")
    public JsonResult<ExamSession> getById(@PathVariable Integer id) {
        return JsonResult.ok(examSessionService.getById(id));
    }

    @RequireRole(3)
    @Operation(summary = "发布考试场次")
    @PostMapping
    public JsonResult<Void> create(@RequestBody ExamSession examSession) {
        examSessionService.save(examSession);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改考试场次")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody ExamSession examSession) {
        examSession.setId(id);
        examSessionService.updateById(examSession);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除考试场次")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        examSessionService.removeById(id);
        return JsonResult.ok();
    }
}
