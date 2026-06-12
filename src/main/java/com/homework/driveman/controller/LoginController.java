package com.homework.driveman.controller;

import com.homework.driveman.entity.LicenseConfig;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.LicenseConfigMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.IUserService;
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
 * 登录控制器 — 登录 + 注册
 */
@Tag(name = "登录认证")
@RestController
public class LoginController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private IUserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private LicenseConfigMapper licenseConfigMapper;

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

        // 校验审核状态
        if (user.getStatus() != 1) {
            String msg = user.getStatus() == 0 ? "您还未通过审核" : "您的账号审核不通过";
            throw new ServiceException(ServiceCode.ERROR_UNAUTHORIZED_DISABLED, msg);
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
        result.put("licenseType", user.getLicenseType());

        // 查询考试模式（1=普通小汽车, 2=特种车辆），前端用于判断功能兼容性
        Integer examMode = null;
        if (user.getLicenseType() != null) {
            LicenseConfig config = licenseConfigMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LicenseConfig>()
                            .eq(LicenseConfig::getLicenseType, user.getLicenseType())
                            .last("LIMIT 1"));
            if (config != null) {
                examMode = config.getExamMode();
            }
        }
        result.put("examMode", examMode);

        return JsonResult.ok(result);
    }

    @Operation(summary = "学员自助注册", description = "公开接口，学员填写基本资料注册为准学员（role=0），支付报名套餐后升级为正式学员（role=1）")
    @PostMapping("/register")
    public JsonResult<Map<String, Object>> register(@RequestBody User user) {
        userService.register(user);
        return JsonResult.ok(Map.of("userId", user.getUserId()));
    }
}
