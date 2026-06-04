package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.*;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.*;
import com.homework.driveman.service.IPaymentRecordService;
import com.homework.driveman.service.IRetakeTrainingService;
import com.homework.driveman.web.ServiceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 二次培训（补考培训）服务实现
 *
 * 核心业务逻辑：
 * 1. 学员挂科后可申请二次培训
 * 2. 全包学员（报名时已支付全包套餐）→ isFree=1，免缴费，自动通过
 * 3. 非全包学员 → isFree=0，管理员审核时设定培训费，生成账单，缴费后开始培训
 * 4. 教练只读可见，无审核权限
 */
@Service
public class RetakeTrainingServiceImpl
        extends ServiceImpl<RetakeTrainingRecordMapper, RetakeTrainingRecord>
        implements IRetakeTrainingService {

    @Autowired
    private ConfigMapper configMapper;

    @Autowired
    private PaymentRecordMapper paymentRecordMapper;

    @Autowired
    private IPaymentRecordService paymentRecordService;

    @Autowired
    private ExamRegistrationMapper examRegistrationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StudentCoachMapper studentCoachMapper;

    @Override
    @Transactional
    public RetakeTrainingRecord apply(Integer studentId, Integer examRegistrationId,
                                      Integer coachId, String reason) {
        // 校验考试报名记录是否存在且为挂科状态
        ExamRegistration exam = examRegistrationMapper.selectById(examRegistrationId);
        if (exam == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "考试报名记录不存在");
        }
        if (exam.getPassStatus() == null || exam.getPassStatus() != 0) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "只能对不合格的考试申请二次培训");
        }
        if (!exam.getStudentId().equals(studentId)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "只能申请自己的二次培训");
        }

        // 校验是否已申请过（防止重复提交）
        long exists = count(new LambdaQueryWrapper<RetakeTrainingRecord>()
                .eq(RetakeTrainingRecord::getExamRegistrationId, examRegistrationId)
                .in(RetakeTrainingRecord::getStatus, 0, 1));
        if (exists > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该考试已申请二次培训，请勿重复提交");
        }

        // 校验教练是否合法（若指定了教练）
        if (coachId != null) {
            // 确保该学员确实绑定了该教练
            long binding = studentCoachMapper.selectCount(
                    new LambdaQueryWrapper<StudentCoach>()
                            .eq(StudentCoach::getStudentId, studentId)
                            .eq(StudentCoach::getCoachId, coachId)
                            .eq(StudentCoach::getStatus, 1));
            if (binding == 0) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "该教练不是您的绑定教练");
            }
        }

        // 判断学员是否全包：有已支付的全包套餐账单 → 全包学员
        boolean isFullPackage = paymentRecordMapper.selectCount(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getStudentId, studentId)
                        .eq(PaymentRecord::getBizType, "registration_fee")
                        .eq(PaymentRecord::getStatus, 1)) > 0;

        RetakeTrainingRecord record = new RetakeTrainingRecord();
        record.setStudentId(studentId);
        record.setCoachId(coachId);
        record.setExamRegistrationId(examRegistrationId);
        record.setSubject(exam.getSubject());
        record.setStatus(0); // 待审核

        if (isFullPackage) {
            // 全包学员：免缴费，自动审核通过
            record.setIsFree(1);
            record.setAmount(BigDecimal.ZERO);
            record.setPayStatus(0); // 无需缴费
            record.setStatus(1); // 直接进入培训中
            record.setAuditTime(LocalDateTime.now());
            record.setApplyReason(reason);
            save(record);
        } else {
            // 非全包学员：待审核，需管理员设定培训费
            record.setIsFree(0);
            record.setPayStatus(0); // 无需缴费（待审核通过后生成账单变为待缴费）
            record.setApplyReason(reason);
            save(record);
        }

        return record;
    }

    @Override
    @Transactional
    public void audit(Integer id, boolean pass, BigDecimal amount) {
        RetakeTrainingRecord record = getById(id);
        if (record == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "二次培训记录不存在");
        }
        if (record.getStatus() != 0) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "当前状态不允许审核");
        }
        // 全包学员不应走到审核流程（apply时已自动通过）
        if (record.getIsFree() == 1) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "全包学员已自动通过审核");
        }

        if (pass) {
            // 审核通过
            record.setStatus(1); // 培训中
            record.setAuditTime(LocalDateTime.now());

            // 培训费：优先使用传入值，否则从 config 表读取默认值
            if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                record.setAmount(amount);
            } else {
                String defaultFee = configMapper.getConfigValue("retake_training_fee");
                if (defaultFee != null) {
                    record.setAmount(new BigDecimal(defaultFee));
                } else {
                    record.setAmount(BigDecimal.ZERO);
                }
            }

            // 生成培训费账单
            if (record.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                record.setPayStatus(1); // 待缴费
                updateById(record);
                paymentRecordService.autoCreate(record.getStudentId(), "training_fee", record.getId(),
                        record.getAmount(),
                        "科目" + record.getSubject() + "二次培训费");
            } else {
                record.setPayStatus(0); // 无需缴费
                updateById(record);
            }
        } else {
            // 审核不通过
            record.setStatus(3); // 已取消
            record.setAuditTime(LocalDateTime.now());
            updateById(record);
        }
    }

    @Override
    @Transactional
    public void complete(Integer id) {
        RetakeTrainingRecord record = getById(id);
        if (record == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "二次培训记录不存在");
        }
        if (record.getStatus() != 1) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "只有培训中的记录可以标记完成");
        }
        // 非全包学员需确认已缴费（从 payment_record 表实时查询，不依赖本地 payStatus 字段）
        if (record.getIsFree() == 0) {
            long paidCount = paymentRecordMapper.selectCount(
                    new LambdaQueryWrapper<PaymentRecord>()
                            .eq(PaymentRecord::getBizType, "training_fee")
                            .eq(PaymentRecord::getBizId, record.getId())
                            .eq(PaymentRecord::getStatus, 1));
            if (paidCount == 0) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "培训费未缴纳，请先完成缴费");
            }
        }
        record.setStatus(2); // 已完成
        // 同步更新 payStatus（冗余字段，主要用于列表展示）
        if (record.getIsFree() == 0) {
            record.setPayStatus(2); // 已缴费
        }
        record.setCompleteTime(LocalDateTime.now());
        updateById(record);
    }

    @Override
    @Transactional
    public void cancel(Integer id) {
        RetakeTrainingRecord record = getById(id);
        if (record == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "二次培训记录不存在");
        }
        if (record.getStatus() == 2) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "已完成的培训无法取消");
        }
        record.setStatus(3); // 已取消
        updateById(record);
    }

    @Override
    public List<Map<String, Object>> listByStudent(Integer studentId) {
        List<RetakeTrainingRecord> list = lambdaQuery()
                .eq(RetakeTrainingRecord::getStudentId, studentId)
                .orderByDesc(RetakeTrainingRecord::getCreateTime)
                .list();
        return attachDetails(list);
    }

    @Override
    public List<Map<String, Object>> listByCoach(Integer coachId) {
        // 查出该教练名下的学员
        List<StudentCoach> bindings = studentCoachMapper.selectList(
                new LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getCoachId, coachId)
                        .eq(StudentCoach::getStatus, 1));
        if (bindings.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Integer> studentIds = bindings.stream()
                .map(StudentCoach::getStudentId)
                .collect(Collectors.toSet());

        List<RetakeTrainingRecord> list = lambdaQuery()
                .in(RetakeTrainingRecord::getStudentId, studentIds)
                .orderByDesc(RetakeTrainingRecord::getCreateTime)
                .list();
        return attachDetails(list);
    }

    @Override
    public Page<Map<String, Object>> pageAll(Page<?> page) {
        Page<RetakeTrainingRecord> rawPage = baseMapper.selectPage(
                new Page<>(page.getCurrent(), page.getSize()), null);
        List<Map<String, Object>> result = attachDetails(rawPage.getRecords());
        Page<Map<String, Object>> resultPage = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        resultPage.setRecords(result);
        return resultPage;
    }

    /** 将实体列表组装为带关联信息的 Map 列表 */
    private List<Map<String, Object>> attachDetails(List<RetakeTrainingRecord> records) {
        if (records.isEmpty()) return Collections.emptyList();

        // 批量加载学员姓名
        Set<Integer> studentIds = records.stream()
                .map(RetakeTrainingRecord::getStudentId)
                .collect(Collectors.toSet());
        Map<Integer, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        // 批量加载非全包学员的支付记录（用于实时展示缴费状态）
        Set<Integer> needPayIds = records.stream()
                .filter(r -> r.getIsFree() == 0 && r.getAmount() != null
                        && r.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(RetakeTrainingRecord::getId)
                .collect(Collectors.toSet());
        Map<Integer, Boolean> paidMap = new HashMap<>();
        if (!needPayIds.isEmpty()) {
            List<PaymentRecord> payments = paymentRecordMapper.selectList(
                    new LambdaQueryWrapper<PaymentRecord>()
                            .eq(PaymentRecord::getBizType, "training_fee")
                            .in(PaymentRecord::getBizId, needPayIds));
            payments.stream()
                    .filter(p -> p.getStatus() == 1)
                    .forEach(p -> paidMap.put(p.getBizId(), true));
        }

        return records.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", r.getId());
            map.put("studentId", r.getStudentId());
            User student = userMap.get(r.getStudentId());
            map.put("studentName", student != null ? student.getRealName() : null);
            map.put("examRegistrationId", r.getExamRegistrationId());
            map.put("subject", r.getSubject());
            map.put("subjectName", "科目" + r.getSubject());
            map.put("status", r.getStatus());
            String statusDesc = switch (r.getStatus()) {
                case 0 -> "待审核";
                case 1 -> "培训中";
                case 2 -> "已完成";
                case 3 -> "已取消";
                default -> "未知";
            };
            map.put("statusDesc", statusDesc);
            map.put("isFree", r.getIsFree());
            map.put("amount", r.getAmount());

            // 实时从 payment_record 表推导缴费状态
            Integer payStatus;
            if (r.getIsFree() == 1 || r.getAmount() == null
                    || r.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                payStatus = 0;
            } else if (paidMap.getOrDefault(r.getId(), false)) {
                payStatus = 2;
            } else {
                payStatus = 1;
            }
            map.put("payStatus", payStatus);
            String payStatusDesc = switch (payStatus) {
                case 0 -> "无需缴费";
                case 1 -> "待缴费";
                case 2 -> "已缴费";
                default -> "未知";
            };
            map.put("payStatusDesc", payStatusDesc);
            map.put("applyReason", r.getApplyReason());
            map.put("auditTime", r.getAuditTime());
            map.put("completeTime", r.getCompleteTime());
            map.put("createTime", r.getCreateTime());
            return map;
        }).collect(Collectors.toList());
    }
}
