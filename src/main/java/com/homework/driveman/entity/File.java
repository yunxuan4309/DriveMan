package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件表 — 存储用户上传的身份证、体检表、报名表、准考证等文件记录
 * file_type 分类: id_card_front, id_card_back, physical_exam, registration_pdf, admission_ticket
 */
@Data
@TableName("`file`")
public class File {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 上传者 user_id */
    private Integer userId;

    /** 原始文件名 */
    private String fileName;

    /** 服务器上存储的相对路径 */
    private String filePath;

    /** 文件分类: id_card_front / id_card_back / physical_exam / registration_pdf / admission_ticket */
    private String fileType;

    /** 上传时间 */
    private LocalDateTime uploadTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
