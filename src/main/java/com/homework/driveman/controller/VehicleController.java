package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.Vehicle;
import com.homework.driveman.service.IVehicleService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教练车管理控制器 — 仅管理员可操作
 */
@Tag(name = "车辆管理")
@RestController
@RequestMapping("/vehicles")
public class VehicleController {

    @Autowired
    private IVehicleService vehicleService;

    @RequireRole(3)
    @Operation(summary = "分页查询车辆列表", description = "支持按车型、状态筛选")
    @GetMapping
    public JsonResult<Page<Vehicle>> list(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String vehicleType,
                                           @RequestParam(required = false) Integer status) {
        Page<Vehicle> result = vehicleService.lambdaQuery()
                .eq(vehicleType != null, Vehicle::getVehicleType, vehicleType)
                .eq(status != null, Vehicle::getStatus, status)
                .orderByAsc(Vehicle::getVehicleType)
                .orderByAsc(Vehicle::getId)
                .page(new Page<>(page, size));
        return JsonResult.ok(result);
    }

    @RequireRole({2, 3})
    @Operation(summary = "查询所有可用车辆", description = "返回 status=1(空闲) 的车辆列表，供教练申请排班时选择")
    @GetMapping("/available")
    public JsonResult<List<Vehicle>> listAvailable(@RequestParam(required = false) String vehicleType) {
        List<Vehicle> list = vehicleService.lambdaQuery()
                .eq(vehicleType != null, Vehicle::getVehicleType, vehicleType)
                .eq(Vehicle::getStatus, 1)
                .orderByAsc(Vehicle::getVehicleType)
                .list();
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "根据ID查询车辆")
    @GetMapping("/{id}")
    public JsonResult<Vehicle> getById(@PathVariable Integer id) {
        return JsonResult.ok(vehicleService.getById(id));
    }

    @RequireRole(3)
    @Operation(summary = "新增车辆")
    @PostMapping
    public JsonResult<Void> create(@RequestBody Vehicle vehicle) {
        vehicleService.save(vehicle);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改车辆信息")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody Vehicle vehicle) {
        vehicle.setId(id);
        vehicleService.updateById(vehicle);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除车辆", description = "逻辑删除")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        vehicleService.removeById(id);
        return JsonResult.ok();
    }
}
