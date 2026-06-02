package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 费用标准表 — 各车型各科目的收费标准
 * subject 字段: 1-4 分别对应科目一到科目四, NULL 表示全包套餐总价
 */
@Data
@TableName("fee_standard")
public class FeeStandard {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 车型: C1/C2/B1... */
    private String licenseType;

    /** 科目: 1-科目一, 2-科目二, 3-科目三, 4-科目四, NULL 表示套餐总价 */
    private Integer subject;

    /** 金额（元） */
    private BigDecimal amount;

    /** 费用说明 */
    private String description;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}