package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统公告表 — 管理员发布的通知公告
 * 支持设置过期时间，过期后不再展示
 */
@Data
@TableName("notice")
public class Notice {

    /** 公告 ID */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 公告标题 */
    private String title;

    /** 公告内容 */
    private String content;

    /** 发布时间 */
    private LocalDateTime publishTime;

    /** 过期时间（为空永不过期） */
    private LocalDateTime expireTime;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer isDeleted;
}