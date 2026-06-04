# database — 数据库脚本目录

## 目录结构

```
database/
├── README.md                       # 本文件
├── full/                           # 完整建库（全量初始化，按编号从上往下执行）
│   ├── 00_create_database.sql      # 创建数据库（DROP + CREATE + USE）
│   ├── 01_schema.sql               # 全部 15 张表的建表语句
│   └── 02_init_data.sql            # 初始化基础数据（用户、教练、车型配置、考场等）
├── upgrade/                        # 增量升级（对已有数据库执行升级）
│   ├── add_constraints.sql         # 外键约束（建议开发后期再启用）
│   ├── upgrade_license_type.sql    # 小汽车车型多车型支持升级
│   ├── upgrade_basic_data.sql      # 基础数据优化（exam_mode、考场、特种车辆）
│   ├── upgrade_coach_application.sql # coach_application 表扩展——支持教练主动移交学员
│   └── upgrade_retake_fee.sql       # 二次培训流程：is_retake + retake_training_record 表
└── test/
    └── test_data.sql               # 测试数据补充
```

## 执行方式

### 全新建库（首次部署）

按编号顺序执行 `full/` 下的文件：

```bash
mysql -u root -proot < database/full/00_create_database.sql
mysql -u root -proot driveman < database/full/01_schema.sql
mysql -u root -proot driveman < database/full/02_init_data.sql
```

或一键执行原入口脚本（等效于以上三条命令的合并）：

```bash
mysql -u root -proot < database/init_script.sql
```

### 已有数据库升级（不丢数据）

按需执行 `upgrade/` 下的文件：

```bash
mysql -u root -proot driveman < database/upgrade/upgrade_license_type.sql
mysql -u root -proot driveman < database/upgrade/upgrade_basic_data.sql
mysql -u root -proot driveman < database/upgrade/upgrade_coach_application.sql
mysql -u root -proot driveman < database/upgrade/upgrade_retake_fee.sql
mysql -u root -proot driveman < database/upgrade/add_constraints.sql
```

### 补充测试数据

```bash
mysql -u root -proot driveman < database/test/test_data.sql
```
