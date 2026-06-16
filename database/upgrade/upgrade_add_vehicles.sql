-- ============================================================
-- 补充教练车数据：每种车型各增加 2 辆
--
-- 执行前请确认无重复车牌号：
--   SELECT plate_number, COUNT(*) FROM vehicle GROUP BY plate_number HAVING COUNT(*) > 1;
-- ============================================================

-- C1（已有 6 辆，追加 2 辆 → 8 辆）
INSERT INTO vehicle (plate_number, vehicle_type, brand, model, seats, status, remarks) VALUES
('京A·C1008', 'C1', '大众', '桑塔纳', 5, 1, 'C1 教练车'),
('京A·C1009', 'C1', '大众', '捷达',   5, 1, 'C1 教练车');

-- C2（已有 4 辆，追加 2 辆 → 6 辆）
INSERT INTO vehicle (plate_number, vehicle_type, brand, model, seats, status, remarks) VALUES
('京A·C2005', 'C2', '大众', '朗逸', 5, 1, 'C2 自动挡教练车'),
('京A·C2006', 'C2', '丰田', '卡罗拉', 5, 1, 'C2 自动挡教练车');

-- B1（已有 1 辆，追加 2 辆 → 3 辆）
INSERT INTO vehicle (plate_number, vehicle_type, brand, model, seats, status, remarks) VALUES
('京A·B1002', 'B1', '丰田', '柯斯达', 19, 1, 'B1 中型客车教练车'),
('京A·B1003', 'B1', '金杯', '大海狮', 15, 1, 'B1 中型客车教练车');

-- N1（已有 2 辆，追加 2 辆 → 4 辆）
INSERT INTO vehicle (plate_number, vehicle_type, brand, model, seats, status, remarks) VALUES
('京A·N1003', 'N1', '杭叉', 'CPD35', 2, 1, 'N1 叉车教练车'),
('京A·N1004', 'N1', '合力', 'CPD40', 2, 1, 'N1 叉车教练车');

-- N2（已有 2 辆，追加 2 辆 → 4 辆）
INSERT INTO vehicle (plate_number, vehicle_type, brand, model, seats, status, remarks) VALUES
('京A·N2003', 'N2', '小松', 'PC220', 2, 1, 'N2 挖掘机教练车'),
('京A·N2004', 'N2', '卡特彼勒', '330D', 2, 1, 'N2 挖掘机教练车');

-- N3（已有 1 辆，追加 2 辆 → 3 辆）
INSERT INTO vehicle (plate_number, vehicle_type, brand, model, seats, status, remarks) VALUES
('京A·N3002', 'N3', '徐工', 'XS263J', 2, 1, 'N3 压路机教练车'),
('京A·N3003', 'N3', '三一', 'SSR360', 2, 1, 'N3 压路机教练车');
