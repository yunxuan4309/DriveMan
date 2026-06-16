package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.CoachSchedule;
import com.homework.driveman.entity.Vehicle;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.mapper.CoachScheduleMapper;
import com.homework.driveman.mapper.VehicleMapper;
import com.homework.driveman.service.IVehicleService;
import com.homework.driveman.web.ServiceCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VehicleServiceImpl extends ServiceImpl<VehicleMapper, Vehicle> implements IVehicleService {

    @Autowired
    private CoachScheduleMapper coachScheduleMapper;

    @Override
    public boolean hasActiveSchedules(Integer vehicleId) {
        Long count = coachScheduleMapper.selectCount(
                new LambdaQueryWrapper<CoachSchedule>()
                        .eq(CoachSchedule::getVehicleId, vehicleId)
                        .eq(CoachSchedule::getStatus, 1) // 已通过
                        .gt(CoachSchedule::getEndTime, LocalDateTime.now())); // 未结束
        return count > 0;
    }

    @Override
    public boolean updateById(Vehicle entity) {
        // 当修改状态为维修(3)或报废(4)时，检查是否有活跃排班
        if (entity.getStatus() != null && (entity.getStatus() == 3 || entity.getStatus() == 4)) {
            if (hasActiveSchedules(entity.getId())) {
                throw new ServiceException(ServiceCode.ERROR_CONFLICT,
                        "该车辆有正在进行或即将开始的排班，无法修改为维修/报废状态");
            }
        }
        return super.updateById(entity);
    }

    @Override
    public Page<Vehicle> pageSearch(Page<Vehicle> page, String vehicleType, Integer status,
                                     String plateNumber, String brand, String model, Integer seats) {
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<Vehicle>()
                .eq(vehicleType != null && !vehicleType.isEmpty(), Vehicle::getVehicleType, vehicleType)
                .eq(status != null, Vehicle::getStatus, status)
                .like(plateNumber != null && !plateNumber.isEmpty(), Vehicle::getPlateNumber, plateNumber)
                .like(brand != null && !brand.isEmpty(), Vehicle::getBrand, brand)
                .like(model != null && !model.isEmpty(), Vehicle::getModel, model)
                .eq(seats != null, Vehicle::getSeats, seats)
                .orderByAsc(Vehicle::getVehicleType)
                .orderByAsc(Vehicle::getId);
        return baseMapper.selectPage(page, wrapper);
    }
}
