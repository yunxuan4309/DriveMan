package com.homework.driveman.service;

import com.homework.driveman.entity.FamiliarizationRecord;

import java.util.List;
import java.util.Map;

/**
 * 合场记录服务 — 学员申请合场、支付、管理
 */
public interface IFamiliarizationRecordService {

    /** 学员申请合场（自动生成费用 + 支付记录） */
    FamiliarizationRecord apply(Integer studentId, Integer examSessionId, Integer carType);

    /** 学员支付合场 */
    FamiliarizationRecord pay(Integer id, Integer studentId);

    /** 管理员安排时间 */
    FamiliarizationRecord schedule(Integer id, String scheduledTime);

    /** 管理员标记完成 */
    FamiliarizationRecord complete(Integer id);

    /** 取消 */
    FamiliarizationRecord cancel(Integer id);

    /** 查询我的合场记录 */
    List<Map<String, Object>> listMyRecords(Integer studentId);

    /** 管理员查询所有 */
    List<Map<String, Object>> listAll();
}
