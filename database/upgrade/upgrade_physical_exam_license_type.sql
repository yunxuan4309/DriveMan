-- ============================================
-- 增量升级: 体检表增加 license_type 字段
-- 说明: 体检标准因车型而异（如C1和B1体检要求不同），增驾时需要按目标车型校验体检
-- 执行: mysql -u root -proot driveman < database/upgrade/upgrade_physical_exam_license_type.sql
-- 日期: 2026-06-14
-- ============================================

ALTER TABLE `physical_exam`
ADD COLUMN `license_type` VARCHAR(10) DEFAULT NULL COMMENT '关联车型（体检标准因车型而异）' AFTER `venue_id`;
