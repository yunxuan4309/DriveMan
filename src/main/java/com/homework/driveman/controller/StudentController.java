package com.homework.driveman.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.User;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 学员管理控制器 — 仅管理学员角色（role=1）的信息
 */
@Tag(name = "学员管理")
@RestController
@RequestMapping("/students")
public class StudentController {

    @Autowired
    private IUserService userService;

    @RequireRole(3)
    @Operation(summary = "分页查询学员",
            description = "支持按用户名、姓名模糊搜索及审核状态筛选，多条件可组合查询，page 从 1 开始")
    @GetMapping
    public JsonResult<Page<User>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) String username,
                                       @RequestParam(required = false) String realName) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getRole, 1)
                .eq(status != null, User::getStatus, status)
                .like(username != null && !username.isEmpty(), User::getUsername, username)
                .like(realName != null && !realName.isEmpty(), User::getRealName, realName)
                .orderByDesc(User::getCreateTime);
        return JsonResult.ok(userService.page(new Page<>(page, size), wrapper));
    }

    @RequireRole(3)
    @Operation(summary = "根据ID查询学员")
    @GetMapping("/{id}")
    public JsonResult<User> getById(@PathVariable Integer id) {
        User user = userService.getById(id);
        if (user != null && user.getRole() != 1) {
            return JsonResult.ok(null);
        }
        return JsonResult.ok(user);
    }

    @RequireRole(3)
    @Operation(summary = "新增学员",
            description = "新增用户，角色自动设为学员，密码自动 BCrypt 加密")
    @PostMapping
    public JsonResult<Void> add(@RequestBody User user) {
        user.setRole(1);
        userService.register(user);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改学员信息",
            description = "密码传空字符串或 null 不会覆盖原密码")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody User user) {
        user.setUserId(id);
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword(null);
        }
        userService.updateById(user);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除学员（逻辑删除）")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        userService.removeById(id);
        return JsonResult.ok();
    }
}
