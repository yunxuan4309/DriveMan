package com.homework.driveman.controller;

import com.homework.driveman.service.IProgressService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 学员进度查询控制器 — 按车型返回各科目状态
 */
@Tag(name = "学员进度")
@RestController
@RequestMapping("/students")
public class ProgressController {

    @Autowired
    private IProgressService progressService;

    @Operation(summary = "查询学员进度",
            description = "根据学员报考车型，返回各科目状态（locked/learning/ready/passed）及学时进度")
    @GetMapping("/{studentId}/progress")
    public JsonResult<Map<String, Object>> getProgress(@PathVariable Integer studentId) {
        return JsonResult.ok(progressService.getProgress(studentId));
    }
}
