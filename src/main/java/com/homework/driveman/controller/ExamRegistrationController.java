package com.homework.driveman.controller;

import com.homework.driveman.entity.ExamRegistration;
import com.homework.driveman.entity.ExamSession;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.service.IExamRegistrationService;
import com.homework.driveman.service.IExamSessionService;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试报名控制器 — 学员报名考试、管理员审核、录入成绩
 */
@Tag(name = "考试报名管理")
@RestController
@RequestMapping("/exam-registrations")
public class ExamRegistrationController {

    @Autowired
    private IExamRegistrationService examRegistrationService;

    @Autowired
    private IExamSessionService examSessionService;

    @Operation(summary = "学员报名考试")
    @PostMapping
    public JsonResult<Void> apply(@RequestParam Integer studentId,
                                  @RequestParam Integer sessionId) {
        ExamSession session = examSessionService.getById(sessionId);
        if (session == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "考试场次不存在");
        }
        if (session.getRemainingQuota() <= 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该场次名额已满");
        }

        ExamRegistration registration = new ExamRegistration();
        registration.setStudentId(studentId);
        registration.setSessionId(sessionId);
        registration.setSubject(session.getSubject());
        registration.setStatus(0);
        registration.setRetakeCount(0);
        registration.setApplyTime(LocalDateTime.now());
        examRegistrationService.save(registration);
        return JsonResult.ok();
    }

    @Operation(summary = "审核考试报名",
            description = "pass=true 审核通过（扣减场次名额），pass=false 审核不通过")
    @PutMapping("/{id}/audit")
    public JsonResult<Void> audit(@PathVariable Integer id,
                                  @RequestParam boolean pass) {
        ExamRegistration registration = examRegistrationService.getById(id);
        if (registration == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "考试报名记录不存在");
        }

        if (pass) {
            // 审核通过 → 扣减场次剩余名额
            ExamSession session = examSessionService.getById(registration.getSessionId());
            if (session == null || session.getRemainingQuota() <= 0) {
                throw new ServiceException(ServiceCode.ERROR_CONFLICT, "场次名额不足");
            }
            session.setRemainingQuota(session.getRemainingQuota() - 1);
            // 名额归零时自动标记为已满
            if (session.getRemainingQuota() == 0) {
                session.setStatus(2);
            }
            examSessionService.updateById(session);

            registration.setStatus(1);
            registration.setAuditTime(LocalDateTime.now());
        } else {
            registration.setStatus(2);
            registration.setAuditTime(LocalDateTime.now());
        }
        examRegistrationService.updateById(registration);
        return JsonResult.ok();
    }

    @Operation(summary = "录入考试成绩",
            description = "score 0-100，≥90 为合格")
    @PutMapping("/{id}/score")
    public JsonResult<Void> enterScore(@PathVariable Integer id,
                                       @RequestParam Integer score) {
        if (score < 0 || score > 100) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "成绩须在0-100之间");
        }

        ExamRegistration registration = examRegistrationService.getById(id);
        if (registration == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "考试报名记录不存在");
        }

        registration.setScore(score);
        registration.setPassStatus(score >= 90 ? 1 : 0);
        registration.setStatus(3); // 已考试
        if (score < 90) {
            registration.setRetakeCount(registration.getRetakeCount() + 1);
        }
        examRegistrationService.updateById(registration);
        return JsonResult.ok();
    }

    @Operation(summary = "查询所有考试报名记录")
    @GetMapping
    public JsonResult<List<ExamRegistration>> list() {
        return JsonResult.ok(examRegistrationService.list());
    }

    @Operation(summary = "根据学员ID查询其考试报名记录")
    @GetMapping("/student/{studentId}")
    public JsonResult<List<ExamRegistration>> listByStudent(@PathVariable Integer studentId) {
        List<ExamRegistration> list = examRegistrationService.lambdaQuery()
                .eq(ExamRegistration::getStudentId, studentId)
                .orderByDesc(ExamRegistration::getApplyTime)
                .list();
        return JsonResult.ok(list);
    }
}
