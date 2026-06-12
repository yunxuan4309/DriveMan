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
     */
    LicenseUpgrade apply(Integer studentId, String targetLicense, Integer upgradeType, Integer licenseFileId);

    /**
     * 查询学员的增驾申请记录
     */
    List<LicenseUpgrade> listByStudent(Integer studentId);

    /**
     * 管理员审核增驾申请
     */
    void audit(Integer id, Integer status, String remark);

    /**
     * 录入增驾考试成绩
     */
    void recordExamResult(Integer id, Integer examStatus, String examRemark);

    /**
     * 分页查询增驾申请（含学员姓名），支持多条件搜索
     */
    Page<Map<String, Object>> pageSearch(Page<?> page, String keyword, String originalLicense,
                                          String targetLicense, Integer status, Integer examStatus,
                                          LocalDateTime createTimeStart, LocalDateTime createTimeEnd);
}
