# DriveMan 开发计划

## P0 — 已完成

- [x] **教练工作量 → 教练效能**
  - 以前：按约课数排序的柱状图，不驱动任何管理决策
  - 现在：按学员考试通过率排名，附带评分、带教学员数、执教年限明细
  - 数据源：coach + exam_registration + student_coach 三表联查
  - 接口：`GET /statistics/coach-workload`（路径不变，返回从约课数改为通过率柱状图 + detailData）

- [x] **考试合格率饼图 → 月度趋势折线图**
  - 以前：静态合格/不合格饼图，看完了无对应动作
  - 现在：按科目拆分、按月展示通过率变化趋势，可监控教学质量波动（哪个科目掉下去了）
  - 数据源：exam_registration + exam_session 联查 join exam_date
  - 接口：`GET /statistics/pass-rate`（路径不变，返回从饼图改为多折线图）

- [x] **支付记录表（payment_record）**
  - 数据库表 + 升级脚本 + 实体/Mapper/Service/Controller
  - 管理员 CRUD + 确认支付 + 退款 + 欠费清单
  - **审核通过时自动生成账单**：报名审核通过 → 报名费 | 考试审核通过 → 考试费
  - **学员端**：`GET /my` 看账单 + `PUT /{id}/my-pay` 支付（归属校验）
  - 收入看板：`GET /statistics/revenue-summary`

## P1 — 待评估

- [ ] **合场功能（familiarization_fee 业务）**
  - 已在 payment_record.biz_type 预留 familiarization_fee 类型
  - 等合场业务表设计时接入

## P2 — 暂缓（等基础功能稳定后再考虑）

- [ ] **学员转化漏斗分析**
  - 注册 → 选教练 → 训练 → 首考 → 拿证，各环节流失率
  - 用于诊断业务瓶颈（注册未约课？训练未考试？）
  - 暂缓原因：需要较多前端配合，且基础模块需先稳定
