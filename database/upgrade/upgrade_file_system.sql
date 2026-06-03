-- ============================================
-- 文件管理模块重构 — 增量升级
-- 说明: 为 file 表补充业务关联字段
-- 执行: mysql -u root -proot driveman < upgrade_file_system.sql
-- ============================================

-- 1. 新增字段（向后兼容，旧数据这些字段为 NULL）
ALTER TABLE `file`
    ADD COLUMN `file_size` BIGINT       DEFAULT NULL COMMENT '文件大小（字节）' AFTER `file_path`,
    ADD COLUMN `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型'      AFTER `file_size`,
    ADD COLUMN `biz_type`  VARCHAR(30)  DEFAULT NULL COMMENT '业务类型'      AFTER `file_type`,
    ADD COLUMN `biz_id`    INT UNSIGNED DEFAULT NULL COMMENT '业务记录ID'    AFTER `biz_type`;

-- 2. 新增复合索引（加快按业务查询）
ALTER TABLE `file`
    ADD INDEX `idx_biz` (`biz_type`, `biz_id`);
