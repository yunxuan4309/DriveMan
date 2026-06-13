-- ============================================
-- 考试报名表 + 特种车辆考试记录表 增加成绩截图关联字段
-- 学员考试后上传成绩截图，管理员录入成绩时关联文件
-- ============================================

-- 1. 普通小汽车考试（C1/C2/B1 等，科目1-4）
ALTER TABLE `exam_registration`
    ADD COLUMN `file_id` INT UNSIGNED DEFAULT NULL COMMENT '关联文件ID（学员上传的成绩截图）'
    AFTER `pass_status`;

-- 2. 特种车辆考试（N1/N2/N3，理论+实操）
ALTER TABLE `special_exam_record`
    ADD COLUMN `file_id` INT UNSIGNED DEFAULT NULL COMMENT '关联文件ID（学员上传的成绩截图）'
    AFTER `pass_status`;
