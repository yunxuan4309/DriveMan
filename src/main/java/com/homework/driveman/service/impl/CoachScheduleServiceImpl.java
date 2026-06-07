package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.entity.CoachSchedule;
import com.homework.driveman.entity.StudentCoach;
import com.homework.driveman.entity.Vehicle;
import com.homework.driveman.entity.Venue;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.CoachMapper;
import com.homework.driveman.mapper.CoachScheduleMapper;
import com.homework.driveman.mapper.StudentCoachMapper;
import com.homework.driveman.mapper.VehicleMapper;
import com.homework.driveman.mapper.VenueMapper;
import com.homework.driveman.service.ICoachScheduleService;
import com.homework.driveman.web.ServiceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class CoachScheduleServiceImpl extends ServiceImpl<CoachScheduleMapper, CoachSchedule> implements ICoachScheduleService {

    @Autowired
    private CoachMapper coachMapper;

    @Autowired
    private VehicleMapper vehicleMapper;

    @Autowired
    private VenueMapper venueMapper;

    @Autowired
    private StudentCoachMapper studentCoachMapper;

    @Override
    @Transactional
    public void apply(CoachSchedule schedule) {
        // 1. 时间合法性
        if (schedule.getStartTime() == null || schedule.getEndTime() == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "开始时间和结束时间不能为空");
        }
        if (!schedule.getStartTime().isBefore(schedule.getEndTime())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "开始时间必须早于结束时间");
        }
        if (schedule.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "排班开始时间必须在未来");
        }

        // 2. 教练存在性 + 车型匹配
        Coach coach = coachMapper.selectById(schedule.getCoachId());
        if (coach == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "教练不存在");
        }
        if (coach.getVehicleType() == null || !containsType(coach.getVehicleType(), schedule.getLicenseType())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    "您的准教车型(" + coach.getVehicleType() + ")不包含" + schedule.getLicenseType());
        }

        // 3. 车辆存在性 + 车型匹配 + 状态
        Vehicle vehicle = vehicleMapper.selectById(schedule.getVehicleId());
        if (vehicle == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "车辆不存在");
        }
        if (!schedule.getLicenseType().equals(vehicle.getVehicleType())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    "该车辆车型为" + vehicle.getVehicleType() + "，不支持" + schedule.getLicenseType() + "培训");
        }
        if (vehicle.getStatus() != 1) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "该车辆当前不可用（状态：" + getVehicleStatusDesc(vehicle.getStatus()) + "）");
        }

        // 4. 场地存在性 + 车型匹配
        Venue venue = venueMapper.selectById(schedule.getVenueId());
        if (venue == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "场地不存在");
        }
        if (venue.getVenueType() != 2) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "该场地不是训练场地（类型：" + venue.getVenueType() + "）");
        }
        if (venue.getSupportedTypes() != null && !venue.getSupportedTypes().isEmpty()
                && !containsType(venue.getSupportedTypes(), schedule.getLicenseType())) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST,
                    "该场地不支持" + schedule.getLicenseType() + "车型训练");
        }
        if (venue.getStatus() != null && venue.getStatus() != 1) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "该场地已停用");
        }

        // 5. 车辆冲突检测
        long vehicleConflict = countConflicting(schedule.getVehicleId(), null,
                schedule.getStartTime(), schedule.getEndTime());
        if (vehicleConflict > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该车辆在此时段已被占用");
        }

        // 6. 场地容量检测
        if (venue.getMaxVehicles() != null && venue.getMaxVehicles() > 0) {
            long venueCount = countConflicting(null, schedule.getVenueId(),
                    schedule.getStartTime(), schedule.getEndTime());
            if (venueCount >= venue.getMaxVehicles()) {
                throw new ServiceException(ServiceCode.ERROR_CONFLICT,
                        "该场地在此时段已达到最大容纳车辆数(" + venue.getMaxVehicles() + ")");
            }
        }

        // 7. 教练冲突检测
        long coachConflict = countConflicting(null, null,
                schedule.getStartTime(), schedule.getEndTime(), schedule.getCoachId());
        if (coachConflict > 0) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "您在此时段已有其他排班");
        }

        schedule.setBookedCount(0);
        schedule.setStatus(0); // 待审核
        schedule.setApplyTime(LocalDateTime.now());
        save(schedule);
    }

    @Override
    @Transactional
    public void audit(Integer scheduleId, Integer status, String remark) {
        CoachSchedule schedule = getById(scheduleId);
        if (schedule == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "排班记录不存在");
        }
        if (schedule.getStatus() != 0) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "该排班已被处理，当前状态：" + getStatusDesc(schedule.getStatus()));
        }
        if (status != 1 && status != 2) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "审核状态只能为 1(通过) 或 2(拒绝)");
        }
        schedule.setStatus(status);
        schedule.setAuditRemark(remark);
        schedule.setAuditTime(LocalDateTime.now());
        updateById(schedule);
    }

    @Override
    @Transactional
    public void cancel(Integer scheduleId, Integer coachId) {
        CoachSchedule schedule = getById(scheduleId);
        if (schedule == null) {
            throw new ServiceException(ServiceCode.ERROR_NOT_FOUND, "排班记录不存在");
        }
        if (!schedule.getCoachId().equals(coachId)) {
            throw new ServiceException(ServiceCode.ERROR_FORBIDDEN, "无权取消其他教练的排班");
        }
        if (schedule.getStatus() != 0 && schedule.getStatus() != 1) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "当前状态不允许取消");
        }
        schedule.setStatus(4); // 已取消
        updateById(schedule);
    }

    @Override
    public List<CoachSchedule> listAvailableForStudent(Integer studentId, String licenseType) {
        // 查学员绑定教练
        StudentCoach binding = studentCoachMapper.selectOne(
                new LambdaQueryWrapper<StudentCoach>()
                        .eq(StudentCoach::getStudentId, studentId)
                        .eq(StudentCoach::getStatus, 1));
        if (binding == null) {
            throw new ServiceException(ServiceCode.ERROR_BAD_REQUEST, "您还没有绑定教练，请先申请分配教练");
        }

        LambdaQueryWrapper<CoachSchedule> wrapper = new LambdaQueryWrapper<CoachSchedule>()
                .eq(CoachSchedule::getCoachId, binding.getCoachId())
                .eq(CoachSchedule::getStatus, 1)
                .gt(CoachSchedule::getStartTime, LocalDateTime.now())
                .apply("booked_count < max_students")
                .orderByAsc(CoachSchedule::getStartTime);

        if (licenseType != null && !licenseType.isEmpty()) {
            wrapper.eq(CoachSchedule::getLicenseType, licenseType);
        }

        return list(wrapper);
    }

    // ==================== 工具方法 ====================

    /**
     * 检测指定车辆/场地/教练在目标时段内的冲突排班数
     */
    private long countConflicting(Integer vehicleId, Integer venueId,
                                   LocalDateTime start, LocalDateTime end, Integer coachId) {
        LambdaQueryWrapper<CoachSchedule> wrapper = new LambdaQueryWrapper<CoachSchedule>()
                .eq(vehicleId != null, CoachSchedule::getVehicleId, vehicleId)
                .eq(venueId != null, CoachSchedule::getVenueId, venueId)
                .eq(coachId != null, CoachSchedule::getCoachId, coachId)
                .eq(CoachSchedule::getStatus, 1) // 只检测已通过的排班
                .lt(CoachSchedule::getStartTime, end)
                .gt(CoachSchedule::getEndTime, start);
        return count(wrapper);
    }

    private long countConflicting(Integer vehicleId, Integer venueId,
                                   LocalDateTime start, LocalDateTime end) {
        return countConflicting(vehicleId, venueId, start, end, null);
    }

    private boolean containsType(String typeList, String target) {
        if (typeList == null || target == null) return false;
        for (String t : typeList.split(",")) {
            if (t.trim().equals(target)) return true;
        }
        return false;
    }

    private String getStatusDesc(Integer status) {
        return switch (status) {
            case 0 -> "待审核";
            case 1 -> "已通过";
            case 2 -> "已拒绝";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }

    private String getVehicleStatusDesc(Integer status) {
        return switch (status) {
            case 1 -> "空闲";
            case 2 -> "使用中";
            case 3 -> "维修";
            case 4 -> "报废";
            default -> "未知";
        };
    }
}
