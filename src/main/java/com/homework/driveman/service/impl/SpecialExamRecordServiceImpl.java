package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.SpecialExamRecord;
import com.homework.driveman.entity.User;
import com.homework.driveman.mapper.SpecialExamRecordMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.ISpecialExamRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 特种车辆考试记录业务实现 */
@Service
public class SpecialExamRecordServiceImpl extends ServiceImpl<SpecialExamRecordMapper, SpecialExamRecord>
        implements ISpecialExamRecordService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public Page<Map<String, Object>> pageWithDetails(Page<SpecialExamRecord> page,
                                                     String studentName,
                                                     String licenseType,
                                                     Integer subject,
                                                     Integer passStatus) {
        LambdaQueryWrapper<SpecialExamRecord> wrapper = new LambdaQueryWrapper<>();

        // 按学员姓名搜索
        if (studentName != null && !studentName.isEmpty()) {
            List<Integer> matchedUserIds = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .like(User::getRealName, studentName)
                            .select(User::getUserId)
            ).stream().map(User::getUserId).toList();
            if (matchedUserIds.isEmpty()) {
                return new Page<>(page.getCurrent(), page.getSize(), 0);
            }
            wrapper.in(SpecialExamRecord::getStudentId, matchedUserIds);
        }

        if (licenseType != null && !licenseType.isEmpty()) {
            wrapper.eq(SpecialExamRecord::getLicenseType, licenseType);
        }
        if (subject != null) {
            wrapper.eq(SpecialExamRecord::getSubject, subject);
        }
        if (passStatus != null) {
            wrapper.eq(SpecialExamRecord::getPassStatus, passStatus);
        }

        wrapper.orderByDesc(SpecialExamRecord::getExamDate);
        Page<SpecialExamRecord> rawPage = baseMapper.selectPage(page, wrapper);

        List<SpecialExamRecord> records = rawPage.getRecords();
        if (records.isEmpty()) {
            return new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        }

        // 批量加载学员姓名
        Set<Integer> studentIds = records.stream()
                .map(SpecialExamRecord::getStudentId)
                .collect(Collectors.toSet());
        Map<Integer, User> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        List<Map<String, Object>> resultList = records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("studentId", r.getStudentId());
            User u = userMap.get(r.getStudentId());
            m.put("studentName", u != null ? u.getRealName() : null);
            m.put("licenseType", r.getLicenseType());
            m.put("subject", r.getSubject());
            m.put("score", r.getScore());
            m.put("passStatus", r.getPassStatus());
            m.put("retakeCount", r.getRetakeCount());
            m.put("examDate", r.getExamDate());
            m.put("certNo", r.getCertNo());
            m.put("createTime", r.getCreateTime());
            return m;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> resultPage = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        resultPage.setRecords(resultList);
        return resultPage;
    }
}
