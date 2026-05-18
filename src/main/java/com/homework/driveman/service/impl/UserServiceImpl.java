package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.User;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.IUserService;
import org.springframework.stereotype.Service;

/** 用户业务实现 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
}
