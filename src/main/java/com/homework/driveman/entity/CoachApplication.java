package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教练选择申请表 — 学员申请指定教练
 * status: 0-待审核, 1-通过, 2-拒绝
 */
@Data
@TableName("coach_application")
public class CoachApplication {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 学员 user_id */
    private Integer studentId;

    /** 申请的教练 coach_id */
    private Integer coachId;

    /** 发起移交的教练 coach_id（NULL 表示学员自主申请，非NULL表示教练发起移交） */
    private Integer sourceCoachId;

    /** 教练移交原因（学员主动申请时为 NULL） */
    private String transferReason;

    /** 状态: 0-待审核, 1-通过, 2-拒绝 */
    private Integer status;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 拒绝原因 */
    private String auditReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
