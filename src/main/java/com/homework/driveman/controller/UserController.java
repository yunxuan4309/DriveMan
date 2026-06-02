package com.homework.driveman.controller;

import com.homework.driveman.entity.User;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户通用控制器 — 仅保留当前用户信息查询和密码修改
 * 学员 CRUD 管理请移步 StudentController (/students)
 */
@Tag(name = "用户通用")
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private IUserService userService;

    @Operation(summary = "获取当前登录用户信息",
            description = "从 Token 解析当前用户，适用于所有角色")
    @GetMapping("/me")
    public JsonResult<User> getCurrentUser(HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        User user = userService.getById(currentUser.getUserId());
        return JsonResult.ok(user);
    }

    @Operation(summary = "修改密码",
            description = "任意登录用户可调用，需要提供旧密码验证身份")
    @PutMapping("/{id}/password")
    public JsonResult<Void> changePassword(@PathVariable Integer id,
                                           @RequestParam String oldPassword,
                                           @RequestParam String newPassword) {
        userService.changePassword(id, oldPassword, newPassword);
        return JsonResult.ok();
    }

    @Operation(summary = "完善个人信息", description = "学员更新报名资料（姓名、身份证、手机号、地址、车型等）")
    @PutMapping("/{id}/profile")
    public JsonResult<Void> updateProfile(@PathVariable Integer id, 
                                          @RequestBody User user,
                                          HttpServletRequest request) {
        // 验证当前用户只能修改自己的信息
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        if (!currentUser.getUserId().equals(id)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "无权修改他人信息");
        }
        
        userService.updateProfile(id, user);
        return JsonResult.ok();
    }
}
