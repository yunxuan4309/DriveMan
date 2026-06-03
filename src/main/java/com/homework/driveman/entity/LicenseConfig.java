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

    /** 车型: C1/C2/B1/N1... */
    private String licenseType;

    /** 科目: 1-4(小汽车), 1=理论,2=实操(特种车) */
    private Integer subject;

    /** 要求学时 */
    private BigDecimal requiredHours;

    /** 考试项目，逗号分隔 */
    private String examItems;

    /** 科目说明 */
    private String description;

    /** 排序 */
    private Integer sortOrder;

    /** 考试模式: 1-小汽车(科一~科四), 2-特种车辆(理论+实操) */
    private Integer examMode;

    /** 报名考试是否需要教练审核: 1-需要, 0-不需要 */
    private Integer coachAuditRequired;

    /** 获证名称(仅特种车辆使用, 如"叉车操作证") */
    private String certName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
