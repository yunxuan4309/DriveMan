package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.User;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.UserMapper;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.web.ServiceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.homework.driveman.dto.CoachRegisterDTO;
import com.homework.driveman.entity.Coach;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** 用户业务实现 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Autowired
    private CoachMapper coachMapper;

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

    @Override
    public void updateProfile(Integer userId, User user) {
        // 查询原用户
        User existUser = getById(userId);
        if (existUser == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "用户不存在");
        }

        // 只允许更新特定字段：realName, idCard, phone, address, licenseType, avatar
        // 不允许修改：userId, role, username, password, status, auditReason
        User updateUser = new User();
        updateUser.setUserId(userId);
        
        // 白名单字段更新
        if (user.getRealName() != null) {
            updateUser.setRealName(user.getRealName());
        }
        if (user.getIdCard() != null) {
            updateUser.setIdCard(user.getIdCard());
        }
        if (user.getPhone() != null) {
            updateUser.setPhone(user.getPhone());
        }
        if (user.getAddress() != null) {
            updateUser.setAddress(user.getAddress());
        }
        if (user.getLicenseType() != null) {
            updateUser.setLicenseType(user.getLicenseType());
        }
        if (user.getAvatar() != null) {
            updateUser.setAvatar(user.getAvatar());
        }

        // 更新到数据库
        updateById(updateUser);
    }

    @Override
    @Transactional
    public void coachRegister(CoachRegisterDTO dto) {
        // 校验唯一性
        Long usernameCount = count(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername()));
        if (usernameCount > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "用户名已存在");
        }
        Long phoneCount = count(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, dto.getPhone()));
        if (phoneCount > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "手机号已被注册");
        }
        Long idCardCount = count(new LambdaQueryWrapper<User>()
                .eq(User::getIdCard, dto.getIdCard()));
        if (idCardCount > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "身份证号已被注册");
        }

        // 创建 User
        User user = new User();
        user.setRole(2);
        user.setUsername(dto.getUsername());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setIdCard(dto.getIdCard());
        user.setPhone(dto.getPhone());
        user.setLicenseType(dto.getVehicleType());
        user.setAvatar(dto.getAvatar());
        user.setStatus(0); // 待审核
        save(user);

        // 创建 Coach
        Coach coach = new Coach();
        coach.setUserId(user.getUserId());
        coach.setCoachYears(dto.getCoachYears() != null ? dto.getCoachYears() : 0);
        coach.setVehicleType(dto.getVehicleType());
        coach.setRating(new BigDecimal("5.0"));
        // 如果有证书URL，需要 Coach 实体有 certificateUrl 字段，取消下行注释
        // coach.setCertificateUrl(dto.getCertificateUrl());
        coachMapper.insert(coach);
    }
}
