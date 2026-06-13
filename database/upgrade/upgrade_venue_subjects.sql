-- ============================================
-- 场地增加科目支持 + 排班增加科目字段
-- 训练场地按科目区分（科目二=封闭场地，科目三=道路）
-- 排班时校验场地是否支持该科目
-- ============================================

DROP PROCEDURE IF EXISTS `upgrade_venue_subjects`;
DELIMITER $$
CREATE PROCEDURE `upgrade_venue_subjects`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'driveman' AND TABLE_NAME = 'venue' AND COLUMN_NAME = 'subjects') THEN
        ALTER TABLE `venue` ADD COLUMN `subjects` VARCHAR(20) DEFAULT NULL COMMENT '支持训练的科目，逗号分隔如"2,3"，NULL表示不限'
            AFTER `supported_types`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = 'driveman' AND TABLE_NAME = 'coach_schedule' AND COLUMN_NAME = 'subject') THEN
        ALTER TABLE `coach_schedule` ADD COLUMN `subject` TINYINT DEFAULT NULL COMMENT '培训科目: 2-科目二, 3-科目三'
            AFTER `license_type`;
    END IF;
END$$
DELIMITER ;
CALL `upgrade_venue_subjects`();
DROP PROCEDURE IF EXISTS `upgrade_venue_subjects`;
