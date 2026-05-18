# DriveMan — 驾校报名管理系统

一个基于 Spring Boot 3 + MyBatis-Plus 的驾校报名管理系统后端项目。

## 技术栈

| 框架/工具 | 版本 | 用途 |
|-----------|------|------|
| Spring Boot | 3.2.5 | 应用框架 |
| Java | 21 | 开发语言 |
| MyBatis-Plus | 3.5.9 | ORM 框架 |
| MySQL | 8.0+ | 数据库 |
| Redis | - | 缓存 |
| Knife4j | 4.5.0 | API 接口文档 |
| Lombok | - | 代码简化 |

## 项目结构

```
src/main/java/com/homework/driveman/
├── DriveManApplication.java        # 启动入口
├── config/                         # 配置类
│   ├── Knife4jConfiguration        # 接口文档配置
│   ├── MybatisPlusConfiguration    # MyBatis-Plus 分页插件
│   ├── MyMetaObjectHandler         # 自动填充（createTime/updateTime）
│   ├── RedisConfig                 # Redis 模板配置
│   ├── ValidationConfiguration     # 参数校验（快速失败）
│   └── WebMvcConfiguration         # 跨域配置
├── controller/                     # 控制器层
│   ├── UserController              # 用户管理接口
│   ├── CoachController             # 教练管理接口
│   └── AppointmentController       # 约课管理接口
├── entity/                         # 实体类（对应数据库表）
│   ├── User.java
│   ├── Coach.java
│   ├── Appointment.java
│   ├── TrainingRecord.java
│   ├── ExamSession.java
│   ├── ExamRegistration.java
│   ├── StudentCoach.java
│   └── CoachApplication.java
├── exception/                      # 异常处理
│   ├── ServiceException            # 业务异常类
│   └── GlobalExceptionHandler      # 全局异常处理器
├── mapper/                         # MyBatis-Plus Mapper 接口
├── service/                        # 业务逻辑层
│   ├── impl/                       # 实现类
│   └── I*.java                     # 业务接口
└── web/                            # 通用响应封装
    ├── JsonResult                  # 统一响应格式
    └── ServiceCode                 # 业务状态码
```

## 快速启动

### 1. 创建数据库

执行项目根目录下的 SQL 脚本：

```bash
mysql -u root -p < database/create_tables.sql
```

### 2. 修改配置

编辑 `src/main/resources/application.yaml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    username: your_username
    password: your_password
```

### 3. 启动项目

```bash
./mvnw spring-boot:run
```

### 4. 访问文档

启动后浏览器打开：http://localhost:9080/doc.html

## 数据库表结构

| 表名 | 说明 |
|------|------|
| user | 用户表（学员/教练/管理员） |
| coach | 教练扩展表 |
| student_coach | 学员-教练关联表 |
| appointment | 约课表 |
| training_record | 学时记录表 |
| exam_session | 考试场次表 |
| exam_registration | 考试报名表 |
| file | 文件表 |
| config | 系统配置表 |
| coach_application | 教练选择申请表 |
| notice | 系统公告表 |
| fee_standard | 费用标准表 |

## API 示例

| 请求方式 | 路径 | 说明 |
|---------|------|------|
| GET | /users | 查询所有用户 |
| GET | /users/{id} | 根据ID查询用户 |
| POST | /users | 新增用户 |
| PUT | /users/{id} | 修改用户 |
| DELETE | /users/{id} | 删除用户 |
| GET | /coaches | 查询所有教练 |
| POST | /coaches | 新增教练 |
| GET | /appointments | 查询所有约课 |
| POST | /appointments | 新增约课 |
| PUT | /appointments/{id}/cancel | 取消约课 |

## 主要特性

- **统一响应格式**: 所有接口返回 `JsonResult` 格式（state + message + data）
- **全局异常处理**: ServiceException 和未知异常统一拦截，不暴露堆栈信息
- **逻辑删除**: 所有表使用 `is_deleted` 字段软删除，MyBatis-Plus 自动拦截
- **自动填充**: createTime/updateTime 由 MyMetaObjectHandler 自动填充
- **接口文档**: Knife4j + SpringDoc OpenAPI，在线调试 API
- **参数校验**: Hibernate Validator 快速失败模式
