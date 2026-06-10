package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.CoachApplication;
import com.homework.driveman.entity.User;
import com.homework.driveman.mapper.CoachApplicationMapper;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.ICoachApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 教练申请审核业务实现 */
@Service
public class CoachApplicationServiceImpl extends ServiceImpl<CoachApplicationMapper, CoachApplication>
        implements ICoachApplicationService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CoachMapper coachMapper;

    @Override
    public Page<Map<String, Object>> pageWithDetails(Page<CoachApplication> page, Integer status, String studentName) {
        LambdaQueryWrapper<CoachApplication> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(CoachApplication::getStatus, status);
        }

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
            wrapper.in(CoachApplication::getStudentId, matchedUserIds);
        }

        wrapper.orderByDesc(CoachApplication::getCreateTime);
        Page<CoachApplication> rawPage = baseMapper.selectPage(page, wrapper);

        // 如果没有数据，直接返回空页
        List<CoachApplication> records = rawPage.getRecords();
        if (records.isEmpty()) {
            return new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        }

        // 批量加载学员信息
        Set<Integer> studentIds = records.stream()
                .map(CoachApplication::getStudentId)
                .collect(Collectors.toSet());
        Map<Integer, User> studentMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        // 批量加载教练信息
        Set<Integer> coachIds = records.stream()
                .map(CoachApplication::getCoachId)
                .collect(Collectors.toSet());
        Map<Integer, Coach> coachMap = coachMapper.selectBatchIds(coachIds).stream()
                .collect(Collectors.toMap(Coach::getCoachId, c -> c, (a, b) -> a));

        // 批量加载源教练信息（移交场景）
        Set<Integer> sourceCoachIds = records.stream()
                .map(CoachApplication::getSourceCoachId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Integer, Coach> sourceCoachMap = sourceCoachIds.isEmpty() ? Map.of()
                : coachMapper.selectBatchIds(sourceCoachIds).stream()
                .collect(Collectors.toMap(Coach::getCoachId, c -> c, (a, b) -> a));

        // 批量加载所有关联的 user 信息（教练名需要从 user 表取）
        Set<Integer> allUserIds = java.util.HashSet.newHashSet(16);
        // 学员 user ID 已在 studentMap 中
        for (Coach coach : coachMap.values()) {
            if (coach.getUserId() != null) allUserIds.add(coach.getUserId());
        }
        for (Coach coach : sourceCoachMap.values()) {
            if (coach.getUserId() != null) allUserIds.add(coach.getUserId());
        }
        Map<Integer, User> userMap = allUserIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(allUserIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        // 组装
        List<Map<String, Object>> result = records.stream().map(app -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", app.getId());
            map.put("studentId", app.getStudentId());
            map.put("coachId", app.getCoachId());
            map.put("sourceCoachId", app.getSourceCoachId());
            map.put("transferReason", app.getTransferReason());
            map.put("status", app.getStatus());
            map.put("applyTime", app.getApplyTime());
            map.put("auditTime", app.getAuditTime());
            map.put("auditReason", app.getAuditReason());

            // 学员姓名
            User student = studentMap.get(app.getStudentId());
            map.put("studentName", student != null ? student.getRealName() : "未知");

            // 目标教练姓名
            Coach targetCoach = coachMap.get(app.getCoachId());
            if (targetCoach != null) {
                User targetUser = userMap.get(targetCoach.getUserId());
                map.put("coachName", targetUser != null ? targetUser.getRealName() : "未知");
            } else {
                map.put("coachName", "未知");
            }

            // 源教练姓名（移交场景）
            if (app.getSourceCoachId() != null) {
                Coach sourceCoach = sourceCoachMap.get(app.getSourceCoachId());
                if (sourceCoach != null) {
                    User sourceUser = userMap.get(sourceCoach.getUserId());
                    map.put("sourceCoachName", sourceUser != null ? sourceUser.getRealName() : "未知");
                } else {
                    map.put("sourceCoachName", "未知");
                }
                map.put("applyType", "教练移交");
            } else {
                map.put("sourceCoachName", null);
                map.put("applyType", "学员申请");
            }

            return map;
        }).collect(Collectors.toList());

        Page<Map<String, Object>> resultPage = new Page<>(rawPage.getCurrent(), rawPage.getSize(), rawPage.getTotal());
        resultPage.setRecords(result);
        return resultPage;
    }
}
