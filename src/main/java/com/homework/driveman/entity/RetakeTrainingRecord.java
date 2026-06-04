package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 二次培训记录表 — 学员挂科后申请二次培训（补考培训）的流程记录。
 *
 * 业务逻辑：
 * - 全包学员（报名时已支付全包套餐）→ isFree=1，免缴费，审核自动通过
 * - 非全包学员 → isFree=0，需缴纳培训费（amount），缴费后才能开始培训
 * - 教练只能查看（只读），无审核权限
 * - 管理员审核申请，非全包需确认培训费金额
 *
 * status: 0-待审核, 1-培训中, 2-已完成, 3-已取消
 * payStatus: 0-无需缴费, 1-待缴费, 2-已缴费
 */
@Data
@TableName("retake_training_record")
public class RetakeTrainingRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 学员 user_id */
    private Integer studentId;

    /** 教练 user_id（培训指派的教练） */
    private Integer coachId;

    /** 关联的挂科考试报名 ID */
    private Integer examRegistrationId;

    /** 需培训的科目: 1-4 */
    private Integer subject;

    /** 状态: 0-待审核, 1-培训中, 2-已完成, 3-已取消 */
    private Integer status;

    /** 是否免费: 1-全包学员免缴费, 0-需缴费 */
    private Integer isFree;

    /** 培训费金额(元)，非全包学员需缴纳 */
    private BigDecimal amount;

    /** 缴费状态: 0-无需缴费, 1-待缴费, 2-已缴费 */
    private Integer payStatus;

    /** 申请说明 */
    private String applyReason;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 培训完成时间 */
    private LocalDateTime completeTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
