package com.homework.driveman.util;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Excel 导出工具 — 将统计报表数据转为 .xlsx 文件流
 */
public class ExcelExportUtil {

    /** 将 Workbook 写入 HttpServletResponse 供前端下载 */
    public static void writeToResponse(Workbook workbook, String fileName,
                                       HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + encoded);
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    // ==================== 报名趋势 ====================

    @SuppressWarnings("unchecked")
    public static Workbook exportRegistrationTrend(Map<String, Object> data) {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("报名趋势");

        List<String> categories = (List<String>) data.get("categories");
        List<Map<String, Object>> series = (List<Map<String, Object>>) data.get("series");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("日期");
        String seriesName = series != null && !series.isEmpty()
                ? String.valueOf(series.get(0).getOrDefault("name", "报名人数"))
                : "报名人数";
        header.createCell(1).setCellValue(seriesName);
        styleHeader(wb, header);

        if (categories != null && series != null && !series.isEmpty()) {
            List<Integer> values = (List<Integer>) series.get(0).get("data");
            for (int i = 0; i < categories.size(); i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(categories.get(i));
                row.createCell(1).setCellValue(values != null && i < values.size() ? values.get(i) : 0);
            }
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        return wb;
    }

    // ==================== 通过率趋势 ====================

    @SuppressWarnings("unchecked")
    public static Workbook exportPassRate(Map<String, Object> data) {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("各科通过率");

        List<String> categories = (List<String>) data.get("categories");
        List<Map<String, Object>> series = (List<Map<String, Object>>) data.get("series");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("月份");
        if (series != null) {
            for (int i = 0; i < series.size(); i++) {
                header.createCell(i + 1).setCellValue(String.valueOf(series.get(i).getOrDefault("name", "")));
            }
        }
        styleHeader(wb, header);

        if (categories != null && series != null) {
            for (int i = 0; i < categories.size(); i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(categories.get(i));
                for (int s = 0; s < series.size(); s++) {
                    List<Number> vals = (List<Number>) series.get(s).get("data");
                    row.createCell(s + 1).setCellValue(vals != null && i < vals.size() ? vals.get(i).doubleValue() : 0);
                }
            }
        }

        for (int i = 0; i <= (series != null ? series.size() : 0); i++) {
            sheet.autoSizeColumn(i);
        }
        return wb;
    }

    // ==================== 教练效能排名 ====================

    @SuppressWarnings("unchecked")
    public static Workbook exportCoachWorkload(Map<String, Object> data) {
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("教练效能排名");

        Row header = sheet.createRow(0);
        String[] cols = {"教练姓名", "通过率(%)", "综合评分", "执教年限", "带教学员数", "考试人次", "通过人次"};
        for (int i = 0; i < cols.length; i++) {
            header.createCell(i).setCellValue(cols[i]);
        }
        styleHeader(wb, header);

        List<Map<String, Object>> detailData = (List<Map<String, Object>>) data.get("detailData");
        if (detailData != null) {
            for (int i = 0; i < detailData.size(); i++) {
                Map<String, Object> d = detailData.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(String.valueOf(d.getOrDefault("coachName", "")));
                row.createCell(1).setCellValue(toDouble(d.get("passRate")));
                row.createCell(2).setCellValue(toDouble(d.get("rating")));
                row.createCell(3).setCellValue(toInt(d.get("coachYears")));
                row.createCell(4).setCellValue(toInt(d.get("studentCount")));
                row.createCell(5).setCellValue(toInt(d.get("examCount")));
                row.createCell(6).setCellValue(toInt(d.get("passCount")));
            }
        }

        for (int i = 0; i < cols.length; i++) {
            sheet.autoSizeColumn(i);
        }
        return wb;
    }

    // ==================== 收入看板 ====================

    @SuppressWarnings("unchecked")
    public static Workbook exportRevenueSummary(Map<String, Object> data) {
        XSSFWorkbook wb = new XSSFWorkbook();

        // 解析 nested 结构
        Map<String, Object> monthlyChart = (Map<String, Object>) data.get("monthlyChart");
        Map<String, Object> bizTypeChart = (Map<String, Object>) data.get("bizTypeChart");
        Map<String, Object> summary = (Map<String, Object>) data.get("summary");

        // Sheet 1 — 月度收入趋势
        Sheet trendSheet = wb.createSheet("月度收入趋势");
        Row header = trendSheet.createRow(0);
        header.createCell(0).setCellValue("月份");
        header.createCell(1).setCellValue("收入(元)");
        styleHeader(wb, header);

        if (monthlyChart != null) {
            Map<String, Object> xAxis = (Map<String, Object>) monthlyChart.get("xAxis");
            List<String> months = xAxis != null ? (List<String>) xAxis.get("data") : null;
            List<Map<String, Object>> series = (List<Map<String, Object>>) monthlyChart.get("series");
            List<Number> values = series != null && !series.isEmpty()
                    ? (List<Number>) series.get(0).get("data") : null;

            if (months != null) {
                for (int i = 0; i < months.size(); i++) {
                    Row row = trendSheet.createRow(i + 1);
                    row.createCell(0).setCellValue(months.get(i));
                    row.createCell(1).setCellValue(values != null && i < values.size() ? values.get(i).doubleValue() : 0);
                }
            }
        }

        // Sheet 2 — 收入来源分布
        Sheet pieSheet = wb.createSheet("收入来源分布");
        Row pieHeader = pieSheet.createRow(0);
        pieHeader.createCell(0).setCellValue("来源");
        pieHeader.createCell(1).setCellValue("金额(元)");
        styleHeader(wb, pieHeader);

        if (bizTypeChart != null) {
            List<Map<String, Object>> bzSeries = (List<Map<String, Object>>) bizTypeChart.get("series");
            if (bzSeries != null && !bzSeries.isEmpty()) {
                List<Map<String, Object>> pieData = (List<Map<String, Object>>) bzSeries.get(0).get("data");
                if (pieData != null) {
                    for (int i = 0; i < pieData.size(); i++) {
                        Row row = pieSheet.createRow(i + 1);
                        row.createCell(0).setCellValue(String.valueOf(pieData.get(i).getOrDefault("name", "")));
                        row.createCell(1).setCellValue(toDouble(pieData.get(i).get("value")));
                    }
                }
            }
        }

        // Sheet 3 — 收支汇总
        Sheet summarySheet = wb.createSheet("收支汇总");
        Row sumHeader = summarySheet.createRow(0);
        sumHeader.createCell(0).setCellValue("指标");
        sumHeader.createCell(1).setCellValue("金额(元)");
        styleHeader(wb, sumHeader);

        if (summary != null) {
            Object[][] items = {
                    {"本月总收入", summary.get("total_income")},
                    {"本月总支出", summary.get("total_expense")},
                    {"本月净利润", summary.get("net_profit")},
            };
            for (int i = 0; i < items.length; i++) {
                Row row = summarySheet.createRow(i + 1);
                row.createCell(0).setCellValue((String) items[i][0]);
                row.createCell(1).setCellValue(toDouble(items[i][1]));
            }
        }

        trendSheet.autoSizeColumn(0);
        trendSheet.autoSizeColumn(1);
        pieSheet.autoSizeColumn(0);
        pieSheet.autoSizeColumn(1);
        summarySheet.autoSizeColumn(0);
        summarySheet.autoSizeColumn(1);
        return wb;
    }

    // ==================== 工具方法 ====================

    private static void styleHeader(Workbook wb, Row row) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null) cell.setCellStyle(style);
        }
    }

    private static double toDouble(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private static int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
