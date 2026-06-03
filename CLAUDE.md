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
│   │   ├── 01_schema.sql            #   全部 15 张表的建表语句
│   │   └── 02_init_data.sql         #   初始化基础数据
│   ├── upgrade/                     # 增量升级（按需执行，不丢数据）
│   │   ├── add_constraints.sql      #   外键约束（建议开发后期启用）
│   │   ├── upgrade_license_type.sql #   小汽车车型升级
│   │   └── upgrade_basic_data.sql   #   基础数据优化（车型模式+考场+特种车）
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
│   ├── dto/                                  # 数据传输对象
│   │   ├── AppointmentActionDTO.java         #   约课操作 DTO
│   │   ├── AvailableTimeDTO.java             #   空闲时间 DTO
│   │   └── RecordHoursDTO.java              #   录入学时 DTO
│   │
│   ├── vo/                                   # 视图对象
│   │   ├── CoachRatingVO.java               #   教练评分 VO
│   │   ├── CoachWorkloadVO.java             #   教练工作量 VO
│   │   └── StudentInfoVO.java               #   学员信息 VO
│   │
│   ├── controller/                           # REST 控制器（18 个）
│   │   ├── LoginController.java              #   认证登录
│   │   ├── UserController.java               #   用户管理
│   │   ├── CoachController.java              #   教练管理
│   │   ├── StudentController.java            #   学员管理（独立拆分）
│   │   ├── CoachPortalController.java        #   教练端门户
│   │   ├── CoachPortalController1.java       #   [待清理] 旧版，功能已合并
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
│   │   ├── ProgressController.java           #   学习进度查询
│   │   └── StatisticsController.java         #   统计报表
│   │   └── # [缺失] ExamVenueController（考场管理）
│   │
│   ├── entity/                               # 数据实体（12 个，DB 共 15 张表）
│   │   ├── User.java                         #   用户表
│   │   ├── Coach.java                        #   教练扩展表
│   │   ├── StudentCoach.java                 #   学员-教练关联表
│   │   ├── Appointment.java                  #   约课表
│   │   ├── TrainingRecord.java               #   学时记录表
│   │   ├── ExamSession.java                  #   考试场次表
│   │   ├── ExamRegistration.java             #   考试报名表
│   │   ├── CoachApplication.java             #   教练选择申请表
│   │   ├── FeeStandard.java                  #   费用标准表
│   │   ├── LicenseConfig.java                #   驾照类型配置表
│   │   ├── Notice.java                       #   通知公告表
│   │   └── File.java                         #   文件表
│   │   └── # [缺失] ExamVenue, SpecialExamRecord, Config 实体
│   │
│   ├── mapper/                               # Mapper 接口（12 个，均继承 BaseMapper）
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
│   │   └── FileMapper.java
│   │   └── # [缺失] ExamVenueMapper, SpecialExamRecordMapper, ConfigMapper
│   │
│   ├── service/                           # 业务层
│   │   ├── IAppointmentService.java
│   │   ├── ICoachService.java
│   │   ├── ICoachPortalService.java       #   教练端门户
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
│   │   └── IUserService.java
│   │   └── # [缺失] IExamVenueService, ISpecialExamService
│   │   │
│   │   └── impl/
│   │       ├── AppointmentServiceImpl.java
│   │       ├── CoachPortalServiceImpl.java
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
├── 登录注册接口文档.md                      # 接口对接文档（拆分模块化）
├── 用户通用接口文档.md
├── 学员管理接口文档.md
├── 教练管理接口文档.md
├── 教练学员管理接口文档.md
├── 约课管理接口文档.md
├── 考试管理接口文档.md
├── 统计报表接口文档.md
├── 费用标准接口文档.md
├── 车型配置接口文档.md
├── 文件管理接口文档.md
├── 业务逻辑分析.md
├── README.md
└── HELP.md
```

## 核心功能模块

### 1. 认证与权限
- **登录** `POST /login` — 用户名/密码登录，返回 JWT Token（7 天有效）
- **拦截器** — `JwtInterceptor` 拦截除公开路径外的所有请求，校验 Token
- **角色注解** — `@RequireRole({1,3})` 方法级权限控制（1=学员, 2=教练, 3=管理员）
- ✅ `@RequireRole` 已应用到 17 个 Controller 共 57 处敏感接口上

### 2. 用户管理
- 学员/教练/管理员的 CRUD，用户状态审核流程（待审核 → 通过/不通过）
- 学员管理独立 `StudentController`，支持分页查询
- 学员端个人信息完善

### 3. 报名审核
- `PUT /registrations/{userId}/audit` — 审核通过时自动生成两份 PDF（报名表 + 准考证）
- PDF 使用 iText 7 生成，支持中文字体

### 4. 教练管理
- 教练信息 CRUD，评分、执教年限、准教车型管理
- 教练分配：自动推荐（按车型匹配 + 评分排序）+ 管理员手动分配 + 学员申请
- `FIND_IN_SET` 处理逗号分隔的准教车型字段
- 教练端门户（`/coach-portal`）：查看名下学员列表、录入学时、约课确认/拒绝、设置空闲时间、查看工作量统计、查看个人评分

### 5. 约课管理
- 学员预约教练课程，课程时间管理，取消约课
- 教练确认/拒绝约课

### 6. 考试管理
- 考试场次发布/修改（含名额管理）
- 学员报名考试，管理员审核（扣减名额），录入成绩（≥90 合格，硬编码）
- 补考次数自动累计

### 7. 费用管理
- 收费标准 CRUD，按驾照类型配置费用项目
- 仅管理员可管理
- ✅ 完全实现

### 8. 驾照车型配置
- 驾照类型（C1/C2/B1/N1/N2/N3）配置管理
- 仅管理员可管理
- ⚠️ Controller 有完整 CRUD，但 Java 实体未包含 3 个新字段（exam_mode, coach_audit_required, cert_name）

### 9. 通知公告
- 通知公告的发布和管理
- 仅管理员可管理

### 10. 学习进度
- 学员端查询个人学习进度
- 学时记录统计
- ⚠️ 仅支持小汽车 4 科模式，不支持特种车辆 2 科模式

### 11. 统计报表
- 报名趋势统计、考试合格率统计、教练工作量统计

### 12. 文件管理
- 本地磁盘存储，按类型分子目录（id_card_front, id_card_back, physical_exam 等）
- 文件上传（5MB 限制）、下载、静态资源访问（`/uploads/**`）

## API 概览

| 模块 | 基础路径 | 角色权限 |
|------|---------|----------|
| 认证 | `/login` | 公开 |
| 用户 | `/users` | 全部（登录） |
| 学员 | `/students` | 管理员 |
| 教练 | `/coaches` | 全部（登录） |
| 教练端 | `/coach-portal` | 教练 |
| 约课 | `/appointments` | 学员/管理员 |
| 报名审核 | `/registrations` | 管理员 |
| 考试场次 | `/exam-sessions` | 管理员 |
| 考试报名 | `/exam-registrations` | 学员/管理员 |
| 教练申请 | `/coach-applications` | 学员/管理员 |
| 教练分配 | `/coach-assignments` | 管理员 |
| 费用标准 | `/fee-standards` | 管理员 |
| 车型配置 | `/license-configs` | 管理员 |
| 通知公告 | `/notices` | 全部（登录） |
| 学习进度 | `/progress` | 全部（登录） |
| 统计报表 | `/statistics` | 管理员 |
| 文件 | `/files` | 全部（登录） |
| **考场管理** | 无 | **待开发** |
| **特种车辆考试** | 无 | **待开发** |

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

# 方式三：已有数据库执行增量升级
mysql -u root -proot driveman < database/upgrade/upgrade_basic_data.sql

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
| coach1 | admin123 | 教练（张教练） |
| coach2 | admin123 | 教练（李教练） |
| student1 | admin123 | 学员（王小明） |
| student2 | admin123 | 学员（李芳） |

## 当前开发状态

### 已完成
- [x] Spring Boot + MyBatis-Plus 框架搭建
- [x] 15 张数据库表结构设计 + 初始化数据 + 测试数据
- [x] 三层架构（Controller/Service/Mapper）代码生成
- [x] JWT 登录认证 + Token 校验拦截器
- [x] `@RequireRole` 角色权限注解（已应用到 17 个 Controller 共 57 处敏感接口）
- [x] 用户 CRUD 管理 + 学员管理独立控制器
- [x] 教练 CRUD + 自动推荐 + 分配 + 解绑
- [x] 学员申请教练 + 管理员审核
- [x] 约课管理（预约/取消/教练确认拒绝）
- [x] 学时记录管理
- [x] 报名审核 + PDF 报名表/准考证生成（iText 7）
- [x] 文件上传/下载/静态访问（本地存储）
- [x] 考试场次 CRUD + 名额管理 + 考试报名审核 + 成绩录入 + 补考
- [x] 教练端门户（查看学员、录入学时、确认约课、空闲时间、工作量统计、评分查看）
- [x] 费用标准管理（CRUD + 按车型查询）
- [x] 驾照车型配置 CRUD（Controller 层完成）
- [x] 通知公告管理
- [x] 学习进度查询（小汽车 4 科模式）
- [x] 统计报表（报名趋势/合格率/教练工作量）
- [x] 全局异常处理 + 统一响应格式
- [x] 全局跨域配置
- [x] Knife4j 接口文档
- [x] 通用配置（分页、自动填充、逻辑删除、Redis 模板、Validator）
- [x] 接口文档按模块拆分（12 份独立文档）
- [x] 数据库目录拆分（full/ + upgrade/ + test/）
- [x] 增量升级脚本 `upgrade_basic_data.sql`（车型模式 + 考场 + 特种车）

### 待完善

**P0 — Java 实体与数据库不同步（不改会启动报错）**
- [ ] `LicenseConfig.java` 缺少 `exam_mode`、`coach_audit_required`、`cert_name` 三个字段
- [ ] `ExamSession.java` 缺少 `venueId` 字段

**P1 — 缺少 Java 代码（表已建、Java 未实现）**
- [ ] `ExamVenue` 实体 + Mapper + Service + Controller（考场管理 CRUD）
- [ ] `SpecialExamRecord` 实体 + Mapper + Service + Controller（特种车辆考试记录）

**P2 — 业务逻辑待优化**
- [ ] `ExamRegistrationController.enterScore()` 硬编码 ≥90，应改为读取 `license_config.pass_score`
- [ ] `ExamRegistrationMapper.findPassedSubjectsByStudent()` 硬编码 `score >= 90`
- [ ] `ProgressServiceImpl` 只支持小汽车 4 科模式，未适配特种车辆 2 科模式
- [ ] 考试报名需校验车型的 `coach_audit_required` 字段，判断是否需要教练审批
- [ ] 学员自助注册接口（当前需由管理员创建）
- [ ] Token 刷新接口
- [ ] 清理已废弃的 `CoachPortalController1.java`

### 评估：缺失功能可实现性

| 待实现 | 难度 | 评估 |
|-------|------|------|
| LicenseConfig 加 3 字段 | ★☆☆ 简单 | 加 3 个成员变量 + 注解，5 分钟 |
| ExamSession 加 venueId | ★☆☆ 简单 | 加 1 个成员变量 |
| ExamVenue CRUD | ★★☆ 中等 | 参照 FeeStandard CRUD 模板，4 个文件 |
| SpecialExamRecord | ★★☆ 中等 | 新建实体/Mapper/Service + Controller |
| 及格分改为读取配置 | ★★☆ 中等 | 注入 LicenseConfigMapper，联表查 pass_score |
| 特种车辆进度 | ★★★ 稍复杂 | 需要区分 exam_mode 分支逻辑 |

## 设计约定

- **ORM 风格** — 全部使用 MyBatis-Plus Lambda 查询，不使用 XML Mapper
- **逻辑删除** — `is_deleted = 0`（未删除），查询时 MP 自动拼接条件
- **时间字段** — `createTime`/`updateTime` 由 `MyMetaObjectHandler` 自动填充
- **异常处理** — 业务异常统一抛出 `ServiceException(ServiceCode, message)`，全局处理器捕获
- **控制器** — 不经 Service 层直写逻辑的简单操作（如单表简单查询）允许写在 Controller 中，复杂业务逻辑（PDF 生成、文件存储、推荐算法）下沉到 Service 实现
- **文档** — API 文档按模块拆分为独立 markdown 文件（登录注册、学员、教练、约课、考试、统计等）
