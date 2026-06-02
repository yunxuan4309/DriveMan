package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.File;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.service.IFileService;
import com.homework.driveman.service.IPdfService;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 报名审核控制器 — 审核学员报名、生成报名表和准考证
 */
@Tag(name = "报名审核")
@RestController
@RequestMapping("/registrations")
public class RegistrationController {

    @Autowired
    private IUserService userService;

    @Autowired
    private IPdfService pdfService;

    @Autowired
    private IFileService fileService;

    @RequireRole(3)
    @Operation(summary = "审核学员报名",
            description = "pass=true 审核通过（自动生成PDF报名表和准考证），pass=false 审核不通过（需填 reason）")
    @PutMapping("/{userId}/audit")
    public JsonResult<Void> audit(@PathVariable Integer userId,
                                  @RequestParam boolean pass,
                                  @RequestParam(required = false) String reason) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "用户不存在");
        }
        if (user.getRole() != 1) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "只能审核学员角色");
        }

        if (pass) {
            // 审核通过 → 更新状态 + 生成PDF
            user.setStatus(1);
            userService.updateById(user);

            // 生成报名表PDF
            String regPath = pdfService.generateRegistrationForm(user);
            fileService.saveRecord(userId, regPath,
                    "报名表_" + user.getRealName() + ".pdf", "registration_pdf");

            // 生成准考证PDF（注册审核时无考试场次，传 null）
            String ticketPath = pdfService.generateAdmissionTicket(user, null);
            fileService.saveRecord(userId, ticketPath,
                    "准考证_" + user.getRealName() + ".pdf", "admission_ticket");
        } else {
            // 审核不通过
            user.setStatus(2);
            user.setAuditReason(reason);
            userService.updateById(user);
        }

        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "查询所有待审核的学员")
    @GetMapping("/pending")
    public JsonResult<java.util.List<User>> listPending() {
        java.util.List<User> list = userService.lambdaQuery()
                .eq(User::getRole, 1)
                .eq(User::getStatus, 0)
                .list();
        return JsonResult.ok(list);
    }

    @Operation(summary = "查询学员的报名相关文件",
            description = "返回该学员的所有报名表和准考证PDF记录")
    @GetMapping("/{userId}/files")
    public JsonResult<java.util.List<File>> listFiles(@PathVariable Integer userId) {
        java.util.List<File> list = fileService.lambdaQuery()
                .eq(File::getUserId, userId)
                .in(File::getFileType, "registration_pdf", "admission_ticket")
                .orderByDesc(File::getCreateTime)
                .list();
        return JsonResult.ok(list);
    }
}
