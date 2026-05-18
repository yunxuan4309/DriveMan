package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.homework.driveman.entity.User;
import org.springframework.stereotype.Repository;

/** 用户表 Mapper — 继承 MyBatis-Plus 的 BaseMapper 获得基础 CRUD */
@Repository
public interface UserMapper extends BaseMapper<User> {
}
