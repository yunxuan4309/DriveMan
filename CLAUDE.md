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
│   ├── init_script.sql          # 完整建表 + 初始化数据（含测试数据）
│   └── add_constraints.sql      # 外键约束（开发后期/生产环境按需启用）
│
├── src/main/java/com/homework/driveman/
│   ├── DriveManApplication.java          # Spring Boot 启动入口
│   │
│   ├── config/                           # 全局配置
│   │   ├── JwtInterceptor.java           #   JWT 鉴权拦截器
│   │   ├── Knife4jConfiguration.java      #   Knife4j 接口文档
│   │   ├── MybatisPlusConfiguration.java #   MyBatis-Plus 分页 + MapperScan
│   │   ├── MyMetaObjectHandler.java      #   自动填充 createTime/updateTime
│   │   ├── RedisConfig.java              #   Redis 序列化配置
│   │   ├── RequireRole.java              #   角色权限注解 @RequireRole
│   │   ├── ValidationConfiguration.java  #   Validator 快速失败
│   │   └── WebMvcConfiguration.java      #   跨域 + 拦截器 + 静态资源
│   │
│   ├── controller/                       # REST 控制器
│   │   ├── LoginController.java          #   认证登录
│   │   ├── UserController.java           #   用户管理
│   │   ├── CoachController.java          #   教练管理
│   │   ├── AppointmentController.java    #   约课管理
│   │   ├── RegistrationController.java   #   报名审核（含 PDF 生成）
│   │   ├── FileController.java           #   文件上传下载
│   │   ├── ExamSessionController.java    #   考试场次管理
│   │   ├── ExamRegistrationController.java # 考试报名管理
│   │   ├── CoachApplicationController.java # 教练申请审核
│   │   └── CoachAssignmentController.java  # 教练分配
│   │
│   ├── entity/                           # 数据实体（9 个）
│   │   ├── User.java                     #   用户表
│   │   ├── Coach.java                    #   教练扩展表
│   │   ├── StudentCoach.java             #   学员-教练关联表
│   │   ├── Appointment.java              #   约课表
│   │   ├── TrainingRecord.java           #   学时记录表
│   │   ├── ExamSession.java              #   考试场次表
│   │   ├── ExamRegistration.java         #   考试报名表
│   │   ├── CoachApplication.java         #   教练选择申请表
│   │   └── File.java                     #   文件表
│   │
│   ├── mapper/                           # Mapper 接口（9 个，均继承 BaseMapper）
│   │   └── *Mapper.java
│   │
│   ├── service/                          # 业务层
│   │   ├── impl/
│   │   │   ├── PdfServiceImpl.java       #   PDF 生成（iText 7）
│   │   │   ├── FileServiceImpl.java      #   文件存储（本地磁盘）
│   │   │   ├── CoachServiceImpl.java     #   教练推荐（FIND_IN_SET）
│   │   │   └── ...
│   │   └── I*Service.java                # 业务接口
│   │
│   ├── utils/
│   │   ├── JwtUtils.java                 #   JWT 签发/解析
│   │   └── CurrentUser.java              #   当前用户上下文 DTO
│   │
│   ├── exception/
│   │   ├── ServiceException.java         #   业务异常
│   │   └── GlobalExceptionHandler.java   #   全局异常处理
│   │
│   └── web/
│       ├── JsonResult.java               #   统一响应封装
│       ├── ServiceCode.java              #   业务状态码枚举
│       └── ServiceCode2.java             #   [废弃] 旧状态码，未使用
│
├── src/main/resources/
│   └── application.yaml                  # 应用配置
│
├── upload-files/                         # 文件上传存储目录（运行期生成）
│
├── API.md                                # 接口对接文档
├── README.md                             # 项目说明
└── 业务逻辑分析.md                        # 业务逻辑分析文档
```

## 核心功能模块

### 1. 认证与权限
- **登录** `POST /login` — 用户名/密码登录，返回 JWT Token（7 天有效）
- **拦截器** — `JwtInterceptor` 拦截除公开路径外的所有请求，校验 Token
- **角色注解** — `@RequireRole({1,3})` 方法级权限控制（1=学员, 2=教练, 3=管理员）
- 当前所有业务接口仅验证 Token 有效性，未施加角色限制

### 2. 用户管理
- 学员/教练/管理员的 CRUD，用户状态审核流程（待审核 → 通过/不通过）

### 3. 报名审核
- `PUT /registrations/{userId}/audit` — 审核通过时自动生成两份 PDF（报名表 + 准考证）
- PDF 使用 iText 7 生成，支持中文字体

### 4. 教练管理
- 教练信息 CRUD，评分、执教年限、准教车型管理
- 教练分配：自动推荐（按车型匹配 + 评分排序）+ 管理员手动分配 + 学员申请
- `FIND_IN_SET` 处理逗号分隔的准教车型字段

### 5. 约课管理
- 学员预约教练课程，课程时间管理，取消约课

### 6. 考试管理
- 考试场次发布/修改（含名额管理）
- 学员报名考试，管理员审核（扣减名额），录入成绩（≥90 合格）
- 补考次数自动累计

### 7. 文件管理
- 本地磁盘存储，按类型分子目录（id_card_front, id_card_back, physical_exam 等）
- 文件上传（5MB 限制）、下载、静态资源访问（`/uploads/**`）

## API 概览

| 模块 | 基础路径 | 公开接口 | 需登录 |
|------|---------|----------|--------|
| 认证 | `/login` | `POST /login` | — |
| 用户 | `/users` | — | 全部 |
| 教练 | `/coaches` | — | 全部 |
| 约课 | `/appointments` | — | 全部 |
| 报名审核 | `/registrations` | — | 全部 |
| 文件 | `/files` | — | 全部 |
| 考试场次 | `/exam-sessions` | — | 全部 |
| 考试报名 | `/exam-registrations` | — | 全部 |
| 教练申请 | `/coach-applications` | — | 全部 |
| 教练分配 | `/coach-assignments` | — | 全部 |

**统一响应格式:** `JsonResult<T>` — `{ state: Integer, message: String | null, data: T }`

**Token 传递方式:** `Authorization: Bearer <token>`

详细接口说明见 `API.md`。

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
# 1. 初始化数据库
mysql -u root -proot < database/init_script.sql

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
| 13812340001 | admin123 | 教练（张教练） |
| 13812340002 | admin123 | 教练（李教练） |
| 15912340001 | admin123 | 学员（王小明） |
| 15912340002 | admin123 | 学员（李芳） |

## 当前开发状态

### 已完成
- [x] Spring Boot + MyBatis-Plus 框架搭建
- [x] 12 张数据库表结构设计 + 初始化数据
- [x] 三层架构（Controller/Service/Mapper）代码生成
- [x] JWT 登录认证 + Token 校验拦截器
- [x] `@RequireRole` 角色权限注解（注解已定义、拦截器已支持，未应用到具体接口）
- [x] 用户 CRUD 管理
- [x] 教练 CRUD + 自动推荐 + 分配 + 解绑
- [x] 学员申请教练 + 管理员审核
- [x] 约课管理（预约/取消）
- [x] 学时记录管理
- [x] 报名审核 + PDF 报名表/准考证生成（iText 7）
- [x] 文件上传/下载/静态访问（本地存储）
- [x] 考试场次 CRUD + 名额管理
- [x] 考试报名 + 审核（扣减名额）+ 成绩录入 + 补考
- [x] 全局异常处理 + 统一响应格式
- [x] 全局跨域配置
- [x] Knife4j 接口文档
- [x] 通用配置（分页、自动填充、逻辑删除、Redis 模板、Validator）

### 待完善
- [ ] `@RequireRole` 应用到各 Controller 的敏感接口上（如仅管理员可删除用户）
- [ ] 学员注册接口（当前需通过 POST /users 由管理员创建，未开放自助注册）
- [ ] Token 刷新接口
- [ ] 报表统计（ECharts JSON 数据接口、Excel 导出）
- [ ] 业务层数据校验增强
- [ ] `add_constraints.sql` 外键约束（建议生产环境按需启用）
- [ ] 清理已废弃的 `ServiceCode2.java`

## 设计约定

- **ORM 风格** — 全部使用 MyBatis-Plus Lambda 查询，不使用 XML Mapper
- **逻辑删除** — `is_deleted = 0`（未删除），查询时 MP 自动拼接条件
- **时间字段** — `createTime`/`updateTime` 由 `MyMetaObjectHandler` 自动填充
- **异常处理** — 业务异常统一抛出 `ServiceException(ServiceCode, message)`，全局处理器捕获
- **控制器** — 不经 Service 层直写逻辑的简单操作（如单表简单查询）允许写在 Controller 中，复杂业务逻辑（PDF 生成、文件存储、推荐算法）下沉到 Service 实现
