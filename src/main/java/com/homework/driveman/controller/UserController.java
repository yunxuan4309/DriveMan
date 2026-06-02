package com.homework.driveman.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户管理控制器 — 学员/教练/管理员的 CRUD、注册、密码修改
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private IUserService userService;

    @RequireRole(3)
    @Operation(summary = "分页查询用户",
            description = "支持按角色筛选，page 从 1 开始")
    @GetMapping
    public JsonResult<Page<User>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) Integer role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(role != null, User::getRole, role)
                .orderByDesc(User::getCreateTime);
        Page<User> result = userService.page(new Page<>(page, size), wrapper);
        return JsonResult.ok(result);
    }

    @RequireRole(3)
    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{id}")
    public JsonResult<User> getById(@PathVariable Integer id) {
        User user = userService.getById(id);
        return JsonResult.ok(user);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public JsonResult<User> getCurrentUser(HttpServletRequest request) {
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        User user = userService.getById(currentUser.getUserId());
        return JsonResult.ok(user);
    }

    @RequireRole(3)
    @Operation(summary = "新增用户（密码自动加密）")
    @PostMapping
    public JsonResult<Void> add(@RequestBody User user) {
        userService.register(user);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody User user) {
        user.setUserId(id);
        // 密码为空时不覆盖
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword(null);
        }
        userService.updateById(user);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除用户（逻辑删除）")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        userService.removeById(id);
        return JsonResult.ok();
    }

    @Operation(summary = "修改密码",
            description = "需要提供旧密码验证身份")
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
