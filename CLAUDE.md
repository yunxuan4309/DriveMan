# DriveMan — 驾校报名管理系统

## 项目定位

驾校报名管理系统，面向驾校的学员报名、教练管理、课程安排、考试预约等核心业务场景。系统分为三种角色（学员/教练/管理员），覆盖从学员注册报名、教练选择、课程预约、学时记录到考试报名、成绩录入的完整业务流程。

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 3.2.5 |
| ORM | MyBatis-Plus | 3.5.9 |
| 数据库 | MySQL 8 | — |
| 缓存 | Redis | — |
| 接口文档 | Knife4j (SpringDoc OpenAPI) | 4.5.0 |
| PDF 生成 | iText 7 Community | 7.2.5 |
| JWT | jjwt | 0.12.5 |
| 密码加密 | Spring Security Crypto (BCrypt) | 6.2.4 |
| 构建工具 | Maven | 3.9.15 |

## 项目结构

```
DriveMan/
├── database/
│   ├── README.md                    # 目录说明 + 执行顺序
│   ├── init_script.sql              # 【入口】一键全量建库（合并 full/ 下三个文件）
│   ├── full/                        # 完整建库（按编号顺序执行）
│   │   ├── 00_create_database.sql   #   创建数据库
│   │   ├── 01_schema.sql            #   全部 23 张表的建表语句
│   │   └── 02_init_data.sql         #   初始化基础数据
│   ├── upgrade/                     # 增量升级（按需执行，不丢数据）
│   │   ├── add_constraints.sql      #   外键约束（建议开发后期启用）
│   │   ├── upgrade_license_type.sql #   小汽车车型升级
│   │   ├── upgrade_basic_data.sql   #   基础数据优化（车型模式+考场+特种车）
│   │   ├── upgrade_version_lock.sql #   乐观锁版本号列
│   │   ├── upgrade_file_system.sql  #   文件系统重构（biz_type/biz_id）
│   │   ├── upgrade_payment_record.sql #   支付记录表
│   │   ├── upgrade_familiarization_record.sql # 合场记录表
│   │   ├── upgrade_coach_vehicle_application.sql # 教练准教车型变更申请表
│   │   ├── upgrade_coach_application.sql # coach_application 表扩展
│   │   ├── upgrade_retake_fee.sql   #   二次培训费字段
│   │   ├── upgrade_physical_exam_license.sql # 体检+增驾申请表
│   │   ├── upgrade_venue_unified.sql   # 场地统一管理（exam_venue → venue）
│   │   ├── upgrade_schedule_vehicle.sql # 排班管理 + 教练车车辆管理
│   │   └── upgrade_disability_special.sql # 残疾信息 + 特殊人群记录表
│   └── test/
│       └── test_data.sql            # 测试数据补充
│
├── src/main/java/com/homework/driveman/
│   ├── DriveManApplication.java              # Spring Boot 启动入口
│   │
│   ├── config/                               # 全局配置
│   │   ├── JwtInterceptor.java               #   JWT 鉴权拦截器
│   │   ├── Knife4jConfiguration.java         #   Knife4j 接口文档
│   │   ├── MybatisPlusConfiguration.java     #   MyBatis-Plus 分页 + MapperScan
│   │   ├── MyMetaObjectHandler.java          #   自动填充 createTime/updateTime
│   │   ├── RedisConfig.java                  #   Redis 序列化配置
│   │   ├── RequireRole.java                  #   角色权限注解 @RequireRole
│   │   ├── ValidationConfiguration.java      #   Validator 快速失败
│   │   └── WebMvcConfiguration.java          #   跨域 + 拦截器 + 静态资源
│   │
│   ├── constant/                             # 常量类
│   │   └── AppointmentStatus.java            #   约课状态常量
│   │
│   ├── dto/                                  # 数据传输对象（8 个）
│   │   ├── AppointmentActionDTO.java         #   约课操作 DTO
│   │   ├── AvailableTimeDTO.java             #   空闲时间 DTO
│   │   ├── RecordHoursDTO.java              #   录入学时 DTO
│   │   ├── ChangePasswordDTO.java           #   修改密码 DTO
│   │   ├── CoachProfileUpdateDTO.java       #   教练资料更新 DTO
│   │   ├── CoachRegisterDTO.java            #   教练注册 DTO
│   │   ├── TimeSlotDTO.java                 #   时间段 DTO
│   │   └── UpdateTimeSlotsDTO.java          #   批量更新时间段 DTO
│   │
│   ├── vo/                                   # 视图对象
│   │   ├── CoachRatingVO.java               #   教练评分 VO
│   │   ├── CoachWorkloadVO.java             #   教练工作量 VO
│   │   └── StudentInfoVO.java               #   学员信息 VO
│   │
│   ├── controller/                           # REST 控制器（31 个，30 个活跃，1 个已禁用）
│   │   ├── LoginController.java              #   认证登录
│   │   ├── UserController.java               #   用户管理
│   │   ├── CoachController.java              #   教练管理
│   │   ├── CoachRegisterController.java      #   教练自助注册
│   │   ├── StudentController.java            #   学员管理（独立拆分）
│   │   ├── CoachPortalController.java        #   教练端门户（23 个端点）
│   │   ├── CoachPortalController1.java       #   [已禁用] 旧版，功能已合并
│   │   ├── AppointmentController.java        #   约课管理
│   │   ├── RegistrationController.java       #   报名审核（含 PDF 生成）
│   │   ├── FileController.java               #   文件上传下载
│   │   ├── ExamSessionController.java        #   考试场次管理
│   │   ├── ExamRegistrationController.java   #   考试报名管理
│   │   ├── CoachApplicationController.java   #   教练申请审核
│   │   ├── CoachAssignmentController.java    #   教练分配
│   │   ├── FeeStandardController.java        #   费用标准管理
│   │   ├── LicenseConfigController.java      #   驾照车型配置
│   │   ├── NoticeController.java             #   通知公告管理
│   │   ├── VenueController.java              #   场地管理
│   │   ├── CoachVehicleApplicationController.java # 教练准教车型变更审核
│   │   ├── ProgressController.java           #   学习进度查询
│   │   ├── StatisticsController.java         #   统计报表
│   │   ├── PaymentController.java            #   支付管理
│   │   ├── FamiliarizationController.java    #   合场管理
│   │   ├── RetakeTrainingController.java     #   二次培训管理
│   │   ├── PhysicalExamController.java       #   体检申请
│   │   ├── LicenseUpgradeController.java     #   增驾申请
│   │   ├── CoachScheduleController.java      #   排班管理（管理员端审核）
│   │   ├── VehicleController.java            #   车辆管理
│   │   ├── DisabilityInfoController.java     #   残疾信息管理
│   │   ├── SpecialPersonRecordController.java #   特殊人群记录管理
│   │   └── SpecialExamController.java        #   特种车辆考试记录
│   │
│   ├── entity/                               # 数据实体（24 个）
│   │   ├── User.java                         #   用户表
│   │   ├── Coach.java                        #   教练扩展表
│   │   ├── StudentCoach.java                 #   学员-教练关联表
│   │   ├── Appointment.java                  #   约课表（含 schedule_id）
│   │   ├── TrainingRecord.java               #   学时记录表
│   │   ├── ExamSession.java                  #   考试场次表
│   │   ├── ExamRegistration.java             #   考试报名表
│   │   ├── CoachApplication.java             #   教练选择/移交申请表
│   │   ├── FeeStandard.java                  #   费用标准表
│   │   ├── LicenseConfig.java                #   驾照类型配置表
│   │   ├── Notice.java                       #   通知公告表
│   │   ├── File.java                         #   文件表
│   │   ├── Venue.java                        #   场地统一管理表
│   │   ├── SpecialExamRecord.java            #   特种车考试记录表
│   │   ├── CoachVehicleApplication.java      #   教练准教车型变更申请表
│   │   ├── PaymentRecord.java                #   支付记录表
│   │   ├── FamiliarizationRecord.java        #   合场记录表
│   │   ├── RetakeTrainingRecord.java         #   二次培训记录表
│   │   ├── PhysicalExam.java                 #   体检申请表
│   │   ├── LicenseUpgrade.java               #   增驾申请表
│   │   ├── CoachSchedule.java                #   教练排班表
│   │   ├── Vehicle.java                      #   教练车表
│   │   ├── DisabilityInfo.java               #   残疾信息表
│   │   └── SpecialPersonRecord.java          #   特殊人群记录表
│   │
│   ├── mapper/                               # Mapper 接口（25 个，均继承 BaseMapper）
│   │   ├── UserMapper.java
│   │   ├── CoachMapper.java
│   │   ├── StudentCoachMapper.java
│   │   ├── AppointmentMapper.java
│   │   ├── TrainingRecordMapper.java
│   │   ├── ExamSessionMapper.java
│   │   ├── ExamRegistrationMapper.java
│   │   ├── CoachApplicationMapper.java
│   │   ├── FeeStandardMapper.java
│   │   ├── LicenseConfigMapper.java
│   │   ├── NoticeMapper.java
│   │   ├── FileMapper.java
│   │   ├── VenueMapper.java
│   │   ├── SpecialExamRecordMapper.java
│   │   ├── ConfigMapper.java
│   │   ├── PaymentRecordMapper.java
│   │   ├── FamiliarizationRecordMapper.java
│   │   ├── RetakeTrainingRecordMapper.java
│   │   ├── PhysicalExamMapper.java
│   │   ├── LicenseUpgradeMapper.java
│   │   ├── CoachVehicleApplicationMapper.java
│   │   ├── CoachScheduleMapper.java
│   │   ├── VehicleMapper.java
│   │   ├── DisabilityInfoMapper.java
│   │   └── SpecialPersonRecordMapper.java
│   │
│   ├── service/                           # 业务层（27 个接口 + 27 个实现）
│   │   ├── IAppointmentService.java
│   │   ├── ICoachService.java
│   │   ├── ICoachPortalService.java       #   教练端门户
│   │   ├── ICoachScheduleService.java     #   排班管理
│   │   ├── IExamRegistrationService.java
│   │   ├── IExamSessionService.java
│   │   ├── IFeeStandardService.java       #   费用标准
│   │   ├── IFileService.java
│   │   ├── ILicenseConfigService.java     #   驾照配置
│   │   ├── INoticeService.java            #   通知公告
│   │   ├── IPdfService.java
│   │   ├── IProgressService.java          #   学习进度
│   │   ├── IStatisticsService.java        #   统计报表
│   │   ├── ITrainingRecordService.java    #   学时记录
│   │   ├── IUserService.java
│   │   ├── IVenueService.java              #   场地管理
│   │   ├── IVehicleService.java            #   车辆管理
│   │   ├── ISpecialExamRecordService.java        #   特种车辆考试记录
│   │   ├── IPaymentRecordService.java            #   支付管理
│   │   ├── IFamiliarizationRecordService.java    #   合场管理
│   │   ├── IRetakeTrainingService.java            #   二次培训
│   │   ├── IPhysicalExamService.java              #   体检申请
│   │   ├── ILicenseUpgradeService.java            #   增驾申请
│   │   ├── ICoachVehicleApplicationService.java  #   教练准教车型变更
│   │   ├── IDisabilityInfoService.java            #   残疾信息管理
│   │   ├── ISpecialPersonRecordService.java       #   特殊人群记录
│   │   │
│   │   └── impl/
│   │       ├── AppointmentServiceImpl.java
│   │       ├── CoachPortalServiceImpl.java
│   │       ├── CoachScheduleServiceImpl.java #   排班冲突检测/场地容量校验
│   │       ├── CoachServiceImpl.java       #   教练推荐（FIND_IN_SET）
│   │       ├── ExamRegistrationServiceImpl.java
│   │       ├── ExamSessionServiceImpl.java
│   │       ├── FeeStandardServiceImpl.java
│   │       ├── FileServiceImpl.java        #   文件存储（本地磁盘）
│   │       ├── LicenseConfigServiceImpl.java
│   │       ├── NoticeServiceImpl.java
│   │       ├── PdfServiceImpl.java         #   PDF 生成（iText 7）
│   │       ├── ProgressServiceImpl.java
│   │       ├── StatisticsServiceImpl.java
│   │       ├── TrainingRecordServiceImpl.java
│   │       ├── VehicleServiceImpl.java      #   车辆 CRUD
│   │       ├── PaymentRecordServiceImpl.java
│   │       ├── FamiliarizationRecordServiceImpl.java
│   │       ├── CoachVehicleApplicationServiceImpl.java
│   │       ├── RetakeTrainingServiceImpl.java      #   二次培训
│   │       ├── VenueServiceImpl.java              #   场地管理
│   │       ├── PhysicalExamServiceImpl.java        #   体检申请
│   │       ├── LicenseUpgradeServiceImpl.java      #   增驾申请
│   │       ├── DisabilityInfoServiceImpl.java      #   残疾信息审核
│   │       ├── SpecialPersonRecordServiceImpl.java #   特殊人群禁驾期计算
│   │       └── UserServiceImpl.java
│   │
│   ├── utils/
│   │   ├── JwtUtils.java                   #   JWT 签发/解析
│   │   └── CurrentUser.java                #   当前用户上下文 DTO
│   │
│   ├── exception/
│   │   ├── ServiceException.java           #   业务异常
│   │   └── GlobalExceptionHandler.java     #   全局异常处理
│   │
│   └── web/
│       ├── JsonResult.java                 #   统一响应封装
│       └── ServiceCode.java                #   业务状态码枚举
│
├── src/main/resources/
│   └── application.yaml                    # 应用配置
│
├── ...接口文档.md                          # 接口对接文档（拆分模块化）
├── README.md
└── HELP.md
```

## 核心功能模块

### 1. 认证与权限
- **登录** `POST /login` — 用户名/密码登录，返回 JWT Token（7 天有效）
- **教练自助注册** `POST /coach/register` — 公开接口，教练自行提交资质信息，管理员审核
- **拦截器** — `JwtInterceptor` 拦截除公开路径外的所有请求，校验 Token
- **角色注解** — `@RequireRole({1,3})` 方法级权限控制（1=学员, 2=教练, 3=管理员）
- ✅ `@RequireRole` 已应用到 26 个 Controller 共 138 处敏感接口上

### 2. 用户管理
- 学员/教练/管理员的 CRUD，用户状态审核流程（待审核 → 通过/不通过）
- 学员管理独立 `StudentController`，支持分页查询
- 学员端个人信息完善
- 教练端修改密码（`PUT /coach-portal/password`，BCrypt 验证旧密码）

### 3. 报名审核
- `PUT /registrations/{userId}/audit` — 审核通过时自动生成两份 PDF（报名表 + 准考证）
- PDF 使用 iText 7 生成，支持中文字体

### 4. 教练管理
- 教练信息 CRUD，评分、执教年限、准教车型管理
- 教练分配：自动推荐（按车型匹配 + 评分排序）+ 管理员手动分配 + 学员申请
- `FIND_IN_SET` 处理逗号分隔的准教车型字段
- 教练端门户（`/coach-portal`）：查看学员列表、考试报名、录入学时、约课确认/拒绝、时间段管理（结构化 CRUD）、工作量统计、评分查看、准教车型变更申请、移交学员、二次培训查看、个人资料编辑、密码修改 — 共 **23 个端点**

### 5. 约课管理
- 学员预约教练课程，课程时间管理，取消约课
- 教练确认/拒绝约课
- 约课关联排班（`schedule_id`），支持按排班自动校验容量

### 6. 考试管理
- 考试场次发布/修改（含名额管理）
- 学员报名考试，管理员审核（扣减名额），录入成绩（合格分数线从 config 表读取）
- 补考次数自动累计

### 7. 费用管理
- 收费标准 CRUD，按驾照类型配置费用项目
- 仅管理员可管理
- ✅ 完全实现

### 8. 驾照车型配置
- 驾照类型（C1/C2/B1/N1/N2/N3）配置管理
- 仅管理员可管理
- ✅ 实体字段与数据库完全同步（exam_mode, coach_audit_required, cert_name 均已在实体中）

### 9. 通知公告
- 通知公告的发布和管理
- 仅管理员可管理

### 10. 学习进度
- 学员端查询个人学习进度
- 学时记录统计
- 数据驱动，按 license_config 表配置动态展示各科进度

### 11. 统计报表
- 报名趋势统计（近30天折线图）
- 各科目月度考试通过率趋势（多线折线图）
- 教练效能排名（柱状图，含评分/带教学员数/通过率明细）
- 收入看板（近12月收入趋势柱状图 + 当月收入来源饼图 + 收支汇总）

### 12. 文件管理
- 本地磁盘存储，按类型分子目录（id_card_front, id_card_back, physical_exam 等）
- 文件上传（5MB 限制）、下载、静态资源访问（`/uploads/**`）

### 13. 支付管理
- 报名/考试报名审核通过时自动生成待支付账单
- 学员端查看我的账单 + 模拟支付
- 管理员端欠费清单 + 确认支付 + 退款 + 手动创建记录
- 收入看板（月度趋势、来源分布、汇总统计）

### 14. 合场管理
- 学员申请合场（教练车需绑定教练陪同 / 考试车由考场提供陪练）
- 系统自动按 fee_standard 定价并生成支付账单
- 管理员安排时间、标记完成、取消合场
- 两种用车模式分别定价

### 15. 排班管理（新增）
- 教练提交排班申请（选择车辆、训练场地、时间、车型）
- 管理员审核（通过/拒绝）
- 冲突检测：同一车辆/同一教练在同一时间段不能有两个已通过排班
- 场地容量校验：不超过场地 `max_vehicles` 上限
- 学员端：查看绑定教练的可用排班，按排班预约课程
- 数据表：`coach_schedule` + `vehicle` + `venue` 扩展字段

### 16. 车辆管理（新增）
- 教练车车队管理（车牌唯一、车型、品牌、座位数）
- 状态管理：空闲/使用中/维修/报废
- 仅管理员可 CRUD，教练可查询可用车辆
- 初始数据：6 辆教练车（含 C1/C2/N1/N2 四种类型）

### 17. 残疾信息管理（新增）
- C5 准驾车型支持：学员提交残疾信息及残疾人证扫描件
- 管理员审核（通过/不通过）
- 每人仅能有一条记录（防重复提交）
- 残疾类型：右下肢/双下肢/右手/听力/左手/其他

### 18. 特殊人群记录（新增）
- 记录学员的犯罪、酒驾、毒驾、肇事逃逸等 6 类记录
- 禁驾期自动计算（固定年限/终生禁驾）
- 学员端查询本人的禁令状态
- 管理员审核 + 查询任意用户的禁令状态

### 19. 教练端增强功能
- 结构化时间段管理（`TimeSlotDTO`），替代原始 JSON 编辑
  - 按星期几分组，支持完整 CRUD（GET/PUT/POST/DELETE）
  - 存储在 `coach.available_time` JSON 列
- 个人资料编辑（白名单字段：姓名/电话/地址/头像/执教年限）
- 密码修改（BCrypt 验证旧密码）

## API 概览

| 模块 | 基础路径 | 角色权限 |
|------|---------|----------|
| 认证 | `/login` | 公开 |
| 教练注册 | `/coach/register` | 公开 |
| 用户 | `/users` | 全部（登录） |
| 学员 | `/students` | 管理员 |
| 教练 | `/coaches` | 全部（登录） |
| 教练端 | `/coach-portal` | 教练 |
| 约课 | `/appointments` | 学员/管理员 |
| 排班 | `/schedules` | 学员/管理员 |
| 报名审核 | `/registrations` | 管理员 |
| 考试场次 | `/exam-sessions` | 全部（登录） |
| 考试报名 | `/exam-registrations` | 学员/管理员 |
| 教练端-考试报名 | `/coach-portal/exam-registrations` | 教练 |
| 场地管理 | `/venues` | 管理员 |
| 车辆管理 | `/vehicles` | 管理员/教练（查询） |
| 教练申请 | `/coach-applications` | 学员/管理员 |
| 教练分配 | `/coach-assignments` | 管理员 |
| 教练分配-按教练查学员 | `/coach-assignments/coach/{coachId}/students` | 管理员 |
| 教练端-移交学员 | `/coach-portal/student-transfers` | 教练 |
| 费用标准 | `/fee-standards` | 管理员 |
| 车型配置 | `/license-configs` | 管理员 |
| 通知公告 | `/notices` | 全部（登录） |
| 学习进度 | `/progress` | 全部（登录） |
| 统计报表 | `/statistics` | 管理员 |
| 支付管理 | `/payment-records` | 学员/管理员 |
| 合场管理 | `/familiarizations` | 学员/管理员 |
| 文件 | `/files` | 全部（登录），删除限管理员 |
| 特种车辆考试 | `/special-exam-records` | 管理员 |
| 教练准教车型变更 | `/coach-portal/vehicle-applications` | 教练 |
| 教练准教车型变更审核 | `/coach-vehicle-applications` | 管理员 |
| 二次培训管理 | `/retake-trainings` | 学员/管理员 |
| 教练端-二次培训 | `/coach-portal/retake-trainings` | 教练 |
| 体检申请 | `/physical-exams` | 学员/管理员 |
| 增驾申请 | `/license-upgrades` | 学员/管理员 |
| 残疾信息 | `/disability-info` | 学员/管理员 |
| 特殊人群记录 | `/special-person-records` | 学员/管理员 |

**统一响应格式:** `JsonResult<T>` — `{ state: Integer, message: String | null, data: T }`

**Token 传递方式:** `Authorization: Bearer <token>`

详细接口说明见各模块文档（`*接口文档.md`）。

## 运行环境

| 配置项 | 值 |
|--------|-----|
| 端口 | 9500 |
| MySQL | `localhost:3306 / driveman / root:root` |
| Redis | `localhost:6379 / db=0` |
| 文件存储 | `./upload-files/`（项目根目录） |
| API 文档 | `http://localhost:9500/doc.html` |

### 启动步骤

```bash
# 1. 初始化数据库（三选一）

# 方式一：一键全量建库（推荐）
mysql -u root -proot < database/init_script.sql

# 方式二：分步执行（便于阅读各模块）
mysql -u root -proot < database/full/00_create_database.sql
mysql -u root -proot driveman < database/full/01_schema.sql
mysql -u root -proot driveman < database/full/02_init_data.sql

# 方式三：已有数据库执行增量升级（按需执行）
mysql -u root -proot driveman < database/upgrade/upgrade_schedule_vehicle.sql
mysql -u root -proot driveman < database/upgrade/upgrade_venue_unified.sql
mysql -u root -proot driveman < database/upgrade/upgrade_basic_data.sql
# 方式三也可升级脚本逐个执行，以下为最新表补充：
mysql -u root -proot driveman < database/upgrade/upgrade_disability_special.sql

# 2. 确保 MySQL 和 Redis 已启动

# 3. 启动项目
mvn spring-boot:run

# 4. 访问接口文档
# http://localhost:9500/doc.html
```

### 测试账号

| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 管理员 |
| coach1 | admin123 | 教练（张教练，准教 C1） |
| coach2 | admin123 | 教练（李教练，准教 C1,C2） |
| student1 | admin123 | 学员（王小明，C1） |
| student2 | admin123 | 学员（李芳，C2） |

## 当前开发状态

### 已完成
- [x] Spring Boot + MyBatis-Plus 框架搭建
- [x] 23 张数据库表结构设计 + 初始化数据 + 测试数据（init_script.sql）
- [x] 15 份增量升级脚本（upgrade/ 目录）
- [x] 三层架构（Controller/Service/Mapper）代码生成
- [x] JWT 登录认证 + Token 校验拦截器
- [x] `@RequireRole` 角色权限注解（已应用到 26 个 Controller 共 138 处敏感接口）
- [x] 用户 CRUD 管理 + 学员管理独立控制器
- [x] 教练 CRUD + 自动推荐（FIND_IN_SET）+ 分配 + 解绑
- [x] 学员申请教练 + 管理员审核
- [x] 约课管理（预约/取消/教练确认拒绝）
- [x] 学时记录管理
- [x] 报名审核 + PDF 报名表/准考证生成（iText 7）
- [x] 文件上传/下载/预览/静态访问（本地磁盘存储，支持 `?preview=true` 浏览器预览）
- [x] 文件系统重构：`biz_type` + `biz_id` 业务关联、`file_size` + `mime_type` 元数据、三端权限矩阵
- [x] 培训记录表 PDF 生成（学员点击生成，含学时汇总、教练信息）
- [x] 文件查询多维度过滤（`bizType`、`fileType`、`keyword` 文件名搜索）
- [x] 考试场次 CRUD + 名额管理 + 考试报名审核 + 成绩录入 + 补考
- [x] 教练端门户（查看学员、录入学时、确认约课、时间段管理、工作量统计、评分查看）
- [x] 费用标准管理（CRUD + 按车型查询）
- [x] 驾照车型配置 CRUD（Controller + 实体完全同步）
- [x] 通知公告管理
- [x] 学习进度查询（小汽车 4 科 + 特种车辆 2 科模式）
- [x] 统计报表重构（报名趋势/分科通过率趋势/教练效能排名/收入看板）
- [x] 全局异常处理 + 统一响应格式
- [x] 全局跨域配置
- [x] Knife4j 接口文档
- [x] 通用配置（分页、自动填充、逻辑删除、Redis 模板、Validator）
- [x] 接口文档按模块拆分（多份独立文档）
- [x] 数据库目录拆分（full/ + upgrade/ + test/）
- [x] MyBatis-Plus 乐观锁（`@Version` + `OptimisticLockerInnerInterceptor`）
- [x] 管理员端考试报名列表补充学员姓名和场次信息（`pageWithDetails`）
- [x] 教练端查看考试报名（`GET /coach-portal/exam-registrations`）
- [x] 考试报名安全加固（学员身份校验、乐观锁防并发冲突）
- [x] 考试场次接口输入校验（日期、名额、车型等参数合法性）
- [x] 学员查询考试报名记录补充场次信息（考试日期、地点）
- [x] 教练准教车型变更申请 + 管理员审核流程（含车型合法性校验）
- [x] `init_script.sql` 与 `01_schema.sql` 对齐（补全 version 列）
- [x] 支付管理系统（payment_record 表 → 实体 → Mapper → Service → Controller）
- [x] 合场功能（familiarization_record 表 → 实体 → Mapper → Service → Controller）
- [x] 账单自动生成（报名审核/考试报名审核通过时自动生成待支付账单）
- [x] 收入看板（月度趋势 / 来源分布饼图 / 收支汇总统计）
- [x] 欠费清单（含学员姓名/电话/车型信息）
- [x] 学员申请教练增加重复提交校验 + 审核通过时自动解绑旧教练
- [x] 教练主动移交学员给其他教练（含审核流程）
- [x] 管理员按教练查询名下学员
- [x] 学员申请记录查询返回教练姓名等可读字段
- [x] 二次培训（补考培训）完整流程：全包免缴费 / 非全包管理员设定培训费
- [x] 体检申请流程：学员提交 → 管理员审核 → 录入结果（+ 报告文件）
- [x] 增驾申请流程：学员申请 → 管理员审核 → 录入成绩（通过自动更新车型）
- [x] 场地统一管理：venue 表三种场地类型（考场/训练场地/体检地点）
- [x] 教练自助注册（`POST /coach/register` 公开接口）
- [x] 教练端个人资料编辑 + 密码修改
- [x] 教练端结构化时间段管理（TimeSlotDTO CRUD，替代原始 JSON）
- [x] 车辆管理（vehicle 表 → 实体 → Mapper → Service → Controller）
- [x] 排班管理（coach_schedule 表 → 冲突检测 + 场地容量校验）
- [x] 残疾信息管理（DisabilityInfo 学员提交 → 管理员审核）
- [x] 特殊人群记录（SpecialPersonRecord 禁驾期计算 + 禁令状态查询）
- [x] 补充数据库：`upgrade_disability_special.sql` 增量升级脚本 + 合并到 `init_script.sql`

### 待完善

**P1 — 功能待完善**
- [ ] Config 实体类（表已存在，目前仅通过 ConfigMapper 原生 SQL 访问）
- [ ] CoachPortalController 新增的 7 个端点（time-slots/profile/password）缺少 `@RequireRole(2)` 注解（虽已通过 resolveCoachId() 做了编程式校验，但与现有声明式模式不一致）

**P2 — 业务逻辑待优化**
- [ ] Token 刷新接口（当前 Token 过期后需重新登录）
- [ ] 密码重置功能
- [ ] 数据报表导出（Excel/PDF）
- [ ] 系统中 config 表的配置项管理页面（目前直连数据库修改）
- [ ] ProgressServiceImpl 按 examMode 区分小汽车（4 科）与特种车辆（2 科）的结业规则

### 评估：缺失功能可实现性

| 待实现 | 现状 |
|-------|------|
| Token 刷新 | 未实现，需新增接口 |
| 密码重置 | 未实现 |
| 报表导出 | 未实现 |
| 特种车辆进度适配 | ProgressServiceImpl 需根据 examMode 分支处理 |
| Config 实体类 | 表已存在，Java 实体待创建（目前 ConfigMapper 走原生 SQL） |
| 教练准教车型变更 | 已实现：教练提交 → 管理员审核 → 自动更新 coach.vehicle_type |
| 支付管理 | 已实现：payment_record 表 → 欠费管理 / 收入看板 / 账单自动生成 |
| 合场管理 | 已实现：学员申请 → 支付 → 管理员安排时间 / 完成 / 取消 |
| 体检申请 | 已实现：学员提交 → 管理员审核 → 录入体检结果 |
| 增驾申请 | 已实现：学员申请 → 管理员审核 → 录入考试结果（通过后自动更新车型） |
| 排班管理 | 已实现：教练提交 → 管理员审核（冲突检测 + 场地容量校验） |
| 车辆管理 | 已实现：管理员 CRUD → 教练查询可用车辆 → 排班引用 |
| 残疾信息管理 | 已实现：学员提交 → 管理员审核（✅ 已补齐建表 SQL） |
| 特殊人群记录 | 已实现：学员提交 → 管理员审核 → 禁驾期计算（✅ 已补齐建表 SQL） |
| 教练自助注册 | 已实现：公开注册 → 管理员审核 |
| 教练资料编辑 | 已实现：白名单字段更新 + BCrypt 密码修改 |

## 设计约定

- **ORM 风格** — 全部使用 MyBatis-Plus Lambda 查询，不使用 XML Mapper
- **逻辑删除** — `is_deleted = 0`（未删除），查询时 MP 自动拼接条件
- **时间字段** — `createTime`/`updateTime` 由 `MyMetaObjectHandler` 自动填充
- **异常处理** — 业务异常统一抛出 `ServiceException(ServiceCode, message)`，全局处理器捕获
- **控制器** — 不经 Service 层直写逻辑的简单操作（如单表简单查询）允许写在 Controller 中，复杂业务逻辑（PDF 生成、文件存储、推荐算法）下沉到 Service 实现
- **文档** — API 文档按模块拆分为独立 markdown 文件（登录注册、学员、教练、约车、考试、统计等）
