package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.CoachVehicleApplication;
import com.homework.driveman.entity.LicenseConfig;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.CoachVehicleApplicationMapper;
import com.homework.driveman.mapper.LicenseConfigMapper;
import com.homework.driveman.service.ICoachVehicleApplicationService;
import com.homework.driveman.web.ServiceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 教练准教车型变更业务实现 */
@Service
public class CoachVehicleApplicationServiceImpl
        extends ServiceImpl<CoachVehicleApplicationMapper, CoachVehicleApplication>
        implements ICoachVehicleApplicationService {

    @Autowired
    private CoachMapper coachMapper;

    @Autowired
    private LicenseConfigMapper licenseConfigMapper;

    /** 从 license_config 表加载所有有效的 license_type */
    private Set<String> getValidVehicleTypes() {
        return licenseConfigMapper.selectList(
                        new LambdaQueryWrapper<LicenseConfig>()
                                .select(LicenseConfig::getLicenseType)
                                .groupBy(LicenseConfig::getLicenseType))
                .stream()
                .map(LicenseConfig::getLicenseType)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public void submitApplication(Integer coachId, String requestedVehicleType, String applyReason) {
        // 校验教练存在
        Coach coach = coachMapper.selectById(coachId);
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练信息不存在");
        }

        // 校验申请车型不能为空
        if (requestedVehicleType == null || requestedVehicleType.isBlank()) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "申请准教车型不能为空");
        }

        // 校验每个车型是否在系统中存在
        Set<String> validTypes = getValidVehicleTypes();
        String[] types = requestedVehicleType.split(",");
        for (String type : types) {
            String trimmed = type.trim();
            if (!validTypes.contains(trimmed)) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                        "准教车型 " + trimmed + " 不存在，系统支持的车型为: " + String.join(", ", validTypes));
            }
        }

        // 校验不能与当前一致
        String current = coach.getVehicleType();
        if (requestedVehicleType.equals(current)) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "申请车型与当前准教车型一致，无需变更");
        }

        // 校验是否存在待审核申请
        Long pendingCount = baseMapper.selectCount(
                new LambdaQueryWrapper<CoachVehicleApplication>()
                        .eq(CoachVehicleApplication::getCoachId, coachId)
                        .eq(CoachVehicleApplication::getStatus, 0));
        if (pendingCount > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "您已有一条待审核的申请，请等待处理");
        }

        CoachVehicleApplication application = new CoachVehicleApplication();
        application.setCoachId(coachId);
        application.setCurrentVehicleType(current);
        application.setRequestedVehicleType(requestedVehicleType);
        application.setApplyReason(applyReason);
        application.setStatus(0);
        application.setApplyTime(LocalDateTime.now());
        baseMapper.insert(application);
    }

    @Override
    @Transactional
    public void audit(Integer id, boolean approved, String auditReason) {
        CoachVehicleApplication application = baseMapper.selectById(id);
        if (application == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "申请记录不存在");
        }
        if (application.getStatus() != 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该申请已处理，无法重复操作");
        }

        if (approved) {
            // 通过：更新 coach 表的 vehicle_type
            Coach coach = coachMapper.selectById(application.getCoachId());
            if (coach == null) {
                throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练信息不存在");
            }
            coach.setVehicleType(application.getRequestedVehicleType());
            coachMapper.updateById(coach);

            application.setStatus(1);
        } else {
            // 拒绝：记录原因
            if (auditReason == null || auditReason.isBlank()) {
                throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "拒绝时必须填写原因");
            }
            application.setStatus(2);
            application.setAuditReason(auditReason);
        }

        application.setAuditTime(LocalDateTime.now());
        baseMapper.updateById(application);
    }

    @Override
    public Page<Map<String, Object>> pagePending(Page<?> page, String keyword, String currentVehicleType,
                                                  String requestedVehicleType,
                                                  LocalDateTime applyTimeStart, LocalDateTime applyTimeEnd) {
        return baseMapper.selectPagePending(page, keyword, currentVehicleType,
                requestedVehicleType, applyTimeStart, applyTimeEnd);
    }

    @Override
    public Page<Map<String, Object>> pageAll(Page<?> page, String keyword, String vehicleType,
                                              Integer status,
                                              LocalDateTime auditTimeStart, LocalDateTime auditTimeEnd) {
        return baseMapper.selectPageAll(page, keyword, vehicleType, status,
                auditTimeStart, auditTimeEnd);
    }
}
