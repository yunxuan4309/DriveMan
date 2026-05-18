package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学员-教练关联表 — 学员与教练的绑定/历史关系
 * status: 1-正常绑定, 0-已解绑
 */
@Data
@TableName("student_coach")
public class StudentCoach {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 学员 user_id */
    private Integer studentId;

    /** 教练 coach_id */
    private Integer coachId;

    /** 绑定时间 */
    private LocalDateTime bindTime;

    /** 状态: 1-正常绑定, 0-已解绑 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
