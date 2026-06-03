package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 特种车辆考试记录表 — 独立记录特种车辆（N1/N2/N3）的理论与实操考试成绩
 * 与普通小汽车考试分离，不走 exam_session / exam_registration 体系
 */
@Data
@TableName("special_exam_record")
public class SpecialExamRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 学员 user_id */
    private Integer studentId;

    /** 车型 N1/N2/N3... */
    private String licenseType;

    /** 科目: 1-理论, 2-实操 */
    private Integer subject;

    /** 成绩 (0-100) */
    private Integer score;

    /** 是否合格: 0-不合格, 1-合格 */
    private Integer passStatus;

    /** 该科目补考次数 */
    private Integer retakeCount;

    /** 考试时间 */
    private LocalDateTime examDate;

    /** 证书编号(双科通过后按规则生成) */
    private String certNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
