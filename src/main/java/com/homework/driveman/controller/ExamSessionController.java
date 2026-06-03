package com.homework.driveman.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.ExamSession;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.service.IExamSessionService;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 考试场次管理控制器 — 发布/修改/查询/删除考试场次
 * 查询接口对所有登录用户开放（学员/教练/管理员均可查看），
 * 增删改仅限管理员（role=3）。
 */
@Tag(name = "考试场次管理")
@RestController
@RequestMapping("/exam-sessions")
public class ExamSessionController {

    @Autowired
    private IExamSessionService examSessionService;

    @Operation(summary = "分页查询考试场次",
            description = "所有登录用户可调用，可按科目或车型筛选")
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

    @Operation(summary = "根据ID查询考试场次",
            description = "所有登录用户可调用")
    @GetMapping("/{id}")
    public JsonResult<ExamSession> getById(@PathVariable Integer id) {
        return JsonResult.ok(examSessionService.getById(id));
    }

    @RequireRole(3)
    @Operation(summary = "发布考试场次",
            description = "仅管理员可调用，自动校验参数合法性")
    @PostMapping
    public JsonResult<Void> create(@RequestBody ExamSession examSession) {
        validateExamSession(examSession, true);
        examSessionService.save(examSession);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改考试场次",
            description = "仅管理员可调用，自动校验参数合法性")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody ExamSession examSession) {
        if (examSessionService.getById(id) == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "考试场次不存在");
        }
        examSession.setId(id);
        validateExamSession(examSession, false);
        examSessionService.updateById(examSession);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除考试场次",
            description = "仅管理员可调用，逻辑删除")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        if (examSessionService.getById(id) == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "考试场次不存在");
        }
        examSessionService.removeById(id);
        return JsonResult.ok();
    }

    /**
     * 校验考试场次参数的合法性
     */
    private void validateExamSession(ExamSession session, boolean isCreate) {
        if (session.getSubject() == null || session.getSubject() < 1 || session.getSubject() > 4) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "科目必须为 1-4");
        }
        if (session.getExamDate() == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "考试日期不能为空");
        }
        if (isCreate && session.getExamDate().isBefore(LocalDate.now())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "考试日期不能早于当前日期");
        }
        if (session.getTotalQuota() == null || session.getTotalQuota() <= 0) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "总名额必须大于 0");
        }
        if (session.getRemainingQuota() == null || session.getRemainingQuota() < 0) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "剩余名额不能为负数");
        }
        if (session.getRemainingQuota() > session.getTotalQuota()) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "剩余名额不能超过总名额");
        }
        if (session.getLocation() == null || session.getLocation().isBlank()) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "考试地点不能为空");
        }
        if (session.getLicenseType() == null || session.getLicenseType().isBlank()) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "适用车型不能为空");
        }
    }
}
