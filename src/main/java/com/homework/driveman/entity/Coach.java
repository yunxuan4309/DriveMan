package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教练扩展表 — 与 user 表一对一关联
 * 存储教练特有的评分、执教年限、准教车型等信息
 */
@Data
@TableName("coach")
public class Coach {

    @TableId(type = IdType.AUTO)
    private Integer coachId;

    /** 关联 user.user_id */
    private Integer userId;

    /** 综合评分 (1.0-5.0) */
    private BigDecimal rating;

    /** 空闲时间 (JSON 格式) */
    private String availableTime;

    /** 执教年限 */
    private Integer coachYears;

    /** 准教车型，逗号分隔 */
    private String vehicleType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
