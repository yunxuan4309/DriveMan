# 驾校报名管理系统 API 接口文档

> 基础路径: `http://localhost:9500`
> 端口已在 application.yaml 中配置，如遇端口冲突可修改 `server.port`

---

## 目录

1. [认证接口](#1-认证接口)
   - [1.1 登录](#11-登录)
   - [1.2 Token 刷新](#12-token-刷新)
2. [用户管理](#2-用户管理)
3. [教练管理](#3-教练管理)
4. [约课管理](#4-约课管理)
5. [报名审核](#5-报名审核)
6. [文件管理](#6-文件管理)
7. [考试场次管理](#7-考试场次管理)
8. [考试报名管理](#8-考试报名管理)
9. [教练申请审核](#9-教练申请审核)
10. [教练分配](#10-教练分配)
11. [权限拦截规则](#11-权限拦截规则)
12. [前端处理要点](#12-前端处理要点)
13. [附录：全局响应格式](#13-附录全局响应格式)

---

## 1. 认证接口

### 1.1 登录

获取 JWT Token，后续所有请求需在请求头中携带此 Token。

- **URL**: `POST /login`
- **是否需要登录**: 否（公开接口）
- **Content-Type**: `application/x-www-form-urlencoded`

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| username | String | 是 | 登录账号（管理员: admin） |
| password | String | 是 | 登录密码（管理员: admin123） |

#### 请求示例

```bash
curl -X POST http://localhost:9500/login \
  -d "username=admin&password=admin123"
```

#### 响应示例（成功）

```json
{
  "state": 20000,
  "message": null,
  "data": {
    "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIxIiwidXNlcm5hbWUiOiJhZG1pbiIsInJvbGUiOjMsImlhdCI6MTc3OTQwOTg3NCwiZXhwIjoxNzgwMDE0Njc0fQ.aCzBxbXUs1b40HMFKS_Ra3LKQzA4TBjAQKAuEUDH_3i-d44BTEv19Oq22iTha4u1",
    "userId": 1,
    "username": "admin",
    "realName": "系统管理员",
    "role": 3
}
```

#### 响应示例（失败 — 账号或密码错误）

```json
{
  "state": 40100,
  "message": "用户名或密码错误",
  "data": null
}
```

#### 返回字段说明

| 字段名 | 类型 | 说明 |
|--------|------|------|
| token | String | JWT Token，有效期 7 天 |
| userId | Integer | 用户 ID |
| username | String | 登录账号 |
| realName | String | 真实姓名 |
| role | Integer | 角色: 1-学员, 2-教练, 3-管理员 |

#### Token 传递方式

所有需要登录的接口，在请求头中传递：

```
Authorization: Bearer <token>
```

示例：

```bash
curl http://localhost:9500/users \
  -H "Authorization: Bearer eyJhbGciOiJIUzM4NCJ9..."
```

### 1.2 Token 刷新

> **当前暂未提供独立的 Token 刷新接口。** Token 有效期为 7 天，到期后需重新调用登录接口获取新 Token。
>
> 前端可在 Token 过期前（如剩余 1 天）或检测到 401 响应时，自动跳转到登录页让用户重新登录。
>
> 后续如需支持无感刷新，可扩展实现 Refresh Token 机制。

---

## 2. 用户管理

基础路径: `/users` — 学员/教练/管理员的 CRUD 操作。

| 方法 | URL | 说明 | 需要登录 |
|------|-----|------|----------|
| GET | `/users` | 查询所有用户 | 是 |
| GET | `/users/{id}` | 根据 ID 查询用户 | 是 |
| POST | `/users` | 新增用户 | 是 |
| PUT | `/users/{id}` | 修改用户 | 是 |
| DELETE | `/users/{id}` | 删除用户（逻辑删除） | 是 |

### GET /users

查询所有用户列表。

#### 请求示例

```bash
curl http://localhost:9500/users \
  -H "Authorization: Bearer <token>"
```

#### 响应示例

```json
{
  "state": 20000,
  "message": null,
  "data": [
    {
      "userId": 1,
      "role": 3,
      "username": "admin",
      "password": null,
      "realName": "系统管理员",
      "idCard": "11010119900307663X",
      "phone": "13800000000",
      "address": "重庆市南岸区",
      "licenseType": null,
      "avatar": null,
      "status": 1,
      "auditReason": null,
      "createTime": "2026-05-22T08:26:46",
      "updateTime": "2026-05-22T08:26:46",
      "isDeleted": 0
    }
  ]
}
```

### GET /users/{id}

根据 ID 查询单个用户。

#### 请求示例

```bash
curl http://localhost:9500/users/1 \
  -H "Authorization: Bearer <token>"
```

#### 响应示例

```json
{
  "state": 20000,
  "message": null,
  "data": {
    "userId": 1,
    "role": 3,
    "username": "admin",
    "realName": "系统管理员",
    "idCard": "11010119900307663X",
    "phone": "13800000000",
    "address": "重庆市南岸区",
    "status": 1,
    "createTime": "2026-05-22T08:26:46",
    "updateTime": "2026-05-22T08:26:46"
  }
}
```

### POST /users

新增用户。

- **Content-Type**: `application/json`

#### 请求体示例

```json
{
  "role": 1,
  "username": "newstudent",
  "password": "password123",
  "realName": "新学员",
  "idCard": "500101200201011234",
  "phone": "15912340003",
  "address": "重庆市南岸区",
  "licenseType": "C1"
}
```

#### 响应示例

```json
{
  "state": 20000,
  "message": null,
  "data": null
}
```

### PUT /users/{id}

修改用户信息。传入需要修改的字段即可。

#### 请求体示例

```json
{
  "realName": "新名字",
  "phone": "15900000000"
}
```

#### 响应

```json
{
  "state": 20000,
  "message": null,
  "data": null
}
```

### DELETE /users/{id}

逻辑删除用户（将 `is_deleted` 置为 1）。

#### 响应

```json
{
  "state": 20000,
  "message": null,
  "data": null
}
```

---

## 3. 教练管理

基础路径: `/coaches` — 教练信息的 CRUD 操作。

| 方法 | URL | 说明 | 需要登录 |
|------|-----|------|----------|
| GET | `/coaches` | 查询所有教练 | 是 |
| GET | `/coaches/{id}` | 根据 ID 查询教练 | 是 |
| POST | `/coaches` | 新增教练 | 是 |
| PUT | `/coaches/{id}` | 修改教练 | 是 |
| DELETE | `/coaches/{id}` | 删除教练 | 是 |

### GET /coaches

查询所有教练列表。

#### 请求示例

```bash
curl http://localhost:9500/coaches \
  -H "Authorization: Bearer <token>"
```

#### 响应示例

```json
{
  "state": 20000,
  "message": null,
  "data": [
    {
      "coachId": 1,
      "userId": 2,
      "rating": 4.8,
      "availableTime": null,
      "coachYears": 6,
      "vehicleType": "C1",
      "createTime": "2026-05-22T08:26:46",
      "updateTime": "2026-05-22T08:26:46",
      "isDeleted": 0
    }
  ]
}
```

### POST /coaches

新增教练。

#### 请求体示例

```json
{
  "userId": 6,
  "rating": 5.0,
  "coachYears": 3,
  "vehicleType": "C1,C2"
}
```

### PUT /coaches/{id}

修改教练信息。

### DELETE /coaches/{id}

删除教练。

---

## 4. 约课管理

基础路径: `/appointments` — 学员预约/取消课程。

| 方法 | URL | 说明 | 需要登录 |
|------|-----|------|----------|
| GET | `/appointments` | 查询所有约课记录 | 是 |
| GET | `/appointments/{id}` | 根据 ID 查询约课 | 是 |
| POST | `/appointments` | 新增约课 | 是 |
| PUT | `/appointments/{id}/cancel` | 取消约课 | 是 |

### POST /appointments

新增约课。

#### 请求体示例

```json
{
  "studentId": 4,
  "coachId": 1,
  "startTime": "2026-05-25T09:00:00",
  "endTime": "2026-05-25T10:00:00"
}
```

### PUT /appointments/{id}/cancel

取消约课，可选填写取消原因。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| reason | String | 否 | 取消原因 |

#### 请求示例

```bash
curl -X PUT "http://localhost:9500/appointments/1/cancel?reason=临时有事" \
  -H "Authorization: Bearer <token>"
```

---

## 5. 报名审核

基础路径: `/registrations` — 审核学员报名，自动生成 PDF 报名表和准考证。

| 方法 | URL | 说明 | 需要登录 |
|------|-----|------|----------|
| PUT | `/registrations/{userId}/audit` | 审核学员报名 | 是 |
| GET | `/registrations/pending` | 查询所有待审核学员 | 是 |
| GET | `/registrations/{userId}/files` | 查询学员的报名相关文件 | 是 |

### PUT /registrations/{userId}/audit

审核学员报名。`pass=true` 时自动生成 PDF 报名表和准考证并存入文件记录；`pass=false` 时需填写 `reason`。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pass | Boolean | 是 | true=通过, false=不通过 |
| reason | String | 否 | 审核不通过原因（pass=false 时必填） |

#### 请求示例（通过）

```bash
curl -X PUT "http://localhost:9500/registrations/4/audit?pass=true" \
  -H "Authorization: Bearer <token>"
```

#### 请求示例（不通过）

```bash
curl -X PUT "http://localhost:9500/registrations/4/audit?pass=false&reason=身份证照片不清晰" \
  -H "Authorization: Bearer <token>"
```

#### 响应示例

```json
{
  "state": 20000,
  "message": null,
  "data": null
}
```

### GET /registrations/pending

查询所有待审核状态的学员（`role=1` 且 `status=0`）。

#### 响应示例

```json
{
  "state": 20000,
  "message": null,
  "data": [
    {
      "userId": 6,
      "username": "student123",
      "realName": "新学员",
      "idCard": "500101200201011234",
      "phone": "15912340003",
      "licenseType": "C1",
      "status": 0,
      "createTime": "2026-05-22T10:00:00"
    }
  ]
}
```

### GET /registrations/{userId}/files

查询某学员的报名表和准考证 PDF 文件记录。

#### 响应示例

```json
{
  "state": 20000,
  "message": null,
  "data": [
    {
      "id": 1,
      "userId": 4,
      "fileName": "报名表_王小明.pdf",
      "filePath": "registration_pdf/xxx.pdf",
      "fileType": "registration_pdf",
      "uploadTime": "2026-05-22T10:00:00"
    }
  ]
}
```

---

## 6. 文件管理

基础路径: `/files` — 文件上传/下载/查看/删除。

| 方法 | URL | 说明 | 需要登录 |
|------|-----|------|----------|
| POST | `/files/upload` | 上传文件 | 是 |
| GET | `/files/{id}` | 根据 ID 查询文件记录 | 是 |
| GET | `/files/user/{userId}` | 查询某用户的所有文件 | 是 |
| GET | `/files/{id}/download` | 下载文件（返回文件流） | 是 |
| DELETE | `/files/{id}` | 删除文件记录（逻辑删除） | 是 |

### POST /files/upload

上传文件。`fileType` 决定存储子目录。

- **Content-Type**: `multipart/form-data`

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| userId | Integer | 是 | 上传者用户 ID |
| file | File | 是 | 上传的文件（最大 5MB） |
| fileType | String | 是 | 文件类型: id_card_front, id_card_back, physical_exam, registration_pdf, admission_ticket |

#### 请求示例

```bash
curl -X POST http://localhost:9500/files/upload \
  -H "Authorization: Bearer <token>" \
  -F "userId=4" \
  -F "file=@/path/to/idcard.jpg" \
  -F "fileType=id_card_front"
```

#### 响应示例

```json
{
  "state": 20000,
  "message": null,
  "data": {
    "id": 1,
    "userId": 4,
    "fileName": "idcard.jpg",
    "filePath": "id_card_front/xxx_idcard.jpg",
    "fileType": "id_card_front",
    "uploadTime": "2026-05-22T10:00:00"
  }
}
```

### GET /files/{id}/download

下载文件，返回文件二进制流。可直接在浏览器中打开链接下载。

#### 响应头

```
Content-Type: application/octet-stream
Content-Disposition: attachment; filename*=UTF-8''%E6%8A%A5%E5%90%8D%E8%A1%A8_%E7%8E%8B%E5%B0%8F%E6%98%8E.pdf
```

### 静态资源访问

上传的文件也可以通过静态资源 URL 直接访问：

```
http://localhost:9500/uploads/{filePath}
```

例如：
```
http://localhost:9500/uploads/id_card_front/xxx_idcard.jpg
```

---

## 7. 考试场次管理

基础路径: `/exam-sessions` — 发布/修改/查询/删除考试场次。

| 方法 | URL | 说明 | 需要登录 |
|------|-----|------|----------|
| GET | `/exam-sessions` | 查询所有考试场次 | 是 |
| GET | `/exam-sessions/{id}` | 根据 ID 查询场次 | 是 |
| POST | `/exam-sessions` | 发布考试场次 | 是 |
| PUT | `/exam-sessions/{id}` | 修改考试场次 | 是 |
| DELETE | `/exam-sessions/{id}` | 删除考试场次 | 是 |

### POST /exam-sessions

发布考试场次。

#### 请求体示例

```json
{
  "subject": 1,
  "examDate": "2026-07-01",
  "startTime": "09:00:00",
  "location": "南岸区车管所",
  "totalQuota": 100,
  "remainingQuota": 100
}
```

#### 响应

```json
{
  "state": 20000,
  "message": null,
  "data": null
}
```

---

## 8. 考试报名管理

基础路径: `/exam-registrations` — 学员报名考试、管理员审核、录入成绩。

| 方法 | URL | 说明 | 需要登录 |
|------|-----|------|----------|
| POST | `/exam-registrations` | 学员报名考试 | 是 |
| PUT | `/exam-registrations/{id}/audit` | 审核考试报名 | 是 |
| PUT | `/exam-registrations/{id}/score` | 录入考试成绩 | 是 |
| GET | `/exam-registrations` | 查询所有考试报名记录 | 是 |
| GET | `/exam-registrations/student/{studentId}` | 查询某学员的考试报名记录 | 是 |

### POST /exam-registrations

学员报名考试。需传入学员 ID 和场次 ID，系统自动扣减场次剩余名额（审核通过时）。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| studentId | Integer | 是 | 学员用户 ID |
| sessionId | Integer | 是 | 考试场次 ID |

#### 请求示例

```bash
curl -X POST "http://localhost:9500/exam-registrations?studentId=4&sessionId=1" \
  -H "Authorization: Bearer <token>"
```

### PUT /exam-registrations/{id}/audit

审核考试报名。`pass=true` 时扣减场次剩余名额。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pass | Boolean | 是 | true=通过, false=不通过 |

#### 请求示例

```bash
curl -X PUT "http://localhost:9500/exam-registrations/1/audit?pass=true" \
  -H "Authorization: Bearer <token>"
```

### PUT /exam-registrations/{id}/score

录入考试成绩。分数范围 0-100，>=90 为合格（视为通过），<90 自动增加补考次数。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| score | Integer | 是 | 考试成绩（0-100） |

#### 请求示例

```bash
curl -X PUT "http://localhost:9500/exam-registrations/1/score?score=95" \
  -H "Authorization: Bearer <token>"
```

### GET /exam-registrations/student/{studentId}

查询指定学员的所有考试报名记录，按报名时间倒序。

---

## 9. 教练申请审核

基础路径: `/coach-applications` — 学员申请 → 管理员审核 → 通过后自动写入绑定关系。

| 方法 | URL | 说明 | 需要登录 |
|------|-----|------|----------|
| POST | `/coach-applications` | 学员提交教练选择申请 | 是 |
| PUT | `/coach-applications/{id}/audit` | 审核教练申请 | 是 |
| GET | `/coach-applications/pending` | 查询所有待审核的教练申请 | 是 |
| GET | `/coach-applications/student/{studentId}` | 查询某学员的所有申请记录 | 是 |

### POST /coach-applications

学员提交教练选择申请。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| studentId | Integer | 是 | 学员用户 ID |
| coachId | Integer | 是 | 教练 ID |

### PUT /coach-applications/{id}/audit

审核教练申请。`pass=true` 时自动写入 `student_coach` 表建立绑定关系。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| pass | Boolean | 是 | true=通过, false=拒绝 |
| reason | String | 否 | 拒绝原因（pass=false 时建议填写） |

### GET /coach-applications/pending

查询状态为「待审核」的所有教练申请记录。

---

## 10. 教练分配

基础路径: `/coach-assignments` — 自动推荐 + 手动分配 + 查看/解绑。

| 方法 | URL | 说明 | 需要登录 |
|------|-----|------|----------|
| GET | `/coach-assignments/recommend` | 自动推荐教练 | 是 |
| POST | `/coach-assignments` | 手动分配教练 | 是 |
| GET | `/coach-assignments` | 查询所有绑定关系 | 是 |
| PUT | `/coach-assignments/{id}/unbind` | 解绑教练 | 是 |

### GET /coach-assignments/recommend

根据学员报考车型匹配准教车型，按评分降序返回推荐教练列表。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| studentId | Integer | 是 | 学员用户 ID |
| topN | Integer | 否 | 返回前 N 条，默认 5 |

#### 请求示例

```bash
curl "http://localhost:9500/coach-assignments/recommend?studentId=4&topN=3" \
  -H "Authorization: Bearer <token>"
```

### POST /coach-assignments

管理员直接为学员绑定教练。

#### 请求参数

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| studentId | Integer | 是 | 学员用户 ID |
| coachId | Integer | 是 | 教练 ID |

### GET /coach-assignments

查询所有绑定关系，返回含学员姓名和教练姓名的列表。

#### 响应示例

```json
{
  "state": 20000,
  "message": null,
  "data": [
    {
      "id": 1,
      "studentId": 4,
      "coachId": 1,
      "bindTime": "2026-05-22T08:26:46",
      "studentName": "王小明",
      "coachName": "张教练"
    }
  ]
}
```

### PUT /coach-assignments/{id}/unbind

解绑教练（将 `student_coach.status` 置为 0）。

---

## 11. 权限拦截规则

### 11.1 角色定义

| 角色值 | 名称 | 说明 |
|--------|------|------|
| 1 | 学员 (student) | 报名学车的普通学员 |
| 2 | 教练 (coach) | 驾校教练员 |
| 3 | 管理员 (admin) | 系统管理员，拥有最高权限 |

### 11.2 接口访问控制

当前权限层级分为两级：

| 访问级别 | 规则 | 说明 |
|----------|------|------|
| **公开** | 无需登录，无需 Token | 仅 `/login` 接口 |
| **登录可访问** | 需携带有效 Token | 当前所有业务接口（/users, /coaches, /appointments, /files 等） |
| **角色限制** | 需特定角色 + 有效 Token | `@RequireRole` 注解已定义，可应用于需要角色控制的接口 |

> **当前状态：** 所有业务接口仅验证 Token 有效性，未做角色级别拦截。如需对某接口限制角色，在 Controller 方法上添加 `@RequireRole` 注解即可，例如：
> - `@RequireRole(3)` — 仅管理员可访问
> - `@RequireRole({1,3})` — 学员和管理员可访问
> - `@RequireRole(2)` — 仅教练可访问

### 11.3 公开路径（无需 Token）

| 路径 | 说明 |
|------|------|
| `/login` | 登录接口 |
| `/doc.html` | Knife4j 接口文档页面 |
| `/swagger-ui/**` | Swagger UI 资源 |
| `/v3/api-docs/**` | OpenAPI 文档 |
| `/webjars/**` | WebJars 静态资源 |
| `/uploads/**` | 上传文件的静态访问 |

### 11.4 统一错误响应

#### 未登录（无 Token 或 Token 格式错误）

```json
{
  "state": 40100,
  "message": "未登录，请先登录",
  "data": null
}
```

#### Token 过期或无效

```json
{
  "state": 40100,
  "message": "登录已过期，请重新登录",
  "data": null
}
```

#### 权限不足（角色不符合 @RequireRole 要求）

```json
{
  "state": 40300,
  "message": "权限不足",
  "data": null
}
```

#### 请求参数错误

```json
{
  "state": 40000,
  "message": "具体的参数错误描述",
  "data": null
}
```

#### 数据不存在

```json
{
  "state": 40400,
  "message": "具体的数据不存在描述",
  "data": null
}
```

#### 数据冲突（如名额已满、重复绑定）

```json
{
  "state": 40900,
  "message": "具体的冲突描述",
  "data": null
}
```

#### 服务器内部错误

```json
{
  "state": 99999,
  "message": "服务器内部错误，请稍后重试",
  "data": null
}
```

---

## 12. 前端处理要点

### 12.1 Token 存储

推荐将 Token 存储在 `localStorage` 或 `sessionStorage` 中：

```javascript
// 登录成功后
localStorage.setItem('token', response.data.token);
localStorage.setItem('userId', response.data.userId);
localStorage.setItem('role', response.data.role);
localStorage.setItem('realName', response.data.realName);
```

**不建议**存储在 Cookie 中，避免 CSRF 风险。

### 12.2 Axios 请求拦截器注入 Token

```javascript
// axios 请求拦截器
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### 12.3 Axios 响应拦截器处理认证错误

```javascript
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.data) {
      const { state, message } = error.response.data;
      if (state === 40100 || state === 40300) {
        // 清除过期 Token
        localStorage.removeItem('token');
        localStorage.removeItem('role');
        // 跳转到登录页
        window.location.href = '/login';
        // 可显示提示消息
        alert(message);
      }
    }
    return Promise.reject(error);
  }
);
```

### 12.4 Token 过期判断

Token 有效期为 7 天（由服务端控制）。前端可通过以下方式判断：

**方式一（推荐）：** 解析 JWT 的 payload 获取 `exp`（过期时间戳）：

```javascript
function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    // exp 是 Unix 时间戳（秒）
    return Date.now() >= payload.exp * 1000;
  } catch {
    return true;
  }
}

// 页面初始化时检查
const token = localStorage.getItem('token');
if (token && isTokenExpired(token)) {
  localStorage.removeItem('token');
  window.location.href = '/login';
}
```

**方式二：** 在响应拦截器中捕获 401 响应（见上节），这种方式更加可靠。

### 12.5 角色相关的前端处理

```javascript
const role = localStorage.getItem('role'); // '1', '2', '3'

const roleNameMap = {
  '1': '学员',
  '2': '教练',
  '3': '管理员'
};

function hasRole(...allowedRoles) {
  return allowedRoles.includes(parseInt(role));
}

// 示例：菜单显示控制
const showAdminMenu = hasRole(3);        // 仅管理员
const showCoachMenu = hasRole(2);        // 仅教练
const showStudentMenu = hasRole(1);      // 仅学员
const showAssignmentMenu = hasRole(2, 3); // 教练和管理员
```

### 12.6 登录流程

```javascript
async function login(username, password) {
  try {
    const res = await axios.post('/login', { username, password });
    if (res.data.state === 20000) {
      const { token, userId, role, realName } = res.data.data;
      localStorage.setItem('token', token);
      localStorage.setItem('userId', userId);
      localStorage.setItem('role', role);
      localStorage.setItem('realName', realName);
      // 根据角色跳转到不同首页
      if (role === 3) {
        window.location.href = '/admin/dashboard';
      } else if (role === 2) {
        window.location.href = '/coach/dashboard';
      } else {
        window.location.href = '/student/dashboard';
      }
    }
  } catch (error) {
    alert('登录失败: ' + (error.response?.data?.message || '网络错误'));
  }
}
```

### 12.7 路由守卫（Vue Router 示例）

```javascript
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token');
  
  if (to.path === '/login') {
    next();
    return;
  }
  
  if (!token) {
    next('/login');
    return;
  }
  
  // 可选：检查 Token 是否过期
  if (isTokenExpired(token)) {
    localStorage.removeItem('token');
    next('/login');
    return;
  }
  
  // 可选：检查角色权限
  const role = parseInt(localStorage.getItem('role'));
  if (to.meta.roles && !to.meta.roles.includes(role)) {
    next('/403'); // 无权限页面
    return;
  }
  
  next();
});
```

### 12.8 登出

```javascript
function logout() {
  localStorage.removeItem('token');
  localStorage.removeItem('userId');
  localStorage.removeItem('role');
  localStorage.removeItem('realName');
  window.location.href = '/login';
}
```

---

## 13. 附录：全局响应格式

所有接口统一返回以下 JSON 结构：

```json
{
  "state": 20000,
  "message": null,
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| state | Integer | 业务状态码，20000 表示成功 |
| message | String/null | 失败时的提示信息，成功时为 null |
| data | Object/Array/null | 响应数据，成功时携带 |

### 状态码速查

| 状态码 | 含义 |
|--------|------|
| 20000 | 成功 |
| 40000 | 请求参数错误 |
| 40100 | 未登录或 Token 过期 |
| 40101 | 账号被禁用 |
| 40300 | 权限不足 |
| 40400 | 数据不存在 |
| 40900 | 数据冲突（名额已满、重复绑定等） |
| 50000 | 插入数据错误 |
| 50200 | 修改数据错误 |
| 99999 | 服务器内部错误 |
