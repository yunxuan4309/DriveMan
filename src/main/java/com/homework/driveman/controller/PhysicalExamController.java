package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.PhysicalExam;
import com.homework.driveman.service.IPhysicalExamService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 体检申请控制器 — 学员端提交体检申请，管理员审核
 */
@Tag(name = "体检申请")
@RestController
@RequestMapping("/physical-exams")
public class PhysicalExamController {

    @Autowired
    private IPhysicalExamService physicalExamService;

    /**
     * 从请求中获取当前登录用户
     */
    private CurrentUser getCurrentUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute("currentUser");
    }

    // ==================== 学员端接口 ====================

    @RequireRole(1)
    @Operation(summary = "提交体检申请", description = "学员选择体检地点（venueId）和日期提交申请")
    @PostMapping("/apply")
    public JsonResult<PhysicalExam> apply(HttpServletRequest request,
                                          @RequestParam Integer venueId,
                                          @RequestParam String examDate) {
        CurrentUser user = getCurrentUser(request);
        PhysicalExam exam = physicalExamService.apply(user.getUserId(), venueId, examDate);
        return JsonResult.ok(exam);
    }

    @RequireRole(1)
    @Operation(summary = "查看我的体检申请", description = "学员查看自己的体检申请记录及结果")
    @GetMapping("/my")
    public JsonResult<List<PhysicalExam>> listMyExams(HttpServletRequest request) {
        CurrentUser user = getCurrentUser(request);
        List<PhysicalExam> list = physicalExamService.listByStudent(user.getUserId());
        return JsonResult.ok(list);
    }

    // ==================== 管理员端接口 ====================

    @RequireRole(3)
    @Operation(summary = "查询所有体检申请", description = "管理员查看所有学员的体检申请")
    @GetMapping
    public JsonResult<List<PhysicalExam>> listAll() {
        List<PhysicalExam> list = physicalExamService.lambdaQuery()
                .orderByDesc(PhysicalExam::getCreateTime)
                .list();
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "审核体检申请", description = "管理员审核学员的体检申请（通过/不通过）")
    @PutMapping("/{id}/audit")
    public JsonResult<Void> audit(@PathVariable Integer id,
                                  @RequestParam Integer status,
                                  @RequestParam(required = false) String remark) {
        physicalExamService.audit(id, status, remark);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "录入体检结果", description = "管理员上传体检报告文件ID并录入结果")
    @PutMapping("/{id}/result")
    public JsonResult<Void> uploadResult(@PathVariable Integer id,
                                         @RequestParam Integer fileId,
                                         @RequestParam Integer result) {
        physicalExamService.uploadResult(id, fileId, result);
        return JsonResult.ok();
    }

    // ==================== 体检地点管理 ====================

    @RequireRole({1, 3})
    @Operation(summary = "获取可选体检地点", description = "获取启用的体检地点列表（从 venue 表查询，venue_type=3），学员端用于下拉选择")
    @GetMapping("/locations")
    public JsonResult<List<String>> getLocations() {
        List<String> locations = physicalExamService.getLocations();
        return JsonResult.ok(locations);
    }
}
