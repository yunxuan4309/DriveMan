package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教练车表 — 驾校教练车车队管理
 * status: 1-空闲, 2-使用中, 3-维修, 4-报废
 */
@Data
@TableName("vehicle")
public class Vehicle {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 车牌号 */
    private String plateNumber;

    /** 车型: C1/C2/B1/N1... */
    private String vehicleType;

    /** 品牌 */
    private String brand;

    /** 型号 */
    private String model;

    /** 座位数/核载人数 */
    private Integer seats;

    /** 状态: 1-空闲, 2-使用中, 3-维修, 4-报废 */
    private Integer status;

    /** 备注 */
    private String remarks;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
