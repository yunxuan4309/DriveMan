package com.homework.driveman.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.IDisabilityInfoService;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

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

    @Autowired
    private IDisabilityInfoService disabilityInfoService;

    @Autowired
    private UserMapper userMapper;

    @Operation(summary = "按关键词搜索用户（用于下拉选择器远程搜索）",
            description = "按真实姓名模糊匹配，可选按角色过滤（如 role=1 学员, role=2 教练），最多返回 20 条")
    @GetMapping("/search")
    public JsonResult<List<User>> search(@RequestParam String keyword,
                                         @RequestParam(required = false) Integer role) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .like(User::getRealName, keyword)
                .select(User::getUserId, User::getRealName, User::getUsername)
                .last("LIMIT 20");
        if (role != null) {
            wrapper.eq(User::getRole, role);
        }
        return JsonResult.ok(userMapper.selectList(wrapper));
    }

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

    @Operation(summary = "完善个人信息", description = "学员更新报名资料（姓名、身份证、手机号、地址、车型等）。选择C5车型时，需先通过残疾信息审核。")
    @PutMapping("/{id}/profile")
    public JsonResult<Void> updateProfile(@PathVariable Integer id,
                                          @RequestBody User user,
                                          HttpServletRequest request) {
        // 验证当前用户只能修改自己的信息
        CurrentUser currentUser = (CurrentUser) request.getAttribute("currentUser");
        if (!currentUser.getUserId().equals(id)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "无权修改他人信息");
        }

        // 如果选择C5车型，校验是否已通过残疾信息审核
        if ("C5".equals(user.getLicenseType())) {
            if (!disabilityInfoService.isAuditPassed(id)) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                        "报考C5车型需先提交并通过残疾信息审核，请先前往 /disability-info/submit 提交残疾信息");
            }
        }

        userService.updateProfile(id, user);
        return JsonResult.ok();
    }
}
