package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.homework.driveman.entity.ExamRegistration;
import com.homework.driveman.entity.User;
import com.homework.driveman.mapper.AppointmentMapper;
import com.homework.driveman.mapper.ExamRegistrationMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.IStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计报表实现 — 基于 SQL 聚合查询，返回 ECharts 标准格式
 */
@Service
public class StatisticsServiceImpl implements IStatisticsService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ExamRegistrationMapper examRegistrationMapper;

    @Autowired
    private AppointmentMapper appointmentMapper;

    @Override
    public Map<String, Object> getRegistrationTrend() {
        // 查询最近 30 天每天的学员报名数
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.select("DATE(create_time) AS date", "COUNT(*) AS count")
                .eq("role", 1)
                .eq("is_deleted", 0)
                .ge("create_time", thirtyDaysAgo)
                .groupBy("DATE(create_time)")
                .orderByAsc("DATE(create_time)");
        List<Map<String, Object>> rows = userMapper.selectMaps(wrapper);

        // 填充完整 30 天（无数据的日期补 0）
        Set<String> dateSet = rows.stream()
                .map(r -> r.get("date").toString())
                .collect(Collectors.toSet());

        List<String> categories = new ArrayList<>();
        List<Integer> values = new ArrayList<>();

        for (int i = 29; i >= 0; i--) {
            String date = thirtyDaysAgo.plusDays(i).toString();
            categories.add(date);
            if (dateSet.contains(date)) {
                rows.stream()
                        .filter(r -> r.get("date").toString().equals(date))
                        .findFirst()
                        .ifPresent(r -> values.add(((Number) r.get("count")).intValue()));
            } else {
                values.add(0);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", Map.of("text", "近30天报名趋势"));
        result.put("xAxis", Map.of("type", "category", "data", categories));
        result.put("yAxis", Map.of("type", "value", "minInterval", 1));
        result.put("series", List.of(Map.of(
                "name", "报名人数",
                "type", "line",
                "smooth", true,
                "data", values
        )));
        return result;
    }

    @Override
    public Map<String, Object> getPassRate() {
        // 统计考试合格与不合格人数
        QueryWrapper<ExamRegistration> wrapper = new QueryWrapper<>();
        wrapper.select("pass_status", "COUNT(*) AS count")
                .eq("is_deleted", 0)
                .isNotNull("pass_status")
                .groupBy("pass_status");
        List<Map<String, Object>> rows = examRegistrationMapper.selectMaps(wrapper);

        List<Map<String, Object>> pieData = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            int passStatus = ((Number) row.get("pass_status")).intValue();
            String name = passStatus == 1 ? "合格" : "不合格";
            int count = ((Number) row.get("count")).intValue();
            pieData.add(Map.of("name", name, "value", count));
        }

        // 无数据时显示占位
        if (pieData.isEmpty()) {
            pieData.add(Map.of("name", "暂无数据", "value", 1));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", Map.of("text", "考试合格率", "left", "center"));
        result.put("tooltip", Map.of("trigger", "item", "formatter", "{b}: {c} ({d}%)"));
        result.put("legend", Map.of("orient", "vertical", "left", "left"));
        result.put("series", List.of(Map.of(
                "name", "考试结果",
                "type", "pie",
                "radius", "50%",
                "data", pieData
        )));
        return result;
    }

    @Override
    public Map<String, Object> getCoachWorkload() {
        List<Map<String, Object>> rows = appointmentMapper.countAppointmentsPerCoach();

        List<String> names = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            names.add(row.get("name").toString());
            values.add(((Number) row.get("value")).intValue());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", Map.of("text", "教练工作量统计"));
        result.put("xAxis", Map.of("type", "category", "data", names));
        result.put("yAxis", Map.of("type", "value", "minInterval", 1));
        result.put("series", List.of(Map.of(
                "name", "约课数",
                "type", "bar",
                "data", values
        )));
        return result;
    }
}
