-- ============================================
-- 增量升级: user 表新增 license_obtained_date 字段
-- 说明: 学员当前车型全科通过时自动记录，用于增驾持有年限自动校验
-- 执行: mysql -u root -proot driveman < database/upgrade/upgrade_license_obtained_date.sql
-- 日期: 2026-06-14
-- ============================================

ALTER TABLE `user`
ADD COLUMN `license_obtained_date` DATETIME DEFAULT NULL COMMENT '驾照获取日期（当前车型全科通过时自动记录）' AFTER `audit_reason`;
