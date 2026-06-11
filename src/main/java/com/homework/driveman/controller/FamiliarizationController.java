package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.FamiliarizationRecord;
import com.homework.driveman.service.IFamiliarizationRecordService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 合场管理控制器 — 学员申请、支付、管理员安排
 */
@Tag(name = "合场管理")
@RestController
@RequestMapping("/familiarizations")
public class FamiliarizationController {

    @Autowired
    private IFamiliarizationRecordService familiarizationRecordService;

    @Autowired
    private com.homework.driveman.mapper.FamiliarizationRecordMapper familiarizationRecordMapper;

    // ==================== 学员端接口 ====================

    @RequireRole(1)
    @Operation(summary = "申请合场",
            description = "学员选择考试场次和用车类型（1-教练车/2-考试车），系统自动按 fee_standard 定价并生成待支付账单。返回含教练姓名和学员姓名的合场详情")
    @PostMapping("/apply")
    public JsonResult<Map<String, Object>> apply(@RequestParam Integer examSessionId,
                                                  @RequestParam Integer carType,
                                                  HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        FamiliarizationRecord record = familiarizationRecordService.apply(currentUser.getUserId(), examSessionId, carType);
        return JsonResult.ok(familiarizationRecordMapper.selectByIdWithDetails(record.getId()));
    }

    @RequireRole(1)
    @Operation(summary = "支付合场", description = "学员支付自己的待支付合场记录（模拟支付）。返回含教练姓名和学员姓名的合场详情")
    @PutMapping("/{id}/pay")
    public JsonResult<Map<String, Object>> pay(@PathVariable Integer id, HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        familiarizationRecordService.pay(id, currentUser.getUserId());
        return JsonResult.ok(familiarizationRecordMapper.selectByIdWithDetails(id));
    }

    @RequireRole(1)
    @Operation(summary = "我的合场记录（分页+条件查询）",
            description = "分页查询自己的合场记录列表，支持按状态、创建时间范围筛选")
    @GetMapping("/my")
    public JsonResult<Page<Map<String, Object>>> getMyRecords(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页条数") int size,
            @RequestParam(required = false) @Parameter(description = "状态筛选：0-待支付, 1-已支付, 2-已完成, 3-已取消") Integer status,
            @RequestParam(required = false) @Parameter(description = "创建时间起始（含），格式 yyyy-MM-dd HH:mm:ss") String startDate,
            @RequestParam(required = false) @Parameter(description = "创建时间结束（含），格式 yyyy-MM-dd HH:mm:ss") String endDate,
            HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        return JsonResult.ok(familiarizationRecordService.pageMyRecords(
                new Page<>(page, size), currentUser.getUserId(), status, startDate, endDate));
    }

    // ==================== 管理员端接口 ====================

    @RequireRole(3)
    @Operation(summary = "合场记录列表（全部）", description = "所有合场记录，含场次信息、教练姓名")
    @GetMapping("/all")
    public JsonResult<List<Map<String, Object>>> listAll() {
        return JsonResult.ok(familiarizationRecordService.listAll());
    }

    @RequireRole(3)
    @Operation(summary = "分页查询合场记录",
            description = "支持多条件筛选：状态、关键字（学员姓名/考试地点）、科目、用车类型、考试日期范围、教练姓名。")
    @GetMapping
    public JsonResult<Page<Map<String, Object>>> pageAll(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页条数") int size,
            @RequestParam(required = false) @Parameter(description = "状态：0-待支付, 1-已支付, 2-已完成, 3-已取消") Integer status,
            @RequestParam(required = false) @Parameter(description = "关键字（学员姓名/考试地点）") String keyword,
            @RequestParam(required = false) @Parameter(description = "科目筛选") Integer subject,
            @RequestParam(required = false) @Parameter(description = "用车类型：1-教练车, 2-考试车") Integer carType,
            @RequestParam(required = false) @Parameter(description = "考试日期起始，格式 yyyy-MM-dd") String examDateStart,
            @RequestParam(required = false) @Parameter(description = "考试日期结束，格式 yyyy-MM-dd") String examDateEnd,
            @RequestParam(required = false) @Parameter(description = "教练姓名") String coachName) {
        return JsonResult.ok(familiarizationRecordService.pageAll(
                new Page<>(page, size), status, keyword, subject, carType, examDateStart, examDateEnd, coachName));
    }

    @RequireRole(3)
    @Operation(summary = "安排合场时间", description = "管理员为已支付的合场记录安排具体时间，格式 yyyy-MM-dd HH:mm:ss。返回含姓名的合场详情")
    @PutMapping("/{id}/schedule")
    public JsonResult<Map<String, Object>> schedule(@PathVariable Integer id,
                                                     @RequestParam String scheduledTime) {
        familiarizationRecordService.schedule(id, scheduledTime);
        return JsonResult.ok(familiarizationRecordMapper.selectByIdWithDetails(id));
    }

    @RequireRole(3)
    @Operation(summary = "合场完成", description = "标记合场已完成。返回含姓名的合场详情")
    @PutMapping("/{id}/complete")
    public JsonResult<Map<String, Object>> complete(@PathVariable Integer id) {
        familiarizationRecordService.complete(id);
        return JsonResult.ok(familiarizationRecordMapper.selectByIdWithDetails(id));
    }

    @RequireRole(3)
    @Operation(summary = "取消合场", description = "取消合场记录（已完成的不允许取消）。返回含姓名的合场详情")
    @PutMapping("/{id}/cancel")
    public JsonResult<Map<String, Object>> cancel(@PathVariable Integer id) {
        familiarizationRecordService.cancel(id);
        return JsonResult.ok(familiarizationRecordMapper.selectByIdWithDetails(id));
    }
}
