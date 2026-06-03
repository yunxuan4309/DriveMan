package com.homework.driveman.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.SpecialExamRecord;
import com.homework.driveman.service.ISpecialExamRecordService;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 特种车辆考试管理控制器 — 管理特种车辆（N1/N2/N3）的理论与实操考试
 * 管理员录入成绩、查询记录；学员/教练查询考试记录
 */
@Tag(name = "特种车辆考试管理")
@RestController
@RequestMapping("/special-exams")
public class SpecialExamController {

    @Autowired
    private ISpecialExamRecordService specialExamRecordService;

    @RequireRole({1, 2, 3})
    @Operation(summary = "查询某学员的特种车辆考试记录", description = "按学员ID和车型查询其所有考试记录")
    @GetMapping("/student/{studentId}")
    public JsonResult<List<SpecialExamRecord>> getByStudent(@PathVariable Integer studentId,
                                                             @RequestParam(required = false) String licenseType) {
        LambdaQueryWrapper<SpecialExamRecord> qw = new LambdaQueryWrapper<SpecialExamRecord>()
                .eq(SpecialExamRecord::getStudentId, studentId);
        if (licenseType != null && !licenseType.isEmpty()) {
            qw.eq(SpecialExamRecord::getLicenseType, licenseType);
        }
        qw.orderByAsc(SpecialExamRecord::getLicenseType)
          .orderByAsc(SpecialExamRecord::getSubject);
        return JsonResult.ok(specialExamRecordService.list(qw));
    }

    @RequireRole(3)
    @Operation(summary = "录入特种车辆考试成绩", description = "管理员录入学员的单科成绩，补考次数自动累计")
    @PostMapping
    public JsonResult<Void> create(@RequestBody SpecialExamRecord record) {
        // 如果已有该学员该车型该科目的记录，则不允许重复新增（应使用更新接口）
        boolean exists = specialExamRecordService.lambdaQuery()
                .eq(SpecialExamRecord::getStudentId, record.getStudentId())
                .eq(SpecialExamRecord::getLicenseType, record.getLicenseType())
                .eq(SpecialExamRecord::getSubject, record.getSubject())
                .exists();
        if (exists) {
            return JsonResult.fail(ServiceCode.ERROR_CONFLICT, "该学员此科目已有考试记录，请使用更新接口");
        }
        specialExamRecordService.save(record);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "更新特种车辆考试成绩", description = "更新成绩时自动累加补考次数")
    @PutMapping("/{id}")
    public JsonResult<Void> updateScore(@PathVariable Integer id, @RequestBody SpecialExamRecord record) {
        SpecialExamRecord existing = specialExamRecordService.getById(id);
        if (existing == null) {
            return JsonResult.fail(ServiceCode.ERROR_NOT_FOUND, "记录不存在");
        }
        // 累加补考次数
        if (record.getScore() != null) {
            record.setRetakeCount(existing.getRetakeCount() + 1);
        }
        record.setId(id);
        specialExamRecordService.updateById(record);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "查询所有特种车辆考试记录", description = "返回全部记录，可按车型筛选")
    @GetMapping
    public JsonResult<List<SpecialExamRecord>> list(@RequestParam(required = false) String licenseType) {
        LambdaQueryWrapper<SpecialExamRecord> qw = new LambdaQueryWrapper<>();
        if (licenseType != null && !licenseType.isEmpty()) {
            qw.eq(SpecialExamRecord::getLicenseType, licenseType);
        }
        qw.orderByAsc(SpecialExamRecord::getLicenseType)
          .orderByAsc(SpecialExamRecord::getSubject);
        return JsonResult.ok(specialExamRecordService.list(qw));
    }
}
