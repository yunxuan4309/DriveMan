package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.Venue;
import com.homework.driveman.service.IVenueService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 场地管理控制器 — 统一管理考场/训练场地/体检地点
 * 管理员可以按类型查询、新增、修改、删除场地
 */
@Tag(name = "场地管理")
@RestController
@RequestMapping("/venues")
public class VenueController {

    @Autowired
    private IVenueService venueService;

    @RequireRole(3)
    @Operation(summary = "查询场地列表", description = "按场地类型筛选，venueType=1考场 2训练场地 3体检地点，不传则返回全部")
    @GetMapping
    public JsonResult<List<Venue>> list(@RequestParam(required = false) Integer venueType) {
        List<Venue> list = venueService.lambdaQuery()
                .eq(venueType != null, Venue::getVenueType, venueType)
                .orderByAsc(Venue::getName)
                .list();
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "根据ID查询场地")
    @GetMapping("/{id}")
    public JsonResult<Venue> getById(@PathVariable Integer id) {
        return JsonResult.ok(venueService.getById(id));
    }

    @RequireRole(3)
    @Operation(summary = "新增场地")
    @PostMapping
    public JsonResult<Void> create(@RequestBody Venue venue) {
        venueService.save(venue);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改场地信息")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody Venue venue) {
        venue.setId(id);
        venueService.updateById(venue);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除场地", description = "逻辑删除场地")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        venueService.removeById(id);
        return JsonResult.ok();
    }
}
