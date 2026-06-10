package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.service.IStatisticsService;
import com.homework.driveman.util.ExcelExportUtil;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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
    @Operation(summary = "各科目月度通过率趋势（折线图）", description = "按科目拆分、按月展示考试通过率变化趋势，返回 ECharts 多 line 格式")
    @GetMapping("/pass-rate")
    public JsonResult<Map<String, Object>> getPassRate() {
        return JsonResult.ok(statisticsService.getPassRate());
    }

    @RequireRole(3)
    @Operation(summary = "教练效能排名（柱状图）", description = "按学员考试通过率排名，含评分/带教学员数/执教年限明细，返回 ECharts bar 格式 + detailData")
    @GetMapping("/coach-workload")
    public JsonResult<Map<String, Object>> getCoachWorkload() {
        return JsonResult.ok(statisticsService.getCoachWorkload());
    }

    @RequireRole(3)
    @Operation(summary = "收入看板", description = "月度收入趋势柱状图 + 当月收入来源饼图 + 收支汇总")
    @GetMapping("/revenue-summary")
    public JsonResult<Map<String, Object>> getRevenueSummary() {
        return JsonResult.ok(statisticsService.getRevenueSummary());
    }

    // ==================== Excel 导出 ====================

    @RequireRole(3)
    @Operation(summary = "导出报名趋势Excel")
    @PostMapping("/registration-trend/export-excel")
    public void exportRegistrationTrend(HttpServletResponse response) throws IOException {
        Map<String, Object> data = statisticsService.getRegistrationTrend();
        Workbook wb = ExcelExportUtil.exportRegistrationTrend(data);
        ExcelExportUtil.writeToResponse(wb, "报名趋势.xlsx", response);
    }

    @RequireRole(3)
    @Operation(summary = "导出通过率趋势Excel")
    @PostMapping("/pass-rate/export-excel")
    public void exportPassRate(HttpServletResponse response) throws IOException {
        Map<String, Object> data = statisticsService.getPassRate();
        Workbook wb = ExcelExportUtil.exportPassRate(data);
        ExcelExportUtil.writeToResponse(wb, "各科通过率.xlsx", response);
    }

    @RequireRole(3)
    @Operation(summary = "导出教练效能排名Excel")
    @PostMapping("/coach-workload/export-excel")
    public void exportCoachWorkload(HttpServletResponse response) throws IOException {
        Map<String, Object> data = statisticsService.getCoachWorkload();
        Workbook wb = ExcelExportUtil.exportCoachWorkload(data);
        ExcelExportUtil.writeToResponse(wb, "教练效能排名.xlsx", response);
    }

    @RequireRole(3)
    @Operation(summary = "导出收入看板Excel")
    @PostMapping("/revenue-summary/export-excel")
    public void exportRevenueSummary(HttpServletResponse response) throws IOException {
        Map<String, Object> data = statisticsService.getRevenueSummary();
        Workbook wb = ExcelExportUtil.exportRevenueSummary(data);
        ExcelExportUtil.writeToResponse(wb, "收入看板.xlsx", response);
    }
}
