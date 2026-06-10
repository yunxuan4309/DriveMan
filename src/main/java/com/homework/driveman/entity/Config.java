package com.homework.driveman.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置表 — 键值对存储系统运行参数
 * config_key 为自然主键（VARCHAR）
 */
@Data
@TableName("config")
public class Config {

    /** 配置键（自然主键） */
    @TableId
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 说明 */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
