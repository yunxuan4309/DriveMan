package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.FeeStandard;
import com.homework.driveman.entity.PaymentRecord;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.service.IFeeStandardService;
import com.homework.driveman.service.IFileService;
import com.homework.driveman.service.IPaymentRecordService;
import com.homework.driveman.service.IPdfService;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.utils.JwtUtils;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 驾考报名控制器 — 准学员(role=0)选择套餐、支付、升级为正式学员(role=1)
 */
@Tag(name = "驾考报名")
@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    /** 需要管理员审核的车型（首次报名时不能直接支付，需审核通过后才生成账单） */
    private static final java.util.Set<String> AUDIT_REQUIRED_TYPES = java.util.Set.of("C5");

    @Autowired
    private IFeeStandardService feeStandardService;

    @Autowired
    private IPaymentRecordService paymentRecordService;

    @Autowired
    private IUserService userService;

    @Autowired
    private IPdfService pdfService;

    @Autowired
    private IFileService fileService;

    @Autowired
    private JwtUtils jwtUtils;

    @RequireRole({0, 1, 3})
    @Operation(summary = "查询可选报名套餐",
            description = "返回 fee_standard 表中 subject 为 NULL 的全包套餐，按车型分组")
    @GetMapping("/packages")
    public JsonResult<Map<String, Object>> listPackages() {
        List<FeeStandard> packages = feeStandardService.lambdaQuery()
                .isNull(FeeStandard::getSubject)
                .orderByAsc(FeeStandard::getLicenseType)
                .list();

        // 按 licenseType 分组
        Map<String, List<FeeStandard>> grouped = packages.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        FeeStandard::getLicenseType,
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("packages", packages);
        result.put("byType", grouped);
        return JsonResult.ok(result);
    }

    @RequireRole(0)
    @Operation(summary = "准学员提交驾考报名",
            description = "选择车型和套餐，生成待支付账单。支付完成后升级为正式学员(role=1)")
    @PostMapping("/apply")
    public JsonResult<Map<String, Object>> apply(@RequestParam Integer studentId,
                                                  @RequestParam String licenseType,
                                                  @RequestParam Integer packageId,
                                                  HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        if (!currentUser.getUserId().equals(studentId)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "只能为自己报名");
        }

        User user = userService.getById(studentId);
        if (user == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "用户不存在");
        }
        if (user.getRole() != 0) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "您已是正式学员，无需重复报名");
        }

        FeeStandard pkg = feeStandardService.getById(packageId);
        if (pkg == null || pkg.getSubject() != null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "套餐不存在");
        }

        // 需要审核的车型（如C5）→ 不生成账单，进入待审核状态
        if (AUDIT_REQUIRED_TYPES.contains(licenseType)) {
            // 防重复提交：已提交过 C5 申请（licenseType 已设为 C5 且 role=0）则直接返回
            if ("C5".equals(user.getLicenseType())) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("needAudit", true);
                result.put("message", "您的报名申请正在审核中，请耐心等待");
                result.put("licenseType", licenseType);
                return JsonResult.ok(result);
            }
            // 首次提交：更新 licenseType
            // 注意：不修改 user.status（保持 status=1 以维持正常登录）
            // 待审核状态由 role=0 + licenseType=C5 组合隐式表达
            user.setLicenseType(licenseType);
            userService.updateById(user);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("needAudit", true);
            result.put("message", licenseType + " 车型需要管理员审核，请等待审核结果");
            result.put("licenseType", licenseType);
            return JsonResult.ok(result);
        }

        // 普通车型 → 更新 licenseType + 创建待支付账单
        user.setLicenseType(licenseType);
        userService.updateById(user);

        // 普通车型 → 直接创建待支付账单
        BigDecimal amount = pkg.getAmount() != null ? pkg.getAmount() : BigDecimal.ZERO;
        PaymentRecord payment = paymentRecordService.autoCreate(
                studentId, "enrollment_fee", studentId, amount,
                licenseType + " " + pkg.getDescription());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paymentId", payment.getId());
        result.put("amount", amount);
        result.put("licenseType", licenseType);
        result.put("description", pkg.getDescription());
        return JsonResult.ok(result);
    }

    @RequireRole({0, 1})
    @Operation(summary = "支付报名套餐",
            description = "模拟支付，支付成功后自动升级为准学员为正式学员(role=1)，并生成报名表和准考证PDF")
    @Transactional
    @PutMapping("/{paymentId}/pay")
    public JsonResult<Map<String, Object>> pay(@PathVariable Integer paymentId,
                                                HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");

        PaymentRecord payment = paymentRecordService.getById(paymentId);
        if (payment == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "支付记录不存在");
        }
        if (!payment.getStudentId().equals(currentUser.getUserId())) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "无权操作此支付记录");
        }
        if (!"enrollment_fee".equals(payment.getBizType())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "该记录不是报名套餐支付");
        }
        if (payment.getStatus() == 1) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "已支付，无需重复操作");
        }

        // 执行支付
        paymentRecordService.pay(paymentId);

        // 升级为正式学员
        User user = userService.getById(currentUser.getUserId());
        user.setRole(1);
        userService.updateById(user);

        // 生成新 JWT（role=1），前端替换旧 token 以避免权限不足
        String newToken = jwtUtils.generateToken(
                new CurrentUser(user.getUserId(), user.getUsername(), 1));

        // 生成 PDF（报名表 + 准考证）
        String regPath = pdfService.generateRegistrationForm(user);
        fileService.saveRecord(user.getUserId(), regPath,
                "报名表_" + user.getRealName() + ".pdf", "registration_pdf",
                "registration_form", user.getUserId());

        String ticketPath = pdfService.generateAdmissionTicket(user, null);
        fileService.saveRecord(user.getUserId(), ticketPath,
                "准考证_" + user.getRealName() + ".pdf", "admission_ticket",
                "exam_ticket", user.getUserId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", newToken);
        result.put("role", 1);
        result.put("message", "支付成功，您已成为正式学员");
        return JsonResult.ok(result);
    }
}
