package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.SpecialExamRecord;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.service.ISpecialExamRecordService;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 特种车辆考试记录控制器 — 管理 N1/N2/N3 的理论与实操考试成绩
 * 与普通小汽车考试分离，独立记录
 */
@Tag(name = "特种车辆考试管理")
@RestController
@RequestMapping("/special-exam-records")
public class SpecialExamRecordController {

    @Autowired
    private ISpecialExamRecordService specialExamRecordService;

    @RequireRole(3)
    @Operation(summary = "分页查询特种车辆考试记录",
            description = "支持按学员姓名、车型、科目、合格状态筛选。前端需实现分页组件。")
    @GetMapping
    public JsonResult<Page<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "每页条数") int size,
            @RequestParam(required = false) @Parameter(description = "学员姓名关键词") String studentName,
            @RequestParam(required = false) @Parameter(description = "车型筛选（N1/N2/N3）") String licenseType,
            @RequestParam(required = false) @Parameter(description = "科目：1-理论, 2-实操") Integer subject,
            @RequestParam(required = false) @Parameter(description = "是否合格：0-不合格, 1-合格") Integer passStatus) {
        return JsonResult.ok(specialExamRecordService.pageWithDetails(
                new Page<>(page, size), studentName, licenseType, subject, passStatus));
    }

    @RequireRole(3)
    @Operation(summary = "根据ID查询考试记录")
    @GetMapping("/{id}")
    public JsonResult<SpecialExamRecord> getById(@PathVariable Integer id) {
        return JsonResult.ok(specialExamRecordService.getById(id));
    }

    @RequireRole(3)
    @Operation(summary = "新增考试记录")
    @PostMapping
    public JsonResult<Void> create(@RequestBody SpecialExamRecord record) {
        specialExamRecordService.save(record);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改考试记录")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody SpecialExamRecord record) {
        record.setId(id);
        specialExamRecordService.updateById(record);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除考试记录")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        specialExamRecordService.removeById(id);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "录入成绩", description = "录入考试成绩（含是否合格、补考次数等），fileId 为学员上传的成绩截图")
    @PutMapping("/{id}/score")
    public JsonResult<Void> enterScore(@PathVariable Integer id,
                                       @RequestParam @Parameter(description = "成绩 (0-100)") Integer score,
                                       @RequestParam @Parameter(description = "是否合格：0-不合格, 1-合格") Integer passStatus,
                                       @RequestParam(required = false) @Parameter(description = "关联文件ID") Integer fileId,
                                       @RequestParam(required = false) @Parameter(description = "补考次数") Integer retakeCount,
                                       @RequestParam(required = false) @Parameter(description = "证书编号") String certNo) {
        SpecialExamRecord record = specialExamRecordService.getById(id);
        if (record == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "记录不存在");
        }
        record.setScore(score);
        record.setPassStatus(passStatus);
        if (fileId != null) record.setFileId(fileId);
        if (retakeCount != null) record.setRetakeCount(retakeCount);
        if (certNo != null) record.setCertNo(certNo);
        specialExamRecordService.updateById(record);
        return JsonResult.ok();
    }
}
