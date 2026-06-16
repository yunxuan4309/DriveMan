package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.SpecialPersonRecord;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.SpecialPersonRecordMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.ISpecialPersonRecordService;
import com.homework.driveman.web.ServiceCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 特殊人群记录服务实现
 */
@Slf4j
@Service
public class SpecialPersonRecordServiceImpl extends ServiceImpl<SpecialPersonRecordMapper, SpecialPersonRecord> implements ISpecialPersonRecordService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public SpecialPersonRecord submit(Integer userId, Integer recordType, LocalDate recordDate,
                                      Integer banYears, String courtDocNo, Integer courtDocFileId) {
        // 校验参数
        if (recordType == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "记录类型不能为空");
        }
        if (recordDate == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "违法/犯罪日期不能为空");
        }
        if (courtDocNo == null || courtDocNo.isEmpty()) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "法律文书编号不能为空");
        }
        if (courtDocFileId == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "请上传法律文书扫描件");
        }

        // 计算禁驾截止日期
        LocalDate banEndDate = null;
        if (banYears != null && banYears > 0) {
            banEndDate = recordDate.plusYears(banYears);
        }
        // banYears为null表示终生禁驾，banEndDate保持null

        SpecialPersonRecord record = new SpecialPersonRecord();
        record.setUserId(userId);
        record.setRecordType(recordType);
        record.setRecordDate(recordDate);
        record.setBanYears(banYears);
        record.setBanEndDate(banEndDate);
        record.setCourtDocNo(courtDocNo);
        record.setCourtDocFileId(courtDocFileId);
        record.setAuditStatus(0); // 待审核
        save(record);

        log.info("特殊人群记录提交成功: userId={}, recordType={}", userId, recordType);
        return record;
    }

    @Override
    public List<SpecialPersonRecord> listByUserId(Integer userId) {
        return lambdaQuery()
                .eq(SpecialPersonRecord::getUserId, userId)
                .orderByDesc(SpecialPersonRecord::getCreateTime)
                .list();
    }

    @Override
    public void audit(Integer id, Integer auditStatus, String auditRemark, Integer auditUserId) {
        if (auditStatus == null || (auditStatus != 1 && auditStatus != 2)) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "审核状态只能为 1(通过) 或 2(不通过)");
        }
        SpecialPersonRecord record = getById(id);
        if (record == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "记录不存在");
        }
        if (record.getAuditStatus() != 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该记录已审核，请勿重复操作");
        }

        record.setAuditStatus(auditStatus);
        record.setAuditRemark(auditRemark);
        record.setAuditTime(LocalDateTime.now());
        record.setAuditUserId(auditUserId);
        updateById(record);

        log.info("特殊人群记录审核完成: id={}, auditStatus={}", id, auditStatus);
    }

    @Override
    public List<SpecialPersonRecord> listPending() {
        return lambdaQuery()
                .eq(SpecialPersonRecord::getAuditStatus, 0)
                .orderByDesc(SpecialPersonRecord::getCreateTime)
                .list();
    }

    @Override
    public boolean isInBanPeriod(Integer userId) {
        LocalDate banEnd = getBanEndDate(userId);
        if (banEnd == null) {
            // 有审核通过的特殊记录但banEndDate为null，表示终生禁驾
            boolean hasPassedRecord = lambdaQuery()
                    .eq(SpecialPersonRecord::getUserId, userId)
                    .eq(SpecialPersonRecord::getAuditStatus, 1)
                    .isNull(SpecialPersonRecord::getBanEndDate)
                    .exists();
            return hasPassedRecord;
        }
        // banEndDate不为null，比较是否已过禁驾期
        return !banEnd.isBefore(LocalDate.now());
    }

    @Override
    public LocalDate getBanEndDate(Integer userId) {
        List<SpecialPersonRecord> records = lambdaQuery()
                .eq(SpecialPersonRecord::getUserId, userId)
                .eq(SpecialPersonRecord::getAuditStatus, 1)
                .list();

        if (records.isEmpty()) {
            return null; // 无记录，无限制
        }

        // 找出最晚的禁驾截止日期
        // null表示终生禁驾，优先级最高
        for (SpecialPersonRecord record : records) {
            if (record.getBanEndDate() == null) {
                return LocalDate.MAX; // 终生禁驾
            }
        }

        return records.stream()
                .map(SpecialPersonRecord::getBanEndDate)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    @Override
    public Page<Map<String, Object>> pageWithDetails(Page<SpecialPersonRecord> page, Integer auditStatus,
                                                      Integer recordType, String keyword) {
        LambdaQueryWrapper<SpecialPersonRecord> wrapper = new LambdaQueryWrapper<SpecialPersonRecord>()
                .eq(auditStatus != null, SpecialPersonRecord::getAuditStatus, auditStatus)
                .eq(recordType != null, SpecialPersonRecord::getRecordType, recordType)
                .orderByDesc(SpecialPersonRecord::getCreateTime);

        // 按学员姓名搜索
        if (keyword != null && !keyword.isEmpty()) {
            List<Integer> matchedUserIds = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .like(User::getRealName, keyword)
                            .select(User::getUserId)
            ).stream().map(User::getUserId).collect(Collectors.toList());
            if (matchedUserIds.isEmpty()) {
                Page<Map<String, Object>> empty = new Page<>(page.getCurrent(), page.getSize(), 0);
                empty.setRecords(Collections.emptyList());
                return empty;
            }
            wrapper.in(SpecialPersonRecord::getUserId, matchedUserIds);
        }

        Page<SpecialPersonRecord> rawPage = baseMapper.selectPage(page, wrapper);
        List<SpecialPersonRecord> records = rawPage.getRecords();
        if (records.isEmpty()) {
            Page<Map<String, Object>> empty = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        // 批量加载学员姓名
        Set<Integer> userIds = records.stream().map(SpecialPersonRecord::getUserId).collect(Collectors.toSet());
        Map<Integer, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        List<Map<String, Object>> enriched = records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("userId", r.getUserId());
            User u = userMap.get(r.getUserId());
            m.put("studentName", u != null ? u.getRealName() : null);
            m.put("studentPhone", u != null ? u.getPhone() : null);
            m.put("recordType", r.getRecordType());
            m.put("recordDate", r.getRecordDate());
            m.put("banYears", r.getBanYears());
            m.put("banEndDate", r.getBanEndDate());
            m.put("courtDocNo", r.getCourtDocNo());
            m.put("courtDocFileId", r.getCourtDocFileId());
            m.put("auditStatus", r.getAuditStatus());
            m.put("auditRemark", r.getAuditRemark());
            m.put("auditTime", r.getAuditTime());
            m.put("createTime", r.getCreateTime());
            return m;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> result = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        result.setRecords(enriched);
        return result;
    }
}
