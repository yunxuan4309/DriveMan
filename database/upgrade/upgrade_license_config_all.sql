-- ============================================
-- 补充 license_config 中缺失的车型科目配置
-- 执行: mysql -u root -proot driveman < database/upgrade/upgrade_license_config_all.sql
-- ============================================

-- C5 (残疾人专用小型自动挡载客汽车) — 同 C2
INSERT IGNORE INTO `license_config` (`license_type`, `subject`, `required_hours`, `exam_items`, `description`, `sort_order`, `exam_mode`, `coach_audit_required`, `cert_name`) VALUES
('C5', 1, 0,     NULL,                                                                               '科目一 道路交通安全法律、法规和相关知识', 1, 1, 1, NULL),
('C5', 2, 12,    '倒车入库,侧方停车,直角转弯,曲线行驶',                                                                  '科目二 场地驾驶技能（无坡道起步）', 2, 1, 1, NULL),
('C5', 3, 22,    '上车准备,起步,直线行驶,加减档操作,变更车道,靠边停车,直行通过路口,路口左转弯,路口右转弯,通过人行横道线,'
                 '通过学校区域,通过公共汽车站,会车,超车,掉头',                                                             '科目三 道路驾驶技能', 3, 1, 1, NULL),
('C5', 4, 0,     NULL,                                                                               '科目四 安全文明驾驶常识', 4, 1, 1, NULL);

-- C6 (轻型牵引挂车) — 仅科目二+科目四
INSERT IGNORE INTO `license_config` (`license_type`, `subject`, `required_hours`, `exam_items`, `description`, `sort_order`, `exam_mode`, `coach_audit_required`, `cert_name`) VALUES
('C6', 2, 10,    '桩考,曲线行驶,直角转弯',                                                             '科目二 场地驾驶技能', 1, 1, 1, NULL),
('C6', 4, 0,     NULL,                                                                               '科目四 安全文明驾驶常识', 2, 1, 1, NULL);

-- B2 (大型货车)
INSERT IGNORE INTO `license_config` (`license_type`, `subject`, `required_hours`, `exam_items`, `description`, `sort_order`, `exam_mode`, `coach_audit_required`, `cert_name`) VALUES
('B2', 1, 0,     NULL,                                                                               '科目一 道路交通安全法律、法规和相关知识', 1, 1, 1, NULL),
('B2', 2, 22,    '倒车入库,侧方停车,坡道定点停车和起步,直角转弯,曲线行驶,通过单边桥,通过限宽门,通过连续障碍,起伏路行驶,窄路掉头',  '科目二 场地驾驶技能', 2, 1, 1, NULL),
('B2', 3, 32,    '上车准备,起步,直线行驶,加减档操作,变更车道,靠边停车,直行通过路口,路口左转弯,路口右转弯,通过人行横道线,'
                 '通过学校区域,通过公共汽车站,会车,超车,掉头,夜间行驶,模拟山区公路,模拟高速公路',                                 '科目三 道路驾驶技能', 3, 1, 1, NULL),
('B2', 4, 0,     NULL,                                                                               '科目四 安全文明驾驶常识', 4, 1, 1, NULL);

-- A1 (大型客车)
INSERT IGNORE INTO `license_config` (`license_type`, `subject`, `required_hours`, `exam_items`, `description`, `sort_order`, `exam_mode`, `coach_audit_required`, `cert_name`) VALUES
('A1', 1, 0,     NULL,                                                                               '科目一 道路交通安全法律、法规和相关知识', 1, 1, 1, NULL),
('A1', 2, 22,    '倒车入库,侧方停车,坡道定点停车和起步,直角转弯,曲线行驶,通过单边桥,通过限宽门,窄路掉头,模拟高速公路',                    '科目二 场地驾驶技能', 2, 1, 1, NULL),
('A1', 3, 32,    '上车准备,起步,直线行驶,加减档操作,变更车道,靠边停车,直行通过路口,路口左转弯,路口右转弯,通过人行横道线,'
                 '通过学校区域,通过公共汽车站,会车,超车,掉头,夜间行驶,模拟山区公路,模拟高速公路',                                 '科目三 道路驾驶技能', 3, 1, 1, NULL),
('A1', 4, 0,     NULL,                                                                               '科目四 安全文明驾驶常识', 4, 1, 1, NULL);

-- A2 (牵引车)
INSERT IGNORE INTO `license_config` (`license_type`, `subject`, `required_hours`, `exam_items`, `description`, `sort_order`, `exam_mode`, `coach_audit_required`, `cert_name`) VALUES
('A2', 1, 0,     NULL,                                                                               '科目一 道路交通安全法律、法规和相关知识', 1, 1, 1, NULL),
('A2', 2, 22,    '倒车入库,侧方停车,坡道定点停车和起步,直角转弯,曲线行驶,通过单边桥,通过限宽门,窄路掉头,模拟高速公路',                    '科目二 场地驾驶技能', 2, 1, 1, NULL),
('A2', 3, 32,    '上车准备,起步,直线行驶,加减档操作,变更车道,靠边停车,直行通过路口,路口左转弯,路口右转弯,通过人行横道线,'
                 '通过学校区域,通过公共汽车站,会车,超车,掉头,夜间行驶,模拟山区公路,模拟高速公路',                                 '科目三 道路驾驶技能', 3, 1, 1, NULL),
('A2', 4, 0,     NULL,                                                                               '科目四 安全文明驾驶常识', 4, 1, 1, NULL);

-- A3 (城市公交车)
INSERT IGNORE INTO `license_config` (`license_type`, `subject`, `required_hours`, `exam_items`, `description`, `sort_order`, `exam_mode`, `coach_audit_required`, `cert_name`) VALUES
('A3', 1, 0,     NULL,                                                                               '科目一 道路交通安全法律、法规和相关知识', 1, 1, 1, NULL),
('A3', 2, 20,    '倒车入库,侧方停车,坡道定点停车和起步,直角转弯,曲线行驶,通过单边桥,通过限宽门',                                          '科目二 场地驾驶技能', 2, 1, 1, NULL),
('A3', 3, 30,    '上车准备,起步,直线行驶,加减档操作,变更车道,靠边停车,直行通过路口,路口左转弯,路口右转弯,通过人行横道线,'
                 '通过学校区域,通过公共汽车站,会车,超车,掉头,夜间行驶,模拟山区公路',                                                    '科目三 道路驾驶技能', 3, 1, 1, NULL),
('A3', 4, 0,     NULL,                                                                               '科目四 安全文明驾驶常识', 4, 1, 1, NULL);

-- D (普通三轮摩托车) — exam_mode=1 标准四科模式
INSERT IGNORE INTO `license_config` (`license_type`, `subject`, `required_hours`, `exam_items`, `description`, `sort_order`, `exam_mode`, `coach_audit_required`, `cert_name`) VALUES
('D', 1, 0,  NULL,               '科目一 道路交通安全法律、法规和相关知识', 1, 1, 0, NULL),
('D', 2, 8,  '桩考,坡道定点停车和起步,通过单边桥',  '科目二 场地驾驶技能', 2, 1, 0, NULL),
('D', 3, 10, '上车准备,起步,直线行驶,变更车道,靠边停车,通过路口,通过人行横道线,掉头', '科目三 道路驾驶技能', 3, 1, 0, NULL),
('D', 4, 0,  NULL,               '科目四 安全文明驾驶常识', 4, 1, 0, NULL);

-- E (普通二轮摩托车)
INSERT IGNORE INTO `license_config` (`license_type`, `subject`, `required_hours`, `exam_items`, `description`, `sort_order`, `exam_mode`, `coach_audit_required`, `cert_name`) VALUES
('E', 1, 0,  NULL,               '科目一 道路交通安全法律、法规和相关知识', 1, 1, 0, NULL),
('E', 2, 8,  '桩考,坡道定点停车和起步,通过单边桥',  '科目二 场地驾驶技能', 2, 1, 0, NULL),
('E', 3, 10, '上车准备,起步,直线行驶,变更车道,靠边停车,通过路口,通过人行横道线,掉头', '科目三 道路驾驶技能', 3, 1, 0, NULL),
('E', 4, 0,  NULL,               '科目四 安全文明驾驶常识', 4, 1, 0, NULL);

-- F (轻便摩托车)
INSERT IGNORE INTO `license_config` (`license_type`, `subject`, `required_hours`, `exam_items`, `description`, `sort_order`, `exam_mode`, `coach_audit_required`, `cert_name`) VALUES
('F', 1, 0,  NULL,               '科目一 道路交通安全法律、法规和相关知识', 1, 1, 0, NULL),
('F', 2, 6,  '桩考,通过单边桥',           '科目二 场地驾驶技能', 2, 1, 0, NULL),
('F', 3, 8,  '上车准备,起步,直线行驶,变更车道,靠边停车,通过路口', '科目三 道路驾驶技能', 3, 1, 0, NULL),
('F', 4, 0,  NULL,               '科目四 安全文明驾驶常识', 4, 1, 0, NULL);

-- M (轮式专用机械车) — 同特种车辆双科模式
INSERT IGNORE INTO `license_config` (`license_type`, `subject`, `required_hours`, `exam_items`, `description`, `sort_order`, `exam_mode`, `coach_audit_required`, `cert_name`) VALUES
('M', 1, 0,  '安全法规,机械基础知识',               '理论考试', 1, 2, 0, '轮式机械操作证'),
('M', 2, 20, '起步,行驶,装卸作业,停放',            '实操考试', 2, 2, 0, NULL);

-- N (无轨电车)
INSERT IGNORE INTO `license_config` (`license_type`, `subject`, `required_hours`, `exam_items`, `description`, `sort_order`, `exam_mode`, `coach_audit_required`, `cert_name`) VALUES
('N', 1, 0,  '安全法规,电车基础知识',               '理论考试', 1, 1, 0, '电车驾驶证'),
('N', 2, 20, '起步,直线行驶,变更车道,靠边停车,通过路口', '场地驾驶技能', 2, 1, 0, NULL),
('N', 3, 24, '上车准备,起步,直线行驶,变更车道,靠边停车,通过路口,通过人行横道线,掉头', '道路驾驶技能', 3, 1, 0, NULL),
('N', 4, 0,  NULL,               '安全文明驾驶常识', 4, 1, 0, NULL);

-- P (有轨电车)
INSERT IGNORE INTO `license_config` (`license_type`, `subject`, `required_hours`, `exam_items`, `description`, `sort_order`, `exam_mode`, `coach_audit_required`, `cert_name`) VALUES
('P', 1, 0,  '安全法规,有轨电车基础知识',            '理论考试', 1, 1, 0, '有轨电车驾驶证'),
('P', 2, 18, '起步,行驶,进出站,通过路口',           '场地驾驶技能', 2, 1, 0, NULL),
('P', 3, 22, '上车准备,起步,行驶,进出站,通过路口,通过人行横道线,掉头', '道路驾驶技能', 3, 1, 0, NULL),
('P', 4, 0,  NULL,               '安全文明驾驶常识', 4, 1, 0, NULL);
