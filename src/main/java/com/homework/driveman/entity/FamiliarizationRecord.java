package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合场记录表 — 学员考前熟悉考场/考车
 */
@Data
@TableName("familiarization_record")
public class FamiliarizationRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 学员 user_id */
    private Integer studentId;

    /** 关联考试场次ID */
    private Integer examSessionId;

    /** 科目（冗余） */
    private Integer subject;

    /** 用车类型: 1-教练车(教练陪同), 2-考试车(考场提供) */
    private Integer carType;

    /** 陪同教练 coach_id（教练车模式时必填） */
    private Integer coachId;

    /** 合场费用(元) */
    private BigDecimal amount;

    /** 关联支付记录ID */
    private Integer paymentRecordId;

    /** 状态: 0-待支付, 1-已支付(待安排), 2-已完成, 3-已取消 */
    private Integer status;

    /** 预约合场时间 */
    private LocalDateTime scheduledTime;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
