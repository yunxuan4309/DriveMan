package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场地统一管理表 — 考场/训练场地/体检地点
 * venue_type: 1-考场, 2-训练场地, 3-体检地点
 * status: 1-启用, 0-停用
 */
@Data
@TableName("venue")
public class Venue {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 类型: 1-考场, 2-训练场地, 3-体检地点 */
    private Integer venueType;

    /** 场地名称 */
    private String name;

    /** 详细地址 */
    private String address;

    /** 联系电话 */
    private String contactPhone;

    /** 容纳人数 */
    private Integer capacity;

    /** 设施设备说明 */
    private String facilities;

    /** 最大同时容纳车辆数（仅训练场地） */
    private Integer maxVehicles;

    /** 支持训练的车型，逗号分隔，NULL表示不限 */
    private String supportedTypes;

    /** 支持训练的科目，逗号分隔如"2,3"，NULL表示不限 */
    private String subjects;

    /** 状态: 1-启用, 0-停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
