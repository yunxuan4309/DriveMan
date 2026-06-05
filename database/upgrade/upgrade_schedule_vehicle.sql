-- ============================================
-- 增量升级：排班管理 + 教练车车辆管理
-- 说明: 新增 vehicle 表 + coach_schedule 表，扩展 venue 和 appointment 表
-- 前置: 基础表已存在
-- 执行: mysql -u root -proot driveman < upgrade_schedule_vehicle.sql
-- ============================================

USE `driveman`;

-- ============================================
-- 1. 教练车表
-- ============================================
CREATE TABLE IF NOT EXISTS `vehicle` (
    `id`              INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '车辆ID',
    `plate_number`    VARCHAR(20)   NOT NULL COMMENT '车牌号',
    `vehicle_type`    VARCHAR(10)   NOT NULL COMMENT '车型: C1/C2/B1/N1...',
    `brand`           VARCHAR(50)   DEFAULT NULL COMMENT '品牌',
    `model`           VARCHAR(50)   DEFAULT NULL COMMENT '型号',
    `seats`           TINYINT       DEFAULT 5 COMMENT '座位数/核载人数',
    `status`          TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 1-空闲, 2-使用中, 3-维修, 4-报废',
    `remarks`         VARCHAR(200)  DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plate_number` (`plate_number`),
    KEY `idx_type` (`vehicle_type`),
    KEY `idx_status` (`status`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='教练车表';

-- ============================================
-- 2. 教练排班 / 车辆使用申请表
-- ============================================
CREATE TABLE IF NOT EXISTS `coach_schedule` (
    `id`              INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '排班ID',
    `coach_id`        INT UNSIGNED NOT NULL COMMENT '教练 coach_id',
    `vehicle_id`      INT UNSIGNED NOT NULL COMMENT '车辆ID',
    `venue_id`        INT UNSIGNED NOT NULL COMMENT '训练场地ID',
    `license_type`    VARCHAR(10)   NOT NULL COMMENT '培训车型（需与车辆车型匹配）',
    `start_time`      DATETIME      NOT NULL COMMENT '开始时间',
    `end_time`        DATETIME      NOT NULL COMMENT '结束时间',
    `max_students`    TINYINT       NOT NULL DEFAULT 1 COMMENT '该时段最大可容纳学员数',
    `booked_count`    TINYINT       NOT NULL DEFAULT 0 COMMENT '已预约学员数',
    `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已拒绝, 3-已完成, 4-已取消',
    `apply_reason`    VARCHAR(200)  DEFAULT NULL COMMENT '申请说明',
    `audit_remark`    VARCHAR(200)  DEFAULT NULL COMMENT '审核备注/拒绝原因',
    `apply_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `audit_time`      DATETIME      DEFAULT NULL COMMENT '审核时间',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `is_deleted`      TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_coach` (`coach_id`),
    KEY `idx_vehicle` (`vehicle_id`),
    KEY `idx_venue` (`venue_id`),
    KEY `idx_status` (`status`),
    KEY `idx_time_range` (`start_time`, `end_time`),
    KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='教练排班/车辆使用申请表';

-- ============================================
-- 3. 场地表扩展：增加训练容量相关字段（兼容旧版 MySQL）
-- ============================================
DROP PROCEDURE IF EXISTS `upgrade_venue_columns`;
DELIMITER $$
CREATE PROCEDURE `upgrade_venue_columns`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'driveman' AND TABLE_NAME = 'venue' AND COLUMN_NAME = 'max_vehicles') THEN
        ALTER TABLE `venue` ADD COLUMN `max_vehicles` TINYINT UNSIGNED DEFAULT NULL COMMENT '最大同时容纳车辆数（仅训练场地使用）';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'driveman' AND TABLE_NAME = 'venue' AND COLUMN_NAME = 'supported_types') THEN
        ALTER TABLE `venue` ADD COLUMN `supported_types` VARCHAR(50) DEFAULT NULL COMMENT '支持训练的车型，逗号分隔，NULL表示不限';
    END IF;
END$$
DELIMITER ;
CALL `upgrade_venue_columns`();
DROP PROCEDURE IF EXISTS `upgrade_venue_columns`;

-- ============================================
-- 4. 约课表扩展：关联排班（兼容旧版 MySQL）
-- ============================================
DROP PROCEDURE IF EXISTS `upgrade_appointment_columns`;
DELIMITER $$
CREATE PROCEDURE `upgrade_appointment_columns`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'driveman' AND TABLE_NAME = 'appointment' AND COLUMN_NAME = 'schedule_id') THEN
        ALTER TABLE `appointment` ADD COLUMN `schedule_id` INT UNSIGNED DEFAULT NULL COMMENT '关联排班ID';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = 'driveman' AND TABLE_NAME = 'appointment' AND INDEX_NAME = 'idx_schedule') THEN
        ALTER TABLE `appointment` ADD INDEX `idx_schedule` (`schedule_id`);
    END IF;
END$$
DELIMITER ;
CALL `upgrade_appointment_columns`();
DROP PROCEDURE IF EXISTS `upgrade_appointment_columns`;

-- ============================================
-- 5. 初始化教练车数据
-- ============================================
INSERT IGNORE INTO `vehicle` (`plate_number`, `vehicle_type`, `brand`, `model`, `seats`, `status`, `remarks`) VALUES
('渝A·C1001', 'C1', '大众', '桑塔纳', 5, 1, '手动挡教练车'),
('渝A·C1002', 'C1', '大众', '桑塔纳', 5, 1, '手动挡教练车'),
('渝A·C2001', 'C2', '丰田', '卡罗拉', 5, 1, '自动挡教练车'),
('渝A·C2002', 'C2', '本田', '思域', 5, 1, '自动挡教练车'),
('渝A·N1001', 'N1', '合力', 'CPD30', 2, 1, '叉车教练车'),
('渝A·N2001', 'N2', '卡特彼勒', '320D', 2, 1, '挖掘机教练车');
