package com.homework.driveman.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.Vehicle;
import com.homework.driveman.exception.ServiceException;
import com.homework.driveman.service.IVehicleService;
import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(summary = "分页查询车辆列表",
            description = "支持车型/状态精确匹配 + 车牌号/品牌/型号模糊搜索 + 座位数精确匹配")
    @GetMapping
    public JsonResult<Page<Vehicle>> list(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) @Parameter(description = "车型，精确匹配") String vehicleType,
                                           @RequestParam(required = false) @Parameter(description = "状态：1-空闲,2-使用中,3-维修,4-报废") Integer status,
                                           @RequestParam(required = false) @Parameter(description = "车牌号，模糊搜索") String plateNumber,
                                           @RequestParam(required = false) @Parameter(description = "品牌，模糊搜索") String brand,
                                           @RequestParam(required = false) @Parameter(description = "型号，模糊搜索") String model,
                                           @RequestParam(required = false) @Parameter(description = "座位数，精确匹配") Integer seats) {
        Page<Vehicle> result = vehicleService.pageSearch(new Page<>(page, size),
                vehicleType, status, plateNumber, brand, model, seats);
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
    @Operation(summary = "删除车辆", description = "逻辑删除。如有活跃排班则禁止删除")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        if (vehicleService.hasActiveSchedules(id)) {
            throw new ServiceException(ServiceCode.ERROR_CONFLICT, "该车辆有正在进行或即将开始的排班，无法删除");
        }
        vehicleService.removeById(id);
        return JsonResult.ok();
    }
}
