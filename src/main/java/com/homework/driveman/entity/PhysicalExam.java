package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 体检申请表 — 学员提交体检申请，选择体检地点和时间
 */
@Data
@TableName("physical_exam")
public class PhysicalExam {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 学员 user_id */
    private Integer studentId;

    /** 体检地点 */
    private String location;

    /** 预约体检日期 */
    private LocalDate examDate;

    /** 状态: 0-待审核, 1-审核通过, 2-审核不通过, 3-已完成 */
    private Integer status;

    /** 审核备注（不通过原因等） */
    private String remark;

    /** 关联的文件ID（体检报告上传后回填） */
    private Integer fileId;

    /** 体检结果: 0-不合格, 1-合格, NULL-未出结果 */
    private Integer result;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
