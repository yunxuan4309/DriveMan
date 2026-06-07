package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.SpecialPersonRecord;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.service.ISpecialPersonRecordService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 特殊人群记录控制器 — 学员主动申报犯罪、酒驾等记录，管理员审核
 */
@Tag(name = "特殊人群记录管理")
@RestController
@RequestMapping("/special-person-records")
public class SpecialPersonRecordController {

    @Autowired
    private ISpecialPersonRecordService specialPersonRecordService;

    private CurrentUser getCurrentUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute("currentUser");
    }

    // ==================== 学员端接口 ====================

    @RequireRole(1)
    @Operation(summary = "提交特殊人群记录",
            description = "学员主动申报犯罪、酒驾等记录\n" +
                    "recordType: 1-犯罪记录, 2-饮酒驾驶, 3-醉酒驾驶, 4-吸毒/毒驾, 5-交通肇事逃逸, 6-超速/超员构成犯罪\n" +
                    "banYears: 禁驾年限（年），null表示终生禁驾\n" +
                    "courtDocFileId: 法律文书扫描件文件ID")
    @PostMapping("/submit")
    public JsonResult<SpecialPersonRecord> submit(HttpServletRequest request,
                                                  @RequestParam Integer recordType,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate recordDate,
                                                  @RequestParam(required = false) Integer banYears,
                                                  @RequestParam String courtDocNo,
                                                  @RequestParam Integer courtDocFileId) {
        CurrentUser user = getCurrentUser(request);
        SpecialPersonRecord record = specialPersonRecordService.submit(
                user.getUserId(), recordType, recordDate, banYears, courtDocNo, courtDocFileId);
        return JsonResult.ok(record);
    }

    @RequireRole(1)
    @Operation(summary = "查看我的特殊人群记录", description = "学员查看自己提交的所有记录及审核状态")
    @GetMapping("/my")
    public JsonResult<List<SpecialPersonRecord>> getMyRecords(HttpServletRequest request) {
        CurrentUser user = getCurrentUser(request);
        List<SpecialPersonRecord> list = specialPersonRecordService.listByUserId(user.getUserId());
        return JsonResult.ok(list);
    }

    @RequireRole(1)
    @Operation(summary = "检查我是否处于禁驾期", description = "返回当前是否处于禁驾期及禁驾截止日期")
    @GetMapping("/my/ban-status")
    public JsonResult<java.util.Map<String, Object>> getMyBanStatus(HttpServletRequest request) {
        CurrentUser user = getCurrentUser(request);
        boolean inBan = specialPersonRecordService.isInBanPeriod(user.getUserId());
        LocalDate banEnd = specialPersonRecordService.getBanEndDate(user.getUserId());

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("inBanPeriod", inBan);
        if (banEnd == null) {
            result.put("banEndDate", null);
            result.put("banType", "无限制");
        } else if (banEnd.equals(LocalDate.MAX)) {
            result.put("banEndDate", "终生");
            result.put("banType", "终生禁驾");
        } else {
            result.put("banEndDate", banEnd.toString());
            result.put("banType", "限期禁驾");
        }
        return JsonResult.ok(result);
    }

    // ==================== 管理员端接口 ====================

    @RequireRole(3)
    @Operation(summary = "查询所有待审核记录", description = "管理员查看所有待审核的特殊人群记录")
    @GetMapping("/pending")
    public JsonResult<List<SpecialPersonRecord>> listPending() {
        List<SpecialPersonRecord> list = specialPersonRecordService.listPending();
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "查询所有记录", description = "管理员查看所有特殊人群记录")
    @GetMapping
    public JsonResult<List<SpecialPersonRecord>> listAll() {
        List<SpecialPersonRecord> list = specialPersonRecordService.lambdaQuery()
                .orderByDesc(SpecialPersonRecord::getCreateTime)
                .list();
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "审核特殊人群记录", description = "管理员审核学员提交的记录（通过/不通过）")
    @PutMapping("/{id}/audit")
    public JsonResult<Void> audit(@PathVariable Integer id,
                                  @RequestParam Integer auditStatus,
                                  @RequestParam(required = false) String auditRemark,
                                  HttpServletRequest request) {
        CurrentUser admin = getCurrentUser(request);
        specialPersonRecordService.audit(id, auditStatus, auditRemark, admin.getUserId());
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "根据用户ID查询记录", description = "管理员根据用户ID查询该用户的所有特殊记录")
    @GetMapping("/user/{userId}")
    public JsonResult<List<SpecialPersonRecord>> getByUserId(@PathVariable Integer userId) {
        List<SpecialPersonRecord> list = specialPersonRecordService.listByUserId(userId);
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "检查用户是否处于禁驾期", description = "管理员检查指定用户是否处于禁驾期")
    @GetMapping("/user/{userId}/ban-status")
    public JsonResult<java.util.Map<String, Object>> getUserBanStatus(@PathVariable Integer userId) {
        boolean inBan = specialPersonRecordService.isInBanPeriod(userId);
        LocalDate banEnd = specialPersonRecordService.getBanEndDate(userId);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("inBanPeriod", inBan);
        if (banEnd == null) {
            result.put("banEndDate", null);
            result.put("banType", "无限制");
        } else if (banEnd.equals(LocalDate.MAX)) {
            result.put("banEndDate", "终生");
            result.put("banType", "终生禁驾");
        } else {
            result.put("banEndDate", banEnd.toString());
            result.put("banType", "限期禁驾");
        }
        return JsonResult.ok(result);
    }
}
