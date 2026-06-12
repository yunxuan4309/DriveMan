package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 文件提交请求表 — 管理员/教练向学员发起文件上传请求
 */
@Data
@TableName("file_request")
public class FileRequest {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 发起人 user_id */
    private Integer requesterId;

    /** 目标用户 user_id */
    private Integer targetUserId;

    /** 请求标题 */
    private String title;

    /** 详细说明 */
    private String description;

    /** 关联业务类型 */
    private String bizType;

    /** 关联业务记录 ID */
    private Integer bizId;

    /** 文件类型标识 */
    private String fileType;

    /** 状态: 0-待提交, 1-已完成, 2-已取消 */
    private Integer status;

    /** 内部备注 */
    private String remark;

    /** 截止日期 */
    private LocalDate deadline;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
