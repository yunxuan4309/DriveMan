package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.web.ServiceCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/** 用户业务实现 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public void register(User user) {
        // 校验用户名是否已存在
        Long count = count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername()));
        if (count > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "用户名已存在");
        }

        // 密码加密
        user.setPassword(encoder.encode(user.getPassword()));
        // 新学员默认待审核
        user.setStatus(0);
        user.setRole(1);
        save(user);
    }

    @Override
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "用户不存在");
        }
        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "旧密码错误");
        }
        user.setPassword(encoder.encode(newPassword));
        updateById(user);
    }
}
