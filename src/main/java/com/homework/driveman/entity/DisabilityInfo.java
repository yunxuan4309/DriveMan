package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 残疾人信息表 — 存储学员的残疾信息（简化版）
 * 与user表一对一关联
 */
@Data
@TableName("disability_info")
public class DisabilityInfo {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 关联用户ID */
    private Integer userId;

    /** 残疾类型: 1-右下肢残疾, 2-双下肢残疾, 3-右手残疾, 4-听力障碍, 5-左手残疾, 9-其他 */
    private Integer disabilityType;

    /** 残疾人证号 */
    private String certificateNo;

    /** 残疾人证扫描件文件ID */
    private Integer certificateFileId;

    /** 审核状态: 0-待审核, 1-审核通过, 2-审核不通过 */
    private Integer auditStatus;

    /** 审核备注 */
    private String auditRemark;

    /** 审核时间 */
    private LocalDateTime auditTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
