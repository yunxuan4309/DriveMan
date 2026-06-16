package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.Vehicle;

public interface IVehicleService extends IService<Vehicle> {

    /**
     * 分页+多条件搜索车辆
     * @param page         分页参数
     * @param vehicleType  车型（精确匹配）
     * @param status       状态（精确匹配）
     * @param plateNumber  车牌号（模糊）
     * @param brand        品牌（模糊）
     * @param model        型号（模糊）
     * @param seats        座位数（精确匹配）
     */
    Page<Vehicle> pageSearch(Page<Vehicle> page, String vehicleType, Integer status,
                             String plateNumber, String brand, String model, Integer seats);

    /**
     * 检查车辆是否有未结束的已通过排班
     * @param vehicleId 车辆ID
     * @return true=有活跃排班
     */
    boolean hasActiveSchedules(Integer vehicleId);
}
