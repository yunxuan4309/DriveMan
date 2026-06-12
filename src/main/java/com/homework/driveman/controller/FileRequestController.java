package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.FileRequest;
import com.homework.driveman.service.IFileRequestService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 文件提交请求控制器 — 管理员/教练发起文件上传请求，学员查看并完成上传
 */
@Tag(name = "文件提交请求")
@RestController
@RequestMapping("/file-requests")
public class FileRequestController {

    @Autowired
    private IFileRequestService fileRequestService;

    private CurrentUser getUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute("currentUser");
    }

    // ==================== 发起方（管理员/教练） ====================

    @RequireRole({2, 3})
    @Operation(summary = "创建文件提交请求", description = "管理员或教练向学员发起文件上传请求")
    @PostMapping
    public JsonResult<FileRequest> create(HttpServletRequest request,
                                          @RequestParam Integer targetUserId,
                                          @RequestParam String title,
                                          @RequestParam(required = false) String description,
                                          @RequestParam(required = false) String bizType,
                                          @RequestParam(required = false) Integer bizId,
                                          @RequestParam(required = false) String fileType,
                                          @RequestParam(required = false) String remark,
                                          @RequestParam(required = false) String deadline) {
        CurrentUser user = getUser(request);
        FileRequest fr = fileRequestService.create(user.getUserId(), targetUserId, title,
                description, bizType, bizId, fileType, remark, deadline);
        return JsonResult.ok(fr);
    }

    @RequireRole({2, 3})
    @Operation(summary = "管理端分页查询所有文件请求", description = "支持按目标学员姓名和状态筛选")
    @GetMapping
    public JsonResult<Page<Map<String, Object>>> listAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String targetName,
            @RequestParam(required = false) Integer status) {
        return JsonResult.ok(fileRequestService.pageAll(new Page<>(page, size), targetName, status));
    }

    @RequireRole({2, 3})
    @Operation(summary = "取消文件请求")
    @PutMapping("/{id}/cancel")
    public JsonResult<Void> cancel(@PathVariable Integer id) {
        fileRequestService.cancel(id);
        return JsonResult.ok();
    }

    // ==================== 目标用户（学员/教练） ====================

    @RequireRole({1, 2})
    @Operation(summary = "查看我的文件请求", description = "目标用户查看发给自己的文件请求")
    @GetMapping("/my")
    public JsonResult<Page<Map<String, Object>>> listMy(HttpServletRequest request,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        CurrentUser user = getUser(request);
        return JsonResult.ok(fileRequestService.pageByTargetUser(new Page<>(page, size), user.getUserId()));
    }

    @RequireRole({1, 2})
    @Operation(summary = "获取未完成请求数", description = "用于登录后红点/横幅提醒")
    @GetMapping("/my-count")
    public JsonResult<Integer> countPending(HttpServletRequest request) {
        CurrentUser user = getUser(request);
        return JsonResult.ok(fileRequestService.countPending(user.getUserId()));
    }
}
