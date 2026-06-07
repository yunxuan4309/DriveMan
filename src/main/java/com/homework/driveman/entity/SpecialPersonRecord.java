package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 特殊人群记录表 — 记录学员的犯罪、酒驾、毒驾等特殊记录
 * 与user表关联，用于报名审核时的资格校验
 */
@Data
@TableName("special_person_record")
public class SpecialPersonRecord {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联用户ID */
    private Integer userId;

    /**
     * 记录类型:
     * 1-犯罪记录（不含酒驾/毒驾）
     * 2-饮酒驾驶（未达醉驾标准）
     * 3-醉酒驾驶
     * 4-吸毒/毒驾
     * 5-交通肇事逃逸
     * 6-超速/超员构成犯罪
     */
    private Integer recordType;

    /** 违法/犯罪日期 */
    private LocalDate recordDate;

    /** 禁驾年限（null表示终生禁驾） */
    private Integer banYears;

    /** 禁驾截止日期（null表示终生禁驾） */
    private LocalDate banEndDate;

    /** 法律文书编号（如判决书编号） */
    private String courtDocNo;

    /** 法律文书扫描件文件ID */
    private Integer courtDocFileId;

    /** 审核状态: 0-待审核, 1-审核通过, 2-审核不通过 */
    private Integer auditStatus;

    /** 审核备注 */
    private String auditRemark;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核人ID */
    private Integer auditUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
