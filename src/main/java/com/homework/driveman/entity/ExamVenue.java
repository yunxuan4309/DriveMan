package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考场信息表 — 管理考场基础信息
 * status: 1-启用, 0-停用
 */
@Data
@TableName("exam_venue")
public class ExamVenue {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 考场名称 */
    private String name;

    /** 详细地址 */
    private String address;

    /** 联系电话 */
    private String contactPhone;

    /** 容纳人数 */
    private Integer capacity;

    /** 设施设备说明 */
    private String facilities;

    /** 状态: 1-启用, 0-停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
