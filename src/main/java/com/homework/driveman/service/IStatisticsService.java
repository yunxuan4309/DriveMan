package com.homework.driveman.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * 统计报表服务 — 返回 ECharts 兼容的 JSON 数据
 */
public interface IStatisticsService {

    /** 报名趋势折线图数据 */
    Map<String, Object> getRegistrationTrend(LocalDate startDate, LocalDate endDate);

    /** 考试合格率趋势 */
    Map<String, Object> getPassRate(Integer year, Integer subject);

    /** 教练工作量柱状图数据 */
    Map<String, Object> getCoachWorkload(String licenseType, Integer topN);

    /** 收入看板（月度趋势 + 来源分布 + 汇总） */
    Map<String, Object> getRevenueSummary(Integer year);
}
