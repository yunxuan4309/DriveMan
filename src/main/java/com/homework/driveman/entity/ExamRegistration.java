package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考试报名表 — 学员报名考试的记录
 * status: 0-待审核, 1-审核通过, 2-审核不通过, 3-已考试
 */
@Data
@TableName("exam_registration")
public class ExamRegistration {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 学员 user_id */
    private Integer studentId;

    /** 考试场次 ID */
    private Integer sessionId;

    /** 科目（冗余字段） */
    private Integer subject;

    /** 状态: 0-待审核, 1-审核通过, 2-审核不通过, 3-已考试 */
    private Integer status;

    /** 成绩 (0-100) */
    private Integer score;

    /** 是否合格: 0-不合格, 1-合格 */
    private Integer passStatus;

    /** 补考次数 */
    private Integer retakeCount;

    /** 是否补考: 0-首次考试, 1-补考（仅标识记录，不影响计费；二次培训费走 retake_training_record 表） */
    private Integer isRetake;

    /** 报名时间 */
    private LocalDateTime applyTime;

    /** 审核时间 */
    private LocalDateTime auditTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
