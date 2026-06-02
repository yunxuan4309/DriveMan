package com.homework.driveman.service;

import java.util.Map;

/** 学员进度查询服务 */
public interface IProgressService {

    /**
     * 查询某学员的进度
     * @param studentId 学员 user_id
     * @return ECharts 兼容的进度 JSON 数据
     */
    Map<String, Object> getProgress(Integer studentId);
}
