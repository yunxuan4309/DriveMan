package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.driveman.entity.*;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.FamiliarizationRecordMapper;
import com.homework.driveman.mapper.StudentCoachMapper;
import com.homework.driveman.service.*;
import com.homework.driveman.web.ServiceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 合场记录实现
 */
@Service
public class FamiliarizationRecordServiceImpl implements IFamiliarizationRecordService {

    @Autowired
    private FamiliarizationRecordMapper familiarizationRecordMapper;

    @Autowired
    private StudentCoachMapper studentCoachMapper;

    @Autowired
    private IUserService userService;

    @Autowired
    private IExamSessionService examSessionService;

    @Autowired
    private IFeeStandardService feeStandardService;

    @Autowired
    private IPaymentRecordService paymentRecordService;

    @Override
    @Transactional
    public FamiliarizationRecord apply(Integer studentId, Integer examSessionId, Integer carType) {
        // 1. 校验学员信息
        User student = userService.getById(studentId);
        if (student == null || student.getRole() != 1) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "学员不存在");
        }
        if (student.getStatus() != 1) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "学员尚未通过审核");
        }

        // 2. 校验考试场次
        ExamSession session = examSessionService.getById(examSessionId);
        if (session == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "考试场次不存在");
        }
        if (session.getRemainingQuota() <= 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该场次名额已满");
        }

        // 3. 校验车型匹配
        if (session.getLicenseType() != null && !session.getLicenseType().equals(student.getLicenseType())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    "该场次仅限 " + session.getLicenseType() + " 车型，您的车型为 " + student.getLicenseType());
        }

        // 4. 教练车模式 → 获取学员当前绑定的教练
        Integer coachId = null;
        if (carType == 1) {
            StudentCoach sc = studentCoachMapper.selectOne(
                    new LambdaQueryWrapper<StudentCoach>()
                            .eq(StudentCoach::getStudentId, studentId)
                            .eq(StudentCoach::getStatus, 1));
            if (sc == null) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "您当前没有绑定的教练，无法使用教练车合场");
            }
            coachId = sc.getCoachId();
        } else if (carType != 2) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "用车类型非法: 1-教练车, 2-考试车");
        }

        // 5. 查询 fee_standard 定价（按车型+科目+用车类型唯一匹配）
        String descTag = (carType == 1) ? "合场(教练车)" : "合场(考试车)";
        FeeStandard fee = feeStandardService.lambdaQuery()
                .eq(FeeStandard::getLicenseType, student.getLicenseType())
                .eq(FeeStandard::getSubject, session.getSubject())
                .eq(FeeStandard::getDescription, descTag)
                .one();
        if (fee == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND,
                    "未配置 " + student.getLicenseType() + " 科目" + session.getSubject()
                            + " 的合场费用标准（" + descTag + "），请联系管理员");
        }

        // 6. 创建合场记录（待支付）
        FamiliarizationRecord record = new FamiliarizationRecord();
        record.setStudentId(studentId);
        record.setExamSessionId(examSessionId);
        record.setSubject(session.getSubject());
        record.setCarType(carType);
        record.setCoachId(coachId);
        record.setAmount(fee.getAmount());
        record.setStatus(0);
        familiarizationRecordMapper.insert(record);

        // 7. 自动生成支付记录并回填 ID
        PaymentRecord payment = paymentRecordService.autoCreate(studentId, "familiarization_fee",
                record.getId(), fee.getAmount(),
                student.getLicenseType() + " 科目" + session.getSubject() + " " + descTag);
        record.setPaymentRecordId(payment.getId());
        familiarizationRecordMapper.updateById(record);

        return record;
    }

    @Override
    @Transactional
    public FamiliarizationRecord pay(Integer id, Integer studentId) {
        FamiliarizationRecord record = familiarizationRecordMapper.selectById(id);
        if (record == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "合场记录不存在");
        }
        if (!record.getStudentId().equals(studentId)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "只能支付自己的合场记录");
        }
        if (record.getStatus() != 0) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "当前状态不允许支付");
        }

        // 先支付关联的支付记录
        paymentRecordService.pay(record.getPaymentRecordId());

        // 再更新合场记录状态
        record.setStatus(1);
        record.setPayTime(LocalDateTime.now());
        familiarizationRecordMapper.updateById(record);
        return record;
    }

    @Override
    @Transactional
    public FamiliarizationRecord schedule(Integer id, String scheduledTime) {
        FamiliarizationRecord record = familiarizationRecordMapper.selectById(id);
        if (record == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "合场记录不存在");
        }
        if (record.getStatus() != 1) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "仅已支付的合场可以安排时间");
        }
        record.setScheduledTime(LocalDateTime.parse(scheduledTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        familiarizationRecordMapper.updateById(record);
        return record;
    }

    @Override
    @Transactional
    public FamiliarizationRecord complete(Integer id) {
        FamiliarizationRecord record = familiarizationRecordMapper.selectById(id);
        if (record == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "合场记录不存在");
        }
        if (record.getStatus() != 1) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "仅已支付的合场可以标记完成");
        }
        record.setStatus(2);
        familiarizationRecordMapper.updateById(record);
        return record;
    }

    @Override
    @Transactional
    public FamiliarizationRecord cancel(Integer id) {
        FamiliarizationRecord record = familiarizationRecordMapper.selectById(id);
        if (record == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "合场记录不存在");
        }
        if (record.getStatus() == 2) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "已完成的合场不能取消");
        }
        record.setStatus(3);
        familiarizationRecordMapper.updateById(record);
        return record;
    }

    @Override
    public List<Map<String, Object>> listMyRecords(Integer studentId) {
        return familiarizationRecordMapper.selectListWithDetails().stream()
                .filter(m -> m.get("student_id") != null
                        && ((Number) m.get("student_id")).intValue() == studentId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> listAll() {
        return familiarizationRecordMapper.selectListWithDetails();
    }
}
