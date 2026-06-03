package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件表 — 存储用户上传/系统生成的文件记录
 * 通过 biz_type + biz_id 关联具体业务，通过 userId 控制归属权限
 */
@Data
@TableName("`file`")
public class File {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 文件归属人 user_id（权限控制用） */
    private Integer userId;

    /** 原始文件名 */
    private String fileName;

    /** 存储路径（相对 upload 根目录） */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** MIME 类型 */
    private String mimeType;

    /** 文件分类（旧字段，向前兼容） */
    private String fileType;

    /**
     * 业务类型:
     * user_profile / enrollment / exam_ticket / registration_form /
     * training_record / physical_exam / license_upgrade / coach_qualification
     */
    private String bizType;

    /** 业务记录 ID */
    private Integer bizId;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
