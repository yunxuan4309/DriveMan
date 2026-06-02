package com.homework.driveman.service;

import java.util.Map;

/**
 * 统计报表服务 — 返回 ECharts 兼容的 JSON 数据
 */
public interface IStatisticsService {

    /** 报名趋势折线图数据 */
    Map<String, Object> getRegistrationTrend();

    /** 考试合格率饼图数据 */
    Map<String, Object> getPassRate();

    /** 教练工作量柱状图数据 */
    Map<String, Object> getCoachWorkload();
}
