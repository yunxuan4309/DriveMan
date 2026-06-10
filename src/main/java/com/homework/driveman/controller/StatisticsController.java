package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.service.IStatisticsService;
import com.homework.driveman.util.ExcelExportUtil;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
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
    @Operation(summary = "报名趋势（折线图）",
            description = "支持自定义日期范围，不传参数时默认近30天。返回 ECharts line 格式")
    @GetMapping("/registration-trend")
    public JsonResult<Map<String, Object>> getRegistrationTrend(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "开始日期，格式 yyyy-MM-dd，默认30天前") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd")
            @Parameter(description = "结束日期，格式 yyyy-MM-dd，默认今天") LocalDate endDate) {
        return JsonResult.ok(statisticsService.getRegistrationTrend(startDate, endDate));
    }

    @RequireRole(3)
    @Operation(summary = "各科目月度通过率趋势（折线图）",
            description = "支持按年份和科目筛选，不传参数时查全部。返回 ECharts 多 line 格式")
    @GetMapping("/pass-rate")
    public JsonResult<Map<String, Object>> getPassRate(
            @RequestParam(required = false) @Parameter(description = "年份，如 2026") Integer year,
            @RequestParam(required = false) @Parameter(description = "科目：1-4") Integer subject) {
        return JsonResult.ok(statisticsService.getPassRate(year, subject));
    }

    @RequireRole(3)
    @Operation(summary = "教练效能排名（柱状图）",
            description = "支持按车型和排名数量筛选，不传参数时查全部教练。返回 ECharts bar 格式 + detailData")
    @GetMapping("/coach-workload")
    public JsonResult<Map<String, Object>> getCoachWorkload(
            @RequestParam(required = false) @Parameter(description = "准教车型，如 C1") String licenseType,
            @RequestParam(required = false) @Parameter(description = "返回前 N 名，如 5") Integer topN) {
        return JsonResult.ok(statisticsService.getCoachWorkload(licenseType, topN));
    }

    @RequireRole(3)
    @Operation(summary = "收入看板",
            description = "支持按年份筛选，不传参数时近12月+当月。返回月度趋势柱状图 + 收入来源饼图 + 收支汇总")
    @GetMapping("/revenue-summary")
    public JsonResult<Map<String, Object>> getRevenueSummary(
            @RequestParam(required = false) @Parameter(description = "年份，如 2026") Integer year) {
        return JsonResult.ok(statisticsService.getRevenueSummary(year));
    }

    // ==================== Excel 导出 ====================

    @RequireRole(3)
    @Operation(summary = "导出报名趋势Excel")
    @PostMapping("/registration-trend/export-excel")
    public void exportRegistrationTrend(HttpServletResponse response) throws IOException {
        Map<String, Object> data = statisticsService.getRegistrationTrend(null, null);
        Workbook wb = ExcelExportUtil.exportRegistrationTrend(data);
        ExcelExportUtil.writeToResponse(wb, "报名趋势.xlsx", response);
    }

    @RequireRole(3)
    @Operation(summary = "导出通过率趋势Excel")
    @PostMapping("/pass-rate/export-excel")
    public void exportPassRate(HttpServletResponse response) throws IOException {
        Map<String, Object> data = statisticsService.getPassRate(null, null);
        Workbook wb = ExcelExportUtil.exportPassRate(data);
        ExcelExportUtil.writeToResponse(wb, "各科通过率.xlsx", response);
    }

    @RequireRole(3)
    @Operation(summary = "导出教练效能排名Excel")
    @PostMapping("/coach-workload/export-excel")
    public void exportCoachWorkload(HttpServletResponse response) throws IOException {
        Map<String, Object> data = statisticsService.getCoachWorkload(null, null);
        Workbook wb = ExcelExportUtil.exportCoachWorkload(data);
        ExcelExportUtil.writeToResponse(wb, "教练效能排名.xlsx", response);
    }

    @RequireRole(3)
    @Operation(summary = "导出收入看板Excel")
    @PostMapping("/revenue-summary/export-excel")
    public void exportRevenueSummary(HttpServletResponse response) throws IOException {
        Map<String, Object> data = statisticsService.getRevenueSummary(null);
        Workbook wb = ExcelExportUtil.exportRevenueSummary(data);
        ExcelExportUtil.writeToResponse(wb, "收入看板.xlsx", response);
    }
}
