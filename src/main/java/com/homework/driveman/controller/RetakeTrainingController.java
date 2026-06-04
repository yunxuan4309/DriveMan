package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.RetakeTrainingRecord;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.service.IRetakeTrainingService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 二次培训（补考培训）控制器
 *
 * 业务说明：
 * 学员挂科后申请二次培训（额外练车），系统自动判断是否全包学员：
 * - 全包学员 → 免缴费，自动通过，直接进入培训流程
 * - 非全包学员 → 管理员审核，设定培训费，生成账单，缴费后开始培训
 *
 * 教练端只读查看（通过 /coach-portal/retake-trainings），无审核权限。
 */
@Tag(name = "二次培训管理")
@RestController
@RequestMapping("/retake-trainings")
public class RetakeTrainingController {

    @Autowired
    private IRetakeTrainingService retakeTrainingService;

    @RequireRole(1)
    @Operation(summary = "学员申请二次培训",
            description = "学员对已挂科的考试申请二次培训。全包学员自动通过免缴费；非全包学员需管理员审核并设定培训费。")
    @PostMapping
    public JsonResult<Void> apply(@RequestParam Integer examRegistrationId,
                                  @RequestParam(required = false) Integer coachId,
                                  @RequestParam(required = false) String reason,
                                  HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        // 只能为自己申请
        retakeTrainingService.apply(currentUser.getUserId(), examRegistrationId, coachId, reason);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "审核二次培训申请",
            description = "管理员审核非全包学员的二次培训申请。amount 为培训费金额（元），为空则从系统配置读取默认值。全包学员无需审核。")
    @PutMapping("/{id}/audit")
    public JsonResult<Void> audit(@PathVariable Integer id,
                                  @RequestParam boolean pass,
                                  @RequestParam(required = false) BigDecimal amount) {
        retakeTrainingService.audit(id, pass, amount);
        return JsonResult.ok();
    }

    @RequireRole(2)
    @Operation(summary = "完成培训（教练标记）",
            description = "教练标记二次培训为已完成。非全包学员需先确认已缴费。")
    @PutMapping("/{id}/complete")
    public JsonResult<Void> complete(@PathVariable Integer id) {
        retakeTrainingService.complete(id);
        return JsonResult.ok();
    }

    @Operation(summary = "取消二次培训申请",
            description = "取消二次培训记录（学员或管理员均可）")
    @PutMapping("/{id}/cancel")
    public JsonResult<Void> cancel(@PathVariable Integer id) {
        retakeTrainingService.cancel(id);
        return JsonResult.ok();
    }

    @Operation(summary = "学员查询自己的二次培训记录")
    @GetMapping("/student/{studentId}")
    public JsonResult<List<Map<String, Object>>> listByStudent(@PathVariable Integer studentId,
                                                                HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        // 学员只能查自己的，管理员可以查任意
        if (currentUser.getRole() == 1 && !currentUser.getUserId().equals(studentId)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "只能查看自己的记录");
        }
        return JsonResult.ok(retakeTrainingService.listByStudent(studentId));
    }

    @RequireRole(3)
    @Operation(summary = "管理员分页查询所有二次培训记录",
            description = "返回所有二次培训记录，含学员姓名等关联信息")
    @GetMapping
    public JsonResult<Page<Map<String, Object>>> listAll(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return JsonResult.ok(retakeTrainingService.pageAll(new Page<>(page, size)));
    }
}
