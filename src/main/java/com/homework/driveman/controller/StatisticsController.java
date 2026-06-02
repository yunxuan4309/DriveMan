package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.service.IStatisticsService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 统计报表控制器 — 返回 ECharts 兼容 JSON 数据
 */
@Tag(name = "统计报表")
@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Autowired
    private IStatisticsService statisticsService;

    @RequireRole(3)
    @Operation(summary = "报名趋势（折线图）", description = "近30天每日报名人数，返回 ECharts line 格式")
    @GetMapping("/registration-trend")
    public JsonResult<Map<String, Object>> getRegistrationTrend() {
        return JsonResult.ok(statisticsService.getRegistrationTrend());
    }

    @RequireRole(3)
    @Operation(summary = "考试合格率（饼图）", description = "所有科目的考试合格/不合格分布，返回 ECharts pie 格式")
    @GetMapping("/pass-rate")
    public JsonResult<Map<String, Object>> getPassRate() {
        return JsonResult.ok(statisticsService.getPassRate());
    }

    @RequireRole(3)
    @Operation(summary = "教练工作量（柱状图）", description = "各教练的已完成/已确认约课数量，返回 ECharts bar 格式")
    @GetMapping("/coach-workload")
    public JsonResult<Map<String, Object>> getCoachWorkload() {
        return JsonResult.ok(statisticsService.getCoachWorkload());
    }
}
