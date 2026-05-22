package com.homework.driveman.controller;

import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.utils.CurrentUser;
import com.homework.driveman.utils.JwtUtils;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录控制器 — 账号密码登录，返回 JWT Token
 */
@Tag(name = "登录认证")
@RestController
public class LoginController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Operation(summary = "登录", description = "使用用户名/手机号和密码登录，返回 token 和用户信息")
    @PostMapping("/login")
    public JsonResult<Map<String, Object>> login(@RequestParam String username,
                                                 @RequestParam String password) {
        // 查找用户
        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username));
        if (user == null) {
            throw new ServiceException(ServiceCode.ERROR_UNAUTHORIZED, "用户名或密码错误");
        }

        // 校验密码
        if (!encoder.matches(password, user.getPassword())) {
            throw new ServiceException(ServiceCode.ERROR_UNAUTHORIZED, "用户名或密码错误");
        }

        // 签发 Token
        CurrentUser currentUser = new CurrentUser(user.getUserId(), user.getUsername(), user.getRole());
        String token = jwtUtils.generateToken(currentUser);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getUserId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());

        return JsonResult.ok(result);
    }
}
