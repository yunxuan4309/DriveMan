# 考试报名 Bug 修复 — 接口变更说明

> 日期：2026-06-10

---

## 问题一：学员已通过科目后可重复报名

### 现象

学员通过科目一考试后，仍可再次报名科目一的考试场次，系统未拦截。

### 修复

`POST /exam-registrations` 新增校验：报名时检查该学员是否已通过目标科目，已通过则返回错误，不再创建报名记录。

### 前端适配

无需改动。调用不变，新增的错误场景下前端展示提示信息即可：

> **错误响应示例**（状态码 400）：
> ```json
> {
>   "state": 40000,
>   "message": "您已通过科目1，无需重复报名",
>   "data": null
> }
> ```

---

## 问题二：管理员审核列表缺少按状态筛选

### 现象

`GET /exam-registrations`（管理员端）返回全部报名记录，所有状态混杂。待审核记录可能被历史记录埋没，管理员难以定位需要审核的报名。

### 修复

该接口新增可选参数 `status`，可按报名状态过滤。

### 接口变更

#### 请求

```http
GET /exam-registrations?page=1&size=10          ← 返回全部（行为不变）
GET /exam-registrations?page=1&size=10&status=0 ← 仅查待审核
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 10 |
| status | int | 否 | 状态：0-待审核, 1-审核通过, 2-审核不通过, 3-已考试 |

#### 响应

与原接口一致，返回 `Page<Map<String, Object>>`，每条记录包含：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int | 报名记录 ID |
| studentId | int | 学员 ID |
| studentName | String | 学员姓名 |
| sessionId | int | 场次 ID |
| subject | int | 科目（1-4） |
| status | int | 0-待审核, 1-审核通过, 2-审核不通过, 3-已考试 |
| score | int | 成绩 |
| passStatus | int | 0-不合格, 1-合格 |
| retakeCount | int | 补考次数 |
| isRetake | int | 0-首次, 1-补考 |
| examDate | LocalDate | 考试日期 |
| location | String | 考试地点 |
| licenseType | String | 车型 |
| applyTime | LocalDateTime | 报名时间 |
| auditTime | LocalDateTime | 审核时间 |

### 前端适配建议

管理员审核页面请求时加 `status=0` 参数，只拉取待审核记录：

```http
GET /exam-registrations?page=1&size=20&status=0
```

---

## 受影响的文件

| 文件 | 变更 |
|------|------|
| `ExamRegistrationController.java` | 报名接口新增已通过科目校验；列表接口新增 status 参数 |
| `IExamRegistrationService.java` | `pageWithDetails` 方法签名增加 `status` 参数 |
| `ExamRegistrationServiceImpl.java` | `pageWithDetails` 实现增加 Lambda 条件筛选 + 按报名时间倒序 |
