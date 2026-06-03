-- ========================================
-- 升级脚本：教练准教车型变更申请表
-- 说明：教练提交增加可教车型申请，管理员审核
-- 执行：mysql -u root -proot driveman < database/upgrade/upgrade_coach_vehicle_application.sql
-- ========================================

CREATE TABLE IF NOT EXISTS `coach_vehicle_application` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '申请ID',
    `coach_id` INT UNSIGNED NOT NULL COMMENT '教练 coach_id',
    `current_vehicle_type` VARCHAR(20) NOT NULL COMMENT '当前准教车型（申请时的快照）',
    `requested_vehicle_type` VARCHAR(20) NOT NULL COMMENT '申请的新准教车型',
    `apply_reason` VARCHAR(200) DEFAULT NULL COMMENT '申请理由',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已拒绝',
    `audit_reason` VARCHAR(200) DEFAULT NULL COMMENT '拒绝原因',
    `apply_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_coach_id` (`coach_id`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教练准教车型变更申请表';
