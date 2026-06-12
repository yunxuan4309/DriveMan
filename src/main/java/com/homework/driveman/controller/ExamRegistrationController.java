package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.ExamRegistration;
import com.homework.driveman.entity.ExamSession;
import com.homework.driveman.entity.FeeStandard;
import com.homework.driveman.entity.LicenseConfig;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.ExamRegistrationMapper;
import com.homework.driveman.mapper.LicenseConfigMapper;
import com.homework.driveman.service.IConfigService;
import com.homework.driveman.mapper.TrainingRecordMapper;
import com.homework.driveman.service.IExamRegistrationService;
import com.homework.driveman.service.IExamSessionService;
import com.homework.driveman.service.IFeeStandardService;
import com.homework.driveman.service.IFileService;
import com.homework.driveman.service.IPaymentRecordService;
import com.homework.driveman.service.IPhysicalExamService;
import com.homework.driveman.service.IPdfService;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private IFeeStandardService feeStandardService;

    @Autowired
    private IPaymentRecordService paymentRecordService;

    @Autowired
    private LicenseConfigMapper licenseConfigMapper;

    @Autowired
    private TrainingRecordMapper trainingRecordMapper;

    @Autowired
    private IConfigService configService;

    @Autowired
    private ExamRegistrationMapper examRegistrationMapper;

    @Autowired
    private IPhysicalExamService physicalExamService;

    /** 从 config 表读取合格分数线，默认 90 */
    private int getPassScore() {
        String val = configService.getConfigValue("exam_pass_score");
        if (val == null) return 90;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 90;
        }
    }

    @RequireRole(1)
    @Operation(summary = "学员报名考试")
    @PostMapping
    public JsonResult<Void> apply(@RequestParam Integer studentId,
                                  @RequestParam Integer sessionId,
                                  HttpServletRequest request) {
        // 只能为自己报名
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        if (!currentUser.getUserId().equals(studentId)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "只能为自己报名考试");
        }

        // 检查体检是否不合格
        physicalExamService.checkPassed(studentId);

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

        // ③ 检测该科目是否已通过（通过后不可再次报名）
        Set<Integer> passedSubjects = examRegistrationMapper.findPassedSubjectsByStudent(studentId);
        if (passedSubjects.contains(session.getSubject())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    "您已通过科目" + session.getSubject() + "，无需重复报名");
        }

        // ④ 检测是否为补考：该学员同一科目是否有不合格记录
        boolean isRetake = examRegistrationService.lambdaQuery()
                .eq(ExamRegistration::getStudentId, studentId)
                .eq(ExamRegistration::getSubject, session.getSubject())
                .eq(ExamRegistration::getPassStatus, 0)
                .count() > 0;

        ExamRegistration registration = new ExamRegistration();
        registration.setStudentId(studentId);
        registration.setSessionId(sessionId);
        registration.setSubject(session.getSubject());
        registration.setStatus(0);
        registration.setRetakeCount(0);
        registration.setIsRetake(isRetake ? 1 : 0);
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
            // 审核通过 → 扣减场次剩余名额（乐观锁防并发）
            ExamSession session = examSessionService.getById(registration.getSessionId());
            if (session == null || session.getRemainingQuota() <= 0) {
                throw new ServiceException(ServiceCode.ERROR_CONFLICT, "场次名额不足");
            }
            session.setRemainingQuota(session.getRemainingQuota() - 1);
            // 名额归零时自动标记为已满
            if (session.getRemainingQuota() == 0) {
                session.setStatus(2);
            }
            boolean sessionUpdated = examSessionService.updateById(session);
            if (!sessionUpdated) {
                throw new ServiceException(ServiceCode.ERROR_CONFLICT, "名额已被其他管理员占用，请刷新后重试");
            }

            registration.setStatus(1);
            registration.setAuditTime(LocalDateTime.now());

            // 审核通过时生成带考试场次信息的准考证
            User student = userService.getById(registration.getStudentId());
            if (student != null) {
                String ticketPath = pdfService.generateAdmissionTicket(student, session);
                fileService.saveRecord(registration.getStudentId(), ticketPath,
                        "准考证_" + student.getRealName() + "_科目" + session.getSubject() + ".pdf",
                        "admission_ticket", "exam_ticket", registration.getId());
            }

            // 审核通过时，根据科目费用标准自动生成待支付账单
            // 注意：补考和首次考试按同一科目考试费标准收费（amount）。
            // 二次培训费（非全包学员挂科后的额外培训）走 retake_training_record 流程，与此无关。
            FeeStandard examFee = feeStandardService.lambdaQuery()
                    .eq(FeeStandard::getLicenseType, session.getLicenseType())
                    .eq(FeeStandard::getSubject, session.getSubject())
                    .one();
            if (examFee != null) {
                paymentRecordService.autoCreate(registration.getStudentId(), "exam_fee", id,
                        examFee.getAmount(),
                        session.getLicenseType() + " 科目" + session.getSubject() + " " + examFee.getDescription());
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
            description = "score 0-100，合格分数线从系统配置读取（默认 90 分）")
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

        int passScore = getPassScore();
        registration.setScore(score);
        registration.setPassStatus(score >= passScore ? 1 : 0);
        registration.setStatus(3); // 已考试
        if (score < passScore) {
            registration.setRetakeCount(registration.getRetakeCount() + 1);
        }
        examRegistrationService.updateById(registration);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "分页查询考试报名记录",
            description = "支持按 status 和学员姓名搜索，返回报名记录及关联的学员姓名、考试日期、地点等信息")
    @GetMapping
    public JsonResult<Page<Map<String, Object>>> list(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) Integer status,
                                                       @RequestParam(required = false) String keyword) {
        return JsonResult.ok(examRegistrationService.pageWithDetails(new Page<>(page, size), status, keyword));
    }

    @Operation(summary = "根据学员ID查询其考试报名记录（分页）",
            description = "返回报名记录及关联的考试日期、地点等场次信息，支持按状态和科目筛选")
    @GetMapping("/student/{studentId}")
    public JsonResult<Page<Map<String, Object>>> listByStudent(@PathVariable Integer studentId,
                                                               @RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int size,
                                                               @RequestParam(required = false) Integer status,
                                                               @RequestParam(required = false) Integer subject) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamRegistration> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamRegistration>()
                        .eq(ExamRegistration::getStudentId, studentId)
                        .eq(status != null, ExamRegistration::getStatus, status)
                        .eq(subject != null, ExamRegistration::getSubject, subject)
                        .orderByDesc(ExamRegistration::getApplyTime);
        Page<ExamRegistration> regPage = examRegistrationService.page(new Page<>(page, size), wrapper);

        List<Map<String, Object>> result = regPage.getRecords().stream().map(reg -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", reg.getId());
            map.put("studentId", reg.getStudentId());
            map.put("sessionId", reg.getSessionId());
            map.put("subject", reg.getSubject());
            map.put("subjectName", "科目" + reg.getSubject());
            map.put("status", reg.getStatus());
            map.put("score", reg.getScore());
            map.put("passStatus", reg.getPassStatus());
            map.put("retakeCount", reg.getRetakeCount());
            map.put("isRetake", reg.getIsRetake());
            map.put("applyTime", reg.getApplyTime());
            map.put("auditTime", reg.getAuditTime());

            if (reg.getSessionId() != null) {
                ExamSession session = examSessionService.getById(reg.getSessionId());
                if (session != null) {
                    map.put("examDate", session.getExamDate());
                    map.put("startTime", session.getStartTime());
                    map.put("location", session.getLocation());
                    map.put("licenseType", session.getLicenseType());
                }
            }

            return map;
        }).collect(java.util.stream.Collectors.toList());

        Page<Map<String, Object>> resultPage = new Page<>(regPage.getCurrent(), regPage.getSize(), regPage.getTotal());
        resultPage.setRecords(result);
        return JsonResult.ok(resultPage);
    }

    @RequireRole(1)
    @Operation(summary = "查询我的各科成绩", description = "学员查看自己各科目考试成绩及合格状态")
    @GetMapping("/my-scores/{studentId}")
    public JsonResult<List<Map<String, Object>>> getMyScores(@PathVariable Integer studentId) {
        // 查询该学员所有考试报名记录（包括未出成绩的）
        List<ExamRegistration> examResults = examRegistrationService.lambdaQuery()
                .eq(ExamRegistration::getStudentId, studentId)
                .orderByAsc(ExamRegistration::getSubject)
                .list();

        // 组装返回数据
        List<Map<String, Object>> result = examResults.stream().map(reg -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("subject", reg.getSubject());
            map.put("subjectName", "科目" + reg.getSubject());
            map.put("score", reg.getScore());
            map.put("passStatus", reg.getPassStatus());
            map.put("passResult", reg.getPassStatus() == null ? "待考试" : (reg.getPassStatus() == 1 ? "合格" : "不合格"));
            map.put("status", reg.getStatus());
            map.put("statusDesc", getStatusDesc(reg.getStatus()));
            map.put("retakeCount", reg.getRetakeCount());
            map.put("isRetake", reg.getIsRetake());
            map.put("applyTime", reg.getApplyTime());

            // 查询考试场次信息
            if (reg.getSessionId() != null) {
                ExamSession session = examSessionService.getById(reg.getSessionId());
                if (session != null) {
                    map.put("examDate", session.getExamDate());
                    map.put("location", session.getLocation());
                }
            }
            
            return map;
        }).collect(java.util.stream.Collectors.toList());

        return JsonResult.ok(result);
    }

    /**
     * 获取报名状态描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "审核通过";
            case 2 -> "审核不通过";
            case 3 -> "已考试";
            default -> "未知";
        };
    }
}
