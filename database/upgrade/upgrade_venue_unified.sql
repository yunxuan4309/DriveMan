-- ============================================
-- 场地管理统一化升级脚本
-- 说明: 将 exam_venue 表合并为 venue 表（考场/训练场地/体检地点统一管理）
-- 前置: 01_schema.sql 已包含 venue 表定义
-- 执行: mysql -u root -proot driveman < upgrade_venue_unified.sql
-- ============================================

-- 1. 重命名 exam_venue -> venue（保留现有数据）
RENAME TABLE IF EXISTS `exam_venue` TO `venue`;

-- 2. 添加 venue_type 列（现有考场全部设为 1）
ALTER TABLE `venue` ADD COLUMN IF NOT EXISTS `venue_type` TINYINT NOT NULL DEFAULT 1 COMMENT '类型: 1-考场, 2-训练场地, 3-体检地点' AFTER `id`;

-- 3. 添加 venue 表缺失的字段（使用 IF NOT EXISTS 避免重复）
ALTER TABLE `venue` ADD COLUMN IF NOT EXISTS `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话' AFTER `address`;
ALTER TABLE `venue` ADD COLUMN IF NOT EXISTS `facilities` VARCHAR(500) DEFAULT NULL COMMENT '设施设备说明' AFTER `capacity`;
ALTER TABLE `venue` ADD COLUMN IF NOT EXISTS `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `status`;
ALTER TABLE `venue` ADD COLUMN IF NOT EXISTS `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间' AFTER `create_time`;
ALTER TABLE `venue` ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除: 0-未删除, 1-已删除' AFTER `update_time`;

-- 4. 添加缺失的索引
ALTER TABLE `venue` ADD INDEX IF NOT EXISTS `idx_venue_type` (`venue_type`);
ALTER TABLE `venue` ADD INDEX IF NOT EXISTS `idx_status` (`status`);
ALTER TABLE `venue` ADD INDEX IF NOT EXISTS `idx_is_deleted` (`is_deleted`);

-- 5. 为 physical_exam 表添加 venue_id 字段
ALTER TABLE `physical_exam` ADD COLUMN IF NOT EXISTS `venue_id` INT UNSIGNED DEFAULT NULL COMMENT '关联场地ID（venue 表）' AFTER `student_id`;
ALTER TABLE `physical_exam` ADD INDEX IF NOT EXISTS `idx_venue_id` (`venue_id`);

-- 6. 回填 physical_exam.venue_id（根据 location 名称匹配 venue 表）
UPDATE `physical_exam` e
JOIN `venue` v ON e.`location` = v.`name` AND v.`venue_type` = 3
SET e.`venue_id` = v.`id`
WHERE e.`venue_id` IS NULL;

-- 7. 插入训练场地和体检地点基础数据（不会重复插入已有数据）
INSERT IGNORE INTO `venue` (`venue_type`, `name`, `address`, `capacity`, `status`) VALUES
(2, '南岸区训练基地', '南岸区', NULL, 1),
(2, '渝北区训练场', '渝北区', NULL, 1),
(3, '南岸区人民医院体检中心', '南岸区', NULL, 1),
(3, '渝中区第一人民医院体检科', '渝中区', NULL, 1),
(3, '江北区中医院体检部', '江北区', NULL, 1);

-- 8. 删除 config 表中的体检地点配置（已迁移到 venue 表）
DELETE FROM `config` WHERE `config_key` = 'physical_exam_locations';
