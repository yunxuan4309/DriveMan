package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 增驾申请表 — 学员申请增驾（同级或升级）
 * upgrade_type: 1-同级增驾, 2-升级增驾
 */
@Data
@TableName("license_upgrade")
public class LicenseUpgrade {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 学员 user_id */
    private Integer studentId;

    /** 原准驾车型 */
    private String originalLicense;

    /** 目标准驾车型 */
    private String targetLicense;

    /** 增驾类型: 1-同级增驾, 2-升级增驾 */
    private Integer upgradeType;

    /** 状态: 0-待审核, 1-审核通过, 2-审核不通过 */
    private Integer status;

    /** 审核备注 */
    private String remark;

    /** 考试状态: 0-待考试, 1-考试通过, 2-考试不通过 */
    private Integer examStatus;

    /** 考试不通过原因/备注 */
    private String examRemark;

    /** 驾驶证材料文件ID列表，逗号分隔（学员上传的驾驶证照片/扫描件） */
    private String licenseFileId;

    /** 跳过的科目编号（逗号分隔，如 "1,3" 表示科目一和科目三免考） */
    private String skipSubjects;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
