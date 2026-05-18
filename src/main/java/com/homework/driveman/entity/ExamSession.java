package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 考试场次表 — 管理各科目考试场次及名额
 * status: 1-报名中, 2-已满, 3-已结束
 */
@Data
@TableName("exam_session")
public class ExamSession {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 科目: 1-4 */
    private Integer subject;

    /** 考试日期 */
    private LocalDate examDate;

    /** 开始时间 */
    private LocalTime startTime;

    /** 考试地点 */
    private String location;

    /** 总名额 */
    private Integer totalQuota;

    /** 剩余名额 */
    private Integer remainingQuota;

    /** 状态: 1-报名中, 2-已满, 3-已结束 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
