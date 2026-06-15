-- ============================================
-- user 表新增：已有驾照信息（外校学员）
-- 执行: mysql -u root -proot driveman --default-character-set=utf8mb4 < database/upgrade/upgrade_existing_license.sql
-- ============================================
ALTER TABLE `user`
  ADD COLUMN `existing_license` VARCHAR(10) DEFAULT NULL COMMENT '已有驾照类型（外校学员）' AFTER `license_obtained_date`,
  ADD COLUMN `existing_license_years` DECIMAL(3,1) DEFAULT NULL COMMENT '已有驾照驾龄（年）' AFTER `existing_license`,
  ADD COLUMN `existing_license_file_id` INT UNSIGNED DEFAULT NULL COMMENT '已有驾照证明文件ID' AFTER `existing_license_years`;
