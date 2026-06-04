package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.homework.driveman.entity.User;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.ExamRegistrationMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.IPaymentRecordService;
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
    private CoachMapper coachMapper;

    @Autowired
    private IPaymentRecordService paymentRecordService;

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
        // 各科目月度考试通过率趋势
        List<Map<String, Object>> rows = examRegistrationMapper.selectMonthlyPassRate();

        // 提取所有月份（去重排序）
        Set<String> monthSet = new LinkedHashSet<>();
        // 提取所有科目（去重）
        Set<Integer> subjectSet = new TreeSet<>();
        // 按 (month, subject) 索引 pass_rate
        Map<String, Map<Integer, Double>> dataMap = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            String month = row.get("month").toString();
            int subject = ((Number) row.get("subject")).intValue();
            double passRate = ((Number) row.get("pass_rate")).doubleValue();

            monthSet.add(month);
            subjectSet.add(subject);
            dataMap.computeIfAbsent(month, k -> new HashMap<>()).put(subject, passRate);
        }

        List<String> months = new ArrayList<>(monthSet);
        // 科目名称映射
        Map<Integer, String> subjectNames = Map.of(
                1, "科目一", 2, "科目二", 3, "科目三", 4, "科目四"
        );

        // 构建 ECharts 多折线图
        List<Map<String, Object>> seriesList = new ArrayList<>();
        Map<String, List<Object>> legendMap = new LinkedHashMap<>();

        for (int subject : subjectSet) {
            List<Object> rates = new ArrayList<>();
            for (String month : months) {
                Map<Integer, Double> monthData = dataMap.get(month);
                if (monthData != null && monthData.containsKey(subject)) {
                    rates.add(monthData.get(subject));
                } else {
                    rates.add(null); // 无数据月份留空
                }
            }
            String name = subjectNames.getOrDefault(subject, "科目" + subject);
            seriesList.add(Map.of(
                    "name", name,
                    "type", "line",
                    "connectNulls", false,
                    "data", rates
            ));
            legendMap.put(name, rates);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", Map.of("text", "各科目月度考试通过率趋势"));
        result.put("tooltip", Map.of("trigger", "axis"));
        result.put("legend", Map.of("data", new ArrayList<>(legendMap.keySet())));
        result.put("xAxis", Map.of("type", "category", "data", months));
        result.put("yAxis", Map.of("type", "value", "min", 0, "max", 100, "name", "通过率(%)"));
        result.put("series", seriesList);
        return result;
    }

    @Override
    public Map<String, Object> getCoachWorkload() {
        // 教练效能：考试通过率排名、评分、带教学员数、执教年限
        List<Map<String, Object>> rows = coachMapper.selectCoachEffectiveness();

        List<String> names = new ArrayList<>();
        List<Double> passRates = new ArrayList<>();
        List<Map<String, Object>> detailList = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            String coachName = row.get("coach_name").toString();
            names.add(coachName);

            Number passRate = (Number) row.get("pass_rate");
            double rate = passRate != null ? passRate.doubleValue() : 0.0;
            passRates.add(rate);

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("coachName", coachName);
            detail.put("rating", row.get("rating"));
            detail.put("coachYears", row.get("coach_years"));
            detail.put("studentCount", row.get("student_count"));
            detail.put("examCount", row.get("exam_count"));
            detail.put("passCount", row.get("pass_count"));
            detail.put("passRate", rate);
            detailList.add(detail);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", Map.of("text", "教练效能排名"));
        result.put("tooltip", Map.of("trigger", "axis"));
        result.put("xAxis", Map.of("type", "category", "data", names));
        result.put("yAxis", Map.of("type", "value", "min", 0, "max", 100, "name", "通过率(%)"));
        result.put("series", List.of(Map.of(
                "name", "考试通过率(%)",
                "type", "bar",
                "data", passRates
        )));
        // 额外明细，供前端表格展示
        result.put("detailData", detailList);
        return result;
    }

    @Override
    public Map<String, Object> getRevenueSummary() {
        return paymentRecordService.getRevenueSummary();
    }
}
