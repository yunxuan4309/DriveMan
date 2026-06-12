package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 约课表 — 学员预约教练课程记录
 * status: 0-待确认, 1-已确认, 2-已拒绝, 3-已取消, 4-已完成
 */
@Data
@TableName("appointment")
public class Appointment {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 学员 user_id */
    private Integer studentId;

    /** 教练 coach_id */
    private Integer coachId;

    /** 课程开始时间 */
    private LocalDateTime startTime;

    /** 课程结束时间 */
    private LocalDateTime endTime;

    /** 状态: 0-待确认, 1-已确认, 2-已拒绝, 3-已取消, 4-已完成 */
    private Integer status;

    /** 关联排班ID */
    private Integer scheduleId;

    /** 取消原因/拒绝原因 */
    private String cancelReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
