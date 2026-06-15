-- ============================================
-- 增驾费用标准
-- 说明: 为各增驾目标车型添加套餐费用标准
-- 前置: fee_standard 表已存在
-- 执行: mysql -u root -proot driveman < upgrade_upgrade_fee.sql
-- ============================================

INSERT IGNORE INTO `fee_standard` (`license_type`, `subject`, `amount`, `description`) VALUES
('C6', NULL, 1500.00, 'C6增驾套餐'),
('B1', NULL, 5000.00, 'B1增驾套餐'),
('B2', NULL, 6000.00, 'B2增驾套餐'),
('A1', NULL, 8000.00, 'A1增驾套餐'),
('A2', NULL, 7000.00, 'A2增驾套餐'),
('A3', NULL, 6000.00, 'A3增驾套餐'),
('D', NULL, 1000.00, 'D增驾套餐'),
('E', NULL, 800.00, 'E增驾套餐'),
('C5', NULL, 4000.00, 'C5增驾套餐');
