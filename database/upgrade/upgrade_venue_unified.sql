-- ============================================
-- 场地管理统一化升级脚本
-- 说明: 将 exam_venue 表合并为 venue 表（考场/训练场地/体检地点统一管理）
-- 前置: 01_schema.sql 已包含 venue 表定义
-- 执行: mysql -u root -proot driveman < upgrade_venue_unified.sql
-- 注意: 本脚本兼容 MySQL 8，使用 information_schema 做 DDL 幂等性检查
-- ============================================

-- ============================================
-- 1. 重命名 exam_venue -> venue（保留现有数据）
-- ============================================
SELECT COUNT(*) INTO @table_exists FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'exam_venue';
SET @stmt = IF(@table_exists > 0, 'RENAME TABLE `exam_venue` TO `venue`', 'SELECT 1');
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 2. 添加 venue 表缺失的字段和信息
-- ============================================

-- 添加 venue_type 列
SELECT COUNT(*) INTO @col_exists FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'venue' AND column_name = 'venue_type';
SET @stmt = IF(@col_exists = 0,
  'ALTER TABLE `venue` ADD COLUMN `venue_type` TINYINT NOT NULL DEFAULT 1 COMMENT ''类型: 1-考场, 2-训练场地, 3-体检地点'' AFTER `id`',
  'SELECT 1');
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 contact_phone
SELECT COUNT(*) INTO @col_exists FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'venue' AND column_name = 'contact_phone';
SET @stmt = IF(@col_exists = 0,
  'ALTER TABLE `venue` ADD COLUMN `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT ''联系电话'' AFTER `address`',
  'SELECT 1');
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 facilities
SELECT COUNT(*) INTO @col_exists FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'venue' AND column_name = 'facilities';
SET @stmt = IF(@col_exists = 0,
  'ALTER TABLE `venue` ADD COLUMN `facilities` VARCHAR(500) DEFAULT NULL COMMENT ''设施设备说明'' AFTER `capacity`',
  'SELECT 1');
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 create_time
SELECT COUNT(*) INTO @col_exists FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'venue' AND column_name = 'create_time';
SET @stmt = IF(@col_exists = 0,
  'ALTER TABLE `venue` ADD COLUMN `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `status`',
  'SELECT 1');
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 update_time
SELECT COUNT(*) INTO @col_exists FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'venue' AND column_name = 'update_time';
SET @stmt = IF(@col_exists = 0,
  'ALTER TABLE `venue` ADD COLUMN `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''修改时间'' AFTER `create_time`',
  'SELECT 1');
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加 is_deleted
SELECT COUNT(*) INTO @col_exists FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'venue' AND column_name = 'is_deleted';
SET @stmt = IF(@col_exists = 0,
  'ALTER TABLE `venue` ADD COLUMN `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''软删除: 0-未删除, 1-已删除'' AFTER `update_time`',
  'SELECT 1');
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 3. 添加缺失的索引
-- ============================================
SELECT COUNT(*) INTO @idx_exists FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'venue' AND index_name = 'idx_venue_type';
SET @stmt = IF(@idx_exists = 0,
  'ALTER TABLE `venue` ADD INDEX `idx_venue_type` (`venue_type`)',
  'SELECT 1');
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @idx_exists FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'venue' AND index_name = 'idx_status';
SET @stmt = IF(@idx_exists = 0,
  'ALTER TABLE `venue` ADD INDEX `idx_status` (`status`)',
  'SELECT 1');
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @idx_exists FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'venue' AND index_name = 'idx_is_deleted';
SET @stmt = IF(@idx_exists = 0,
  'ALTER TABLE `venue` ADD INDEX `idx_is_deleted` (`is_deleted`)',
  'SELECT 1');
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 4. 为 physical_exam 表添加 venue_id 字段和索引
-- ============================================
SELECT COUNT(*) INTO @col_exists FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'physical_exam' AND column_name = 'venue_id';
SET @stmt = IF(@col_exists = 0,
  'ALTER TABLE `physical_exam` ADD COLUMN `venue_id` INT UNSIGNED DEFAULT NULL COMMENT ''关联场地ID（venue 表）'' AFTER `student_id`',
  'SELECT 1');
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @idx_exists FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'physical_exam' AND index_name = 'idx_venue_id';
SET @stmt = IF(@idx_exists = 0,
  'ALTER TABLE `physical_exam` ADD INDEX `idx_venue_id` (`venue_id`)',
  'SELECT 1');
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================
-- 5. 回填 physical_exam.venue_id
-- ============================================
UPDATE `physical_exam` e
JOIN `venue` v ON e.`location` = v.`name` AND v.`venue_type` = 3
SET e.`venue_id` = v.`id`
WHERE e.`venue_id` IS NULL;

-- ============================================
-- 6. 插入训练场地和体检地点基础数据（不会重复插入已有数据）
-- ============================================
INSERT IGNORE INTO `venue` (`venue_type`, `name`, `address`, `contact_phone`, `capacity`, `facilities`, `status`) VALUES
(2, '南岸区训练基地', '南岸区', '023-62800555', NULL, '配备休息室、夜间照明', 1),
(2, '渝北区训练场', '渝北区', '023-67890123', NULL, '大型训练场、免费停车', 1),
(3, '南岸区人民医院体检中心', '南岸区', '023-62800120', NULL, '周一至周五 8:00-17:00', 1),
(3, '渝中区第一人民医院体检科', '渝中区', '023-63832211', NULL, '周六上午可体检', 1),
(3, '江北区中医院体检部', '江北区', '023-67788000', NULL, '需提前预约', 1);

-- ============================================
-- 7. 更新已有场地的 contact_phone 和 facilities（仅当为空时）
-- ============================================
UPDATE `venue` SET `contact_phone` = '023-62800123', `facilities` = '配备候考大厅、空调、停车场' WHERE `name` = '南岸区车管所' AND `contact_phone` IS NULL;
UPDATE `venue` SET `contact_phone` = '023-62988001', `facilities` = '科目二专用考场、视频监控' WHERE `name` = '南坪科目二考场' AND `contact_phone` IS NULL;
UPDATE `venue` SET `contact_phone` = '023-66321000', `facilities` = '科目三实际道路考场' WHERE `name` = '八公里科目三考场' AND `contact_phone` IS NULL;
UPDATE `venue` SET `contact_phone` = '023-62800555', `facilities` = '配备休息室、夜间照明' WHERE `name` = '南岸区训练基地' AND `contact_phone` IS NULL;
UPDATE `venue` SET `contact_phone` = '023-67890123', `facilities` = '大型训练场、免费停车' WHERE `name` = '渝北区训练场' AND `contact_phone` IS NULL;
UPDATE `venue` SET `contact_phone` = '023-62800120', `facilities` = '周一至周五 8:00-17:00' WHERE `name` = '南岸区人民医院体检中心' AND `contact_phone` IS NULL;
UPDATE `venue` SET `contact_phone` = '023-63832211', `facilities` = '周六上午可体检' WHERE `name` = '渝中区第一人民医院体检科' AND `contact_phone` IS NULL;
UPDATE `venue` SET `contact_phone` = '023-67788000', `facilities` = '需提前预约' WHERE `name` = '江北区中医院体检部' AND `contact_phone` IS NULL;

-- ============================================
-- 8. 删除 config 表中的体检地点配置（已迁移到 venue 表）
-- ============================================
DELETE FROM `config` WHERE `config_key` = 'physical_exam_locations';
