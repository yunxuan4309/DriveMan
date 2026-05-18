package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学时记录表 — 记录每次培训的学时信息
 * subject_type: 1-科目一, 2-科目二, 3-科目三, 4-科目四
 */
@Data
@TableName("training_record")
public class TrainingRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 学员 user_id */
    private Integer studentId;

    /** 教练 coach_id */
    private Integer coachId;

    /** 关联的约课 ID */
    private Integer appointmentId;

    /** 本次学时（小时） */
    private BigDecimal duration;

    /** 科目: 1-科目一, 2-科目二, 3-科目三, 4-科目四 */
    private Integer subjectType;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
