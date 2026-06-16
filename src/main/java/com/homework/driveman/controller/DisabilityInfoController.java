package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.DisabilityInfo;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.service.IDisabilityInfoService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 残疾人信息控制器 — C5报名残疾信息管理（简化版）
 * 学员提交信息，管理员审核
 */
@Tag(name = "残疾人信息管理")
@RestController
@RequestMapping("/disability-info")
public class DisabilityInfoController {

    @Autowired
    private IDisabilityInfoService disabilityInfoService;

    /**
     * 从请求中获取当前登录用户
     */
    private CurrentUser getCurrentUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute("currentUser");
    }

    // ==================== 学员端接口 ====================

    @RequireRole(1)
    @Operation(summary = "提交残疾信息",
            description = "C5学员提交残疾信息及相关材料\n" +
                    "disabilityType: 1-右下肢残疾, 2-双下肢残疾, 3-右手残疾, 4-听力障碍, 5-左手残疾, 9-其他\n" +
                    "certificateFileId: 残疾人证扫描件文件ID")
    @PostMapping("/submit")
    public JsonResult<DisabilityInfo> submit(HttpServletRequest request,
                                             @RequestParam Integer disabilityType,
                                             @RequestParam String certificateNo,
                                             @RequestParam Integer certificateFileId) {
        CurrentUser user = getCurrentUser(request);
        DisabilityInfo info = disabilityInfoService.submit(
                user.getUserId(), disabilityType, certificateNo, certificateFileId);
        return JsonResult.ok(info);
    }

    @RequireRole(1)
    @Operation(summary = "查看我的残疾信息", description = "学员查看自己提交的残疾信息及审核状态")
    @GetMapping("/my")
    public JsonResult<DisabilityInfo> getMyInfo(HttpServletRequest request) {
        CurrentUser user = getCurrentUser(request);
        DisabilityInfo info = disabilityInfoService.getByUserId(user.getUserId());
        if (info == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "您尚未提交残疾信息");
        }
        return JsonResult.ok(info);
    }

    // ==================== 管理员端接口 ====================

    @RequireRole(3)
    @Operation(summary = "查询所有待审核的残疾信息", description = "管理员查看所有待审核的残疾信息提交")
    @GetMapping("/pending")
    public JsonResult<List<DisabilityInfo>> listPending() {
        List<DisabilityInfo> list = disabilityInfoService.listPending();
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "查询所有残疾信息", description = "管理员查看所有残疾信息记录")
    @GetMapping
    public JsonResult<List<DisabilityInfo>> listAll() {
        List<DisabilityInfo> list = disabilityInfoService.lambdaQuery()
                .orderByDesc(DisabilityInfo::getCreateTime)
                .list();
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "分页查询残疾信息", description = "支持按审核状态和学员姓名搜索，含学员姓名")
    @GetMapping("/page")
    public JsonResult<Page<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) Integer auditStatus,
                                                       @RequestParam(required = false) String keyword) {
        return JsonResult.ok(disabilityInfoService.pageWithDetails(new Page<>(page, size), auditStatus, keyword));
    }

    @RequireRole(3)
    @Operation(summary = "审核残疾信息", description = "管理员审核学员提交的残疾信息（通过/不通过）")
    @PutMapping("/{id}/audit")
    public JsonResult<Void> audit(@PathVariable Integer id,
                                  @RequestParam Integer auditStatus,
                                  @RequestParam(required = false) String auditRemark) {
        disabilityInfoService.audit(id, auditStatus, auditRemark);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "根据用户ID查询残疾信息", description = "管理员根据用户ID查询残疾信息详情")
    @GetMapping("/user/{userId}")
    public JsonResult<DisabilityInfo> getByUserId(@PathVariable Integer userId) {
        DisabilityInfo info = disabilityInfoService.getByUserId(userId);
        if (info == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "该用户尚未提交残疾信息");
        }
        return JsonResult.ok(info);
    }
}
