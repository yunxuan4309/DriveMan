-- ========================================
-- 升级脚本：为考试场次表添加乐观锁版本号
-- 说明：防止并发审核时名额超扣
-- 执行：mysql -u root -proot driveman < database/upgrade/upgrade_version_lock.sql
-- ========================================

-- 为 exam_session 表添加 version 字段（默认 1，现有记录自动填充为 1）
ALTER TABLE `exam_session`
    ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本号'
    AFTER `status`;
