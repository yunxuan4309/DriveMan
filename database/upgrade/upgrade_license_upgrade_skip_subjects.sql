-- ============================================
-- 增驾申请表增加 skip_subjects 列
-- 说明: 管理员审核时可指定跳过的科目编号
-- 前置: license_upgrade 表已存在
-- 执行: mysql -u root -proot driveman < upgrade_license_upgrade_skip_subjects.sql
-- ============================================

ALTER TABLE `license_upgrade`
    ADD COLUMN `skip_subjects` VARCHAR(10) DEFAULT NULL
    COMMENT '跳过的科目编号(逗号分隔,如1,3)' AFTER `exam_remark`;
