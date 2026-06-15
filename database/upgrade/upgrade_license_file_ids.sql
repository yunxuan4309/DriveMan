-- ============================================
-- license_upgrade.license_file_id 改为 VARCHAR 支持多文件
-- 执行: mysql -u root -proot driveman --default-character-set=utf8mb4 < database/upgrade/upgrade_license_file_ids.sql
-- ============================================
ALTER TABLE `license_upgrade`
  MODIFY COLUMN `license_file_id` VARCHAR(200) DEFAULT NULL COMMENT '驾驶证材料文件ID列表（逗号分隔，支持多文件）';
