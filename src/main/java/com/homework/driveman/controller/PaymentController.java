package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.PaymentRecord;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.service.IPaymentRecordService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 支付记录控制器 — 收入管理、欠费管理
 */
@Tag(name = "支付管理")
@RestController
@RequestMapping("/payment-records")
public class PaymentController {

    @Autowired
    private IPaymentRecordService paymentRecordService;

    @RequireRole(3)
    @Operation(summary = "创建支付记录", description = "手动创建一笔待支付记录（管理员录入）")
    @PostMapping
    public JsonResult<PaymentRecord> create(@RequestBody PaymentRecord record) {
        return JsonResult.ok(paymentRecordService.create(record));
    }

    @RequireRole(3)
    @Operation(summary = "分页查询支付记录",
            description = "按学员姓名、业务类型、状态筛选，含学员姓名。前端需实现分页组件。")
    @GetMapping
    public JsonResult<Page<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页条数") int size,
            @RequestParam(required = false) @Parameter(description = "学员ID") Integer studentId,
            @RequestParam(required = false) @Parameter(description = "学员姓名关键字") String keyword,
            @RequestParam(required = false) @Parameter(description = "业务类型") String bizType,
            @RequestParam(required = false) @Parameter(description = "状态：0-待支付, 1-已支付, 2-已退款") Integer status) {
        return JsonResult.ok(paymentRecordService.pageList(new Page<>(page, size), studentId, keyword, bizType, status));
    }

    @RequireRole(3)
    @Operation(summary = "确认支付", description = "将待支付记录标记为已支付（模拟支付）")
    @PutMapping("/{id}/pay")
    public JsonResult<PaymentRecord> pay(@PathVariable Integer id) {
        return JsonResult.ok(paymentRecordService.pay(id));
    }

    @RequireRole(3)
    @Operation(summary = "退款", description = "将已支付的记录退款")
    @PutMapping("/{id}/refund")
    public JsonResult<PaymentRecord> refund(@PathVariable Integer id) {
        return JsonResult.ok(paymentRecordService.refund(id));
    }

    @RequireRole(3)
    @Operation(summary = "欠费清单（分页）",
            description = "所有待支付记录，含学员姓名、电话、车型信息。支持多条件筛选。")
    @GetMapping("/outstanding")
    public JsonResult<Page<Map<String, Object>>> listOutstanding(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页条数") int size,
            @RequestParam(required = false) @Parameter(description = "学员姓名关键字") String keyword,
            @RequestParam(required = false) @Parameter(description = "手机号") String phone,
            @RequestParam(required = false) @Parameter(description = "报考车型") String licenseType,
            @RequestParam(required = false) @Parameter(description = "业务类型") String bizType) {
        return JsonResult.ok(paymentRecordService.pageOutstanding(new Page<>(page, size), keyword, phone, licenseType, bizType));
    }

    // ==================== 学员端接口 ====================

    @RequireRole(1)
    @Operation(summary = "查看我的账单", description = "学员查看自己所有支付记录（待支付/已支付/已退款）")
    @GetMapping("/my")
    public JsonResult<List<PaymentRecord>> getMyPayments(HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        return JsonResult.ok(paymentRecordService.list(currentUser.getUserId(), null, null));
    }

    @RequireRole(1)
    @Operation(summary = "支付我的账单", description = "学员支付自己的待支付账单（模拟支付，一点即付）")
    @PutMapping("/{id}/my-pay")
    public JsonResult<PaymentRecord> myPay(@PathVariable Integer id, HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        // 校验归属：学员只能支付自己的账单
        PaymentRecord record = paymentRecordService.getById(id);
        if (record == null || !record.getStudentId().equals(currentUser.getUserId())) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "只能支付自己的账单");
        }
        return JsonResult.ok(paymentRecordService.pay(id));
    }
}
