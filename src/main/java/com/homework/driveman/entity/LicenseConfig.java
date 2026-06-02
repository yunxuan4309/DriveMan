package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车型科目配置表 — 各车型各科目的学时要求、考试项目
 */
@Data
@TableName("license_config")
public class LicenseConfig {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 车型: C1/C2/B1... */
    private String licenseType;

    /** 科目: 1-4 */
    private Integer subject;

    /** 要求学时 */
    private BigDecimal requiredHours;

    /** 考试项目，逗号分隔 */
    private String examItems;

    /** 科目说明 */
    private String description;

    /** 排序 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
