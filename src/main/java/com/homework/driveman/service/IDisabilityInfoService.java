package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.DisabilityInfo;

import java.util.List;
import java.util.Map;

/**
 * 残疾人信息服务接口（简化版）
 */
public interface IDisabilityInfoService extends IService<DisabilityInfo> {

    /**
     * 提交残疾信息（学员首次填写）
     */
    DisabilityInfo submit(Integer userId, Integer disabilityType, String certificateNo,
                          Integer certificateFileId);

    /**
     * 根据用户ID查询残疾信息
     */
    DisabilityInfo getByUserId(Integer userId);

    /**
     * 管理员审核残疾信息
     */
    void audit(Integer id, Integer auditStatus, String auditRemark);

    /**
     * 查询待审核的残疾信息列表
     */
    List<DisabilityInfo> listPending();

    /**
     * 查询用户是否已通过残疾信息审核
     */
    boolean isAuditPassed(Integer userId);

    /**
     * 分页查询残疾信息（含学员姓名）
     */
    Page<Map<String, Object>> pageWithDetails(Page<DisabilityInfo> page, Integer auditStatus, String keyword);
}
