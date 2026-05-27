package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.User;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理控制器 — 学员/教练/管理员的 CRUD 接口
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private IUserService userService;

    @RequireRole(3)
    @Operation(summary = "查询所有用户")
    @GetMapping
    public JsonResult<List<User>> list() {
        List<User> list = userService.list();
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "根据ID查询用户")
    @GetMapping("/{id}")
    public JsonResult<User> getById(@PathVariable Integer id) {
        User user = userService.getById(id);
        return JsonResult.ok(user);
    }

    @RequireRole(3)
    @Operation(summary = "新增用户")
    @PostMapping
    public JsonResult<Void> add(@RequestBody User user) {
        userService.save(user);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改用户")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody User user) {
        user.setUserId(id);
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
}
