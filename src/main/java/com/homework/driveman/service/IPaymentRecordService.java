package com.homework.driveman.service;

import com.homework.driveman.entity.PaymentRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 支付记录服务 — 收入统计、欠费管理
 */
public interface IPaymentRecordService {

    /** 创建支付记录 */
    PaymentRecord create(PaymentRecord record);

    /** 根据 ID 查询 */
    PaymentRecord getById(Integer id);

    /** 自动生成支付记录（供业务审核通过时调用，按 fee_standard 定价） */
    PaymentRecord autoCreate(Integer studentId, String bizType, Integer bizId, BigDecimal amount, String description);

    /** 确认支付 (status: 0 → 1) */
    PaymentRecord pay(Integer id);

    /** 退款 (status: 1 → 2) */
    PaymentRecord refund(Integer id);

    /** 按条件查询支付记录 */
    List<PaymentRecord> list(Integer studentId, String bizType, Integer status);

    /** 欠费清单（待支付 + 学员信息） */
    List<Map<String, Object>> listOutstanding();

    /** 收入看板数据（支持按年份筛选；不传时为近12月+当月） */
    Map<String, Object> getRevenueSummary(Integer year);
}
