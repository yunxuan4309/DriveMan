-- ============================================
-- 增量升级：残疾信息表 + 特殊人群记录表
-- 说明: 新增 disability_info 表 + special_person_record 表
-- 前置: 基础表已存在
-- 执行: mysql -u root -proot driveman < upgrade_disability_special.sql
-- ============================================

USE `driveman`;

-- ============================================
-- 1. 残疾人信息表（简化版）
-- 与 user 表一对一关联，用于 C5 驾照报名审核
-- ============================================
CREATE TABLE IF NOT EXISTS `disability_info` (
    `id`                  INT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '残疾信息ID',
    `user_id`             INT UNSIGNED  NOT NULL COMMENT '关联用户ID',
    `disability_type`     TINYINT       NOT NULL COMMENT '残疾类型: 1-右下肢残疾, 2-双下肢残疾, 3-右手残疾, 4-听力障碍, 5-左手残疾, 9-其他',
    `certificate_no`      VARCHAR(50)   NOT NULL COMMENT '残疾人证号',
    `certificate_file_id` INT UNSIGNED  DEFAULT NULL COMMENT '残疾人证扫描件文件ID',
    `audit_status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '审核状态: 0-待审核, 1-审核通过, 2-审核不通过',
    `audit_remark`        VARCHAR(200)  DEFAULT NULL COMMENT '审核备注',
    `audit_time`          DATETIME      DEFAULT NULL COMMENT '审核时间',
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`          TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_audit_status` (`audit_status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='残疾人信息表';

-- ============================================
-- 2. 特殊人群记录表（犯罪、酒驾、毒驾等）
-- 与 user 表关联，用于报名审核时的资格校验
-- ============================================
CREATE TABLE IF NOT EXISTS `special_person_record` (
    `id`                  INT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`             INT UNSIGNED  NOT NULL COMMENT '关联用户ID',
    `record_type`         TINYINT       NOT NULL COMMENT '记录类型: 1-犯罪记录, 2-饮酒驾驶, 3-醉酒驾驶, 4-吸毒/毒驾, 5-交通肇事逃逸, 6-超速/超员构成犯罪',
    `record_date`         DATE          NOT NULL COMMENT '违法/犯罪日期',
    `ban_years`           INT           DEFAULT NULL COMMENT '禁驾年限（年），null表示终生禁驾',
    `ban_end_date`        DATE          DEFAULT NULL COMMENT '禁驾截止日期，null表示终生禁驾',
    `court_doc_no`        VARCHAR(100)  NOT NULL COMMENT '法律文书编号',
    `court_doc_file_id`   INT UNSIGNED  DEFAULT NULL COMMENT '法律文书扫描件文件ID',
    `audit_status`        TINYINT       NOT NULL DEFAULT 0 COMMENT '审核状态: 0-待审核, 1-审核通过, 2-审核不通过',
    `audit_remark`        VARCHAR(200)  DEFAULT NULL COMMENT '审核备注',
    `audit_time`          DATETIME      DEFAULT NULL COMMENT '审核时间',
    `audit_user_id`       INT UNSIGNED  DEFAULT NULL COMMENT '审核人ID',
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted`          TINYINT(1)    NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_record_type` (`record_type`),
    KEY `idx_audit_status` (`audit_status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='特殊人群记录表';
