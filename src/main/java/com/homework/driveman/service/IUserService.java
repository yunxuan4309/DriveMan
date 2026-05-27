package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.User;

/** 用户业务接口 */
public interface IUserService extends IService<User> {

    /** 注册新用户（密码自动 BCrypt 加密） */
    void register(User user);

    /** 修改密码 */
    void changePassword(Integer userId, String oldPassword, String newPassword);
}
