-- ============================================================
-- exam_registration 表新增 cert_no 字段
-- 证书编号：特种车辆（N1/N2/N3）双科通过后自动生成，标准车辆为 NULL
-- ============================================================
ALTER TABLE `exam_registration`
ADD COLUMN `cert_no` VARCHAR(50) DEFAULT NULL COMMENT '证书编号（特种车辆双科通过后生成）'
AFTER `is_retake`;
