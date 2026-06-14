-- ============================================
-- 增量升级: 考试报名截止天数 + 挂科冷静期天数配置项
-- 执行方式: mysql -u root -proot driveman < database/upgrade/upgrade_exam_deadline_config.sql
-- 日期: 2026-06-14
-- 说明:
--   exam_registration_deadline_days: 考试报名截止天数（考试前N天停止报名），默认2天
--   exam_retake_cooldown_days: 挂科后冷静期天数（不合格后N天内不可重新报名同科目），默认7天
-- ============================================

INSERT IGNORE INTO `config` (`config_key`, `config_value`, `description`) VALUES
('exam_registration_deadline_days', '2', '考试报名截止天数（考试前N天停止报名）'),
('exam_retake_cooldown_days', '7', '挂科后冷静期天数（不合格后N天内不可重新报名同科目）');
