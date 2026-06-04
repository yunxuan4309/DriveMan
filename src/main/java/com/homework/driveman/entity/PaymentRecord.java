package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录表 — 记录每笔应收/实收/退款
 */
@Data
@TableName("payment_record")
public class PaymentRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 学员 user_id */
    private Integer studentId;

    /** 业务类型: registration_fee-报名费, exam_fee-考试费, familiarization_fee-合场费, other-其他 */
    private String bizType;

    /** 关联业务记录ID */
    private Integer bizId;

    /** 金额(元) */
    private BigDecimal amount;

    /** 状态: 0-待支付, 1-已支付, 2-已退款 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 退款时间 */
    private LocalDateTime refundTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
