package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表 — 学员/教练/管理员共用
 * role: 1-学员, 2-教练, 3-管理员
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Integer userId;

    /** 角色: 1-学员, 2-教练, 3-管理员 */
    private Integer role;

    /** 登录账号（手机号/身份证号） */
    private String username;

    /** 密码（BCrypt 加密） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 身份证号 */
    private String idCard;

    /** 手机号 */
    private String phone;

    /** 通讯地址 */
    private String address;

    /** 报考车型: C1/C2/C5... */
    private String licenseType;

    /** 头像 URL */
    private String avatar;

    /** 审核状态: 0-待审核, 1-审核通过(已报名), 2-审核不通过 */
    private Integer status;

    /** 审核不通过原因 */
    private String auditReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
