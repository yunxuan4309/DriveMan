package com.homework.driveman.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.homework.driveman.entity.Vehicle;
import com.homework.driveman.mapper.VehicleMapper;
import com.homework.driveman.service.IVehicleService;
import org.springframework.stereotype.Service;

@Service
public class VehicleServiceImpl extends ServiceImpl<VehicleMapper, Vehicle> implements IVehicleService {

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
