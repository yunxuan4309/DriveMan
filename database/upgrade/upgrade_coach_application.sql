-- ============================================
-- 升级脚本: coach_application 表扩展 — 支持教练主动移交学员
-- 新增加列:
--   source_coach_id  发起移交的教练（NULL=学员自主申请）
--   transfer_reason  移交原因
-- ============================================

ALTER TABLE `coach_application`
    ADD COLUMN `source_coach_id` INT UNSIGNED DEFAULT NULL
        COMMENT '发起移交的教练coach_id，NULL表示学员自主申请'
        AFTER `coach_id`,
    ADD COLUMN `transfer_reason` VARCHAR(200) DEFAULT NULL
        COMMENT '教练移交原因（学员主动申请时为NULL）'
        AFTER `source_coach_id`,
    ADD INDEX `idx_source_coach` (`source_coach_id`);
