package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.ExamRegistration;
import com.homework.driveman.entity.ExamSession;
import com.homework.driveman.entity.LicenseConfig;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.LicenseConfigMapper;
import com.homework.driveman.mapper.TrainingRecordMapper;
import com.homework.driveman.service.IExamRegistrationService;
import com.homework.driveman.service.IExamSessionService;
import com.homework.driveman.service.IFileService;
import com.homework.driveman.service.IPdfService;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

    @Autowired
    private IPdfService pdfService;

    @Autowired
    private IFileService fileService;

    @Autowired
    private IUserService userService;

    @Autowired
    private LicenseConfigMapper licenseConfigMapper;

    @Autowired
    private TrainingRecordMapper trainingRecordMapper;

    @RequireRole(1)
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

        // ① 校验学员车型与场次车型是否匹配
        User student = userService.getById(studentId);
        if (student == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "学员不存在");
        }
        if (session.getLicenseType() != null
                && !session.getLicenseType().equals(student.getLicenseType())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    "该场次仅限 " + session.getLicenseType() + " 车型报名，您的车型为 " + student.getLicenseType());
        }

        // ② 校验学时是否达标
        LicenseConfig config = licenseConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LicenseConfig>()
                        .eq(LicenseConfig::getLicenseType, student.getLicenseType())
                        .eq(LicenseConfig::getSubject, session.getSubject()));
        if (config != null && config.getRequiredHours().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal trained = trainingRecordMapper.sumTrainingHours(
                    studentId, student.getLicenseType(), session.getSubject());
            if (trained.compareTo(config.getRequiredHours()) < 0) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                        "学时不足，当前 " + trained + " 小时，需 " + config.getRequiredHours() + " 小时");
            }
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

    @RequireRole(3)
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

            // 审核通过时生成带考试场次信息的准考证
            User student = userService.getById(registration.getStudentId());
            if (student != null) {
                String ticketPath = pdfService.generateAdmissionTicket(student, session);
                fileService.saveRecord(registration.getStudentId(), ticketPath,
                        "准考证_" + student.getRealName() + "_科目" + session.getSubject() + ".pdf",
                        "admission_ticket");
            }
        } else {
            registration.setStatus(2);
            registration.setAuditTime(LocalDateTime.now());
        }
        examRegistrationService.updateById(registration);
        return JsonResult.ok();
    }

    @RequireRole(3)
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

    @RequireRole(3)
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
