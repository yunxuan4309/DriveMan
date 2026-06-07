package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.SpecialPersonRecord;

import java.time.LocalDate;
import java.util.List;

/**
 * 特殊人群记录服务接口
 */
public interface ISpecialPersonRecordService extends IService<SpecialPersonRecord> {

    /**
     * 学员提交特殊人群记录
     */
    SpecialPersonRecord submit(Integer userId, Integer recordType, LocalDate recordDate,
                               Integer banYears, String courtDocNo, Integer courtDocFileId);

    /**
     * 根据用户ID查询所有记录
     */
    List<SpecialPersonRecord> listByUserId(Integer userId);

    /**
     * 管理员审核记录
     */
    void audit(Integer id, Integer auditStatus, String auditRemark, Integer auditUserId);

    /**
     * 查询待审核记录列表
     */
    List<SpecialPersonRecord> listPending();

    /**
     * 检查用户当前是否处于禁驾期
     * @return true-处于禁驾期（不能报名），false-无限制或禁驾期已过
     */
    boolean isInBanPeriod(Integer userId);

    /**
     * 获取用户的禁驾截止日期（取所有记录中最晚的日期）
     * @return null-无限制，LocalDate.MAX-终生禁驾，其他-具体截止日期
     */
    LocalDate getBanEndDate(Integer userId);
}
