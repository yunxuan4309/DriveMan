package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.driveman.entity.PaymentRecord;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.PaymentRecordMapper;
import com.homework.driveman.service.IPaymentRecordService;
import com.homework.driveman.web.ServiceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 支付记录实现
 */
@Service
public class PaymentRecordServiceImpl implements IPaymentRecordService {

    /** 有效业务类型集合 */
    public static final Set<String> VALID_BIZ_TYPES = Set.of(
            "registration_fee", "exam_fee", "familiarization_fee", "training_fee", "other"
    );

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Override
    public PaymentRecord getById(Integer id) {
        return paymentRecordMapper.selectById(id);
    }

    @Override
    public PaymentRecord autoCreate(Integer studentId, String bizType, Integer bizId,
                                     BigDecimal amount, String description) {
        // 按 fee_standard 定价自动生成待支付账单
        PaymentRecord record = new PaymentRecord();
        record.setStudentId(studentId);
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setAmount(amount);
        record.setStatus(0); // 待支付
        record.setRemark(description);
        paymentRecordMapper.insert(record);
        return record;
    }

    @Override
    public PaymentRecord create(PaymentRecord record) {
        if (record.getStudentId() == null || record.getAmount() == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "学员ID和金额不能为空");
        }
        if (record.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "金额必须大于0");
        }
        if (record.getBizType() != null && !VALID_BIZ_TYPES.contains(record.getBizType())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    "无效的业务类型，可选值: " + String.join(", ", VALID_BIZ_TYPES));
        }
        if (record.getStatus() == null) {
            record.setStatus(0);
        }
        if (record.getBizType() == null) {
            record.setBizType("other");
        }
        paymentRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public PaymentRecord pay(Integer id) {
        PaymentRecord record = paymentRecordMapper.selectById(id);
        if (record == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "支付记录不存在");
        }
        if (record.getStatus() != 0) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "当前状态不允许支付");
        }
        record.setStatus(1);
        record.setPayTime(LocalDateTime.now());
        paymentRecordMapper.updateById(record);
        return record;
    }

    @Override
    @Transactional
    public PaymentRecord refund(Integer id) {
        PaymentRecord record = paymentRecordMapper.selectById(id);
        if (record == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "支付记录不存在");
        }
        if (record.getStatus() != 1) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "只能对已支付的记录进行退款");
        }
        record.setStatus(2);
        record.setRefundTime(LocalDateTime.now());
        paymentRecordMapper.updateById(record);
        return record;
    }

    @Override
    public List<PaymentRecord> list(Integer studentId, String bizType, Integer status) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<>();
        if (studentId != null) {
            wrapper.eq(PaymentRecord::getStudentId, studentId);
        }
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(PaymentRecord::getBizType, bizType);
        }
        if (status != null) {
            wrapper.eq(PaymentRecord::getStatus, status);
        }
        wrapper.orderByDesc(PaymentRecord::getCreateTime);
        return paymentRecordMapper.selectList(wrapper);
    }

    @Override
    public List<Map<String, Object>> listOutstanding() {
        return paymentRecordMapper.selectOutstandingList();
    }

    @Override
    public Map<String, Object> getRevenueSummary(Integer year) {
        // 月度趋势
        List<Map<String, Object>> monthlyRows = paymentRecordMapper.selectMonthlyRevenue(year);

        List<String> months = new ArrayList<>();
        List<Double> monthlyValues = new ArrayList<>();

        // 填充12个月（无收入的月份补0）
        Set<String> monthSet = new HashSet<>();
        for (Map<String, Object> row : monthlyRows) {
            monthSet.add(row.get("month").toString());
        }
        LocalDateTime baseTime = (year != null)
                ? LocalDateTime.of(year, 1, 1, 0, 0)
                : LocalDateTime.now();
        for (int i = 11; i >= 0; i--) {
            String month = baseTime.minusMonths(i).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            months.add(month);
            if (monthSet.contains(month)) {
                monthlyRows.stream()
                        .filter(r -> r.get("month").toString().equals(month))
                        .findFirst()
                        .ifPresent(r -> monthlyValues.add(((Number) r.get("total")).doubleValue()));
            } else {
                monthlyValues.add(0.0);
            }
        }

        String monthlyTitle = (year != null) ? year + "年收入趋势" : "近12个月收入趋势";
        Map<String, Object> monthlyChart = new LinkedHashMap<>();
        monthlyChart.put("title", Map.of("text", monthlyTitle));
        monthlyChart.put("xAxis", Map.of("type", "category", "data", months));
        monthlyChart.put("yAxis", Map.of("type", "value"));
        monthlyChart.put("series", List.of(Map.of(
                "name", "收入", "type", "bar", "data", monthlyValues
        )));

        // 收入来源分布
        List<Map<String, Object>> bizRows = paymentRecordMapper.selectRevenueByBizType(year);
        List<Map<String, Object>> bizPieData = new ArrayList<>();
        for (Map<String, Object> row : bizRows) {
            String bizType = row.get("biz_type").toString();
            String label = switch (bizType) {
                case "registration_fee" -> "报名费";
                case "exam_fee" -> "考试费";
                case "familiarization_fee" -> "合场费";
                case "training_fee" -> "二次培训费";
                default -> bizType;
            };
            bizPieData.add(Map.of("name", label, "value", row.get("total")));
        }
        if (bizPieData.isEmpty()) {
            bizPieData.add(Map.of("name", "暂无数据", "value", 1));
        }

        String pieTitle = (year != null) ? year + "年收入来源" : "当月收入来源";
        Map<String, Object> bizTypeChart = new LinkedHashMap<>();
        bizTypeChart.put("title", Map.of("text", pieTitle, "left", "center"));
        bizTypeChart.put("series", List.of(Map.of(
                "name", "收入来源", "type", "pie", "radius", "50%", "data", bizPieData
        )));

        // 汇总
        Map<String, Object> summary = paymentRecordMapper.selectPaymentSummary();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("monthlyChart", monthlyChart);
        result.put("bizTypeChart", bizTypeChart);
        result.put("summary", summary);
        return result;
    }
}
