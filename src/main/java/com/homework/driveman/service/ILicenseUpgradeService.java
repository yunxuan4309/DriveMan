package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.LicenseUpgrade;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 增驾申请服务接口
 */
public interface ILicenseUpgradeService extends IService<LicenseUpgrade> {

    /**
     * 提交增驾申请
     * @param studentId 学员ID
     * @param targetLicense 目标准驾车型
     * @param upgradeType 增驾类型: 1-同级增驾, 2-升级增驾
     * @param licenseFileId 驾驶证材料文件ID（升级增驾时必填）
     * @param skipAgeCheck 是否跳过年龄/路径/驾龄校验（演示用，默认 false）
     */
    LicenseUpgrade apply(Integer studentId, String targetLicense, Integer upgradeType, Integer licenseFileId, boolean skipAgeCheck);

    /**
     * 查询学员的增驾申请记录
     */
    List<LicenseUpgrade> listByStudent(Integer studentId);

    /**
     * 管理员审核增驾申请
     * @param skipSubjects 跳过的科目编号（逗号分隔如 "1,3"），不能跳过全部科目
     */
    void audit(Integer id, Integer status, String remark, String skipSubjects);

    /**
     * 查询增驾进度
     * @return Map 包含: upgrade, paid, skippedSubjects, passedSubjects, pendingSubjects, allPassed
     */
    Map<String, Object> getProgress(Integer id);

    /**
     * 完成增驾（检查所有未免考科目已通过 + 已缴费，更新学员车型）
     */
    void completeUpgrade(Integer id);

    /**
     * 分页查询增驾申请（含学员姓名），支持多条件搜索
     */
    Page<Map<String, Object>> pageSearch(Page<?> page, String keyword, String originalLicense,
                                          String targetLicense, Integer status, Integer examStatus,
                                          LocalDateTime createTimeStart, LocalDateTime createTimeEnd);
}
