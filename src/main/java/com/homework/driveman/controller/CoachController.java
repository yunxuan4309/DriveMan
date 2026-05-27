package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.Coach;
import com.homework.driveman.service.ICoachService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教练管理控制器 — 教练信息的 CRUD 接口
 */
@Tag(name = "教练管理")
@RestController
@RequestMapping("/coaches")
public class CoachController {

    @Autowired
    private ICoachService coachService;

    @Operation(summary = "查询所有教练")
    @GetMapping
    public JsonResult<List<Coach>> list() {
        List<Coach> list = coachService.list();
        return JsonResult.ok(list);
    }

    @Operation(summary = "根据ID查询教练")
    @GetMapping("/{id}")
    public JsonResult<Coach> getById(@PathVariable Integer id) {
        Coach coach = coachService.getById(id);
        return JsonResult.ok(coach);
    }

    @RequireRole(3)
    @Operation(summary = "新增教练")
    @PostMapping
    public JsonResult<Void> add(@RequestBody Coach coach) {
        coachService.save(coach);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改教练")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody Coach coach) {
        coach.setCoachId(id);
        coachService.updateById(coach);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除教练")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        coachService.removeById(id);
        return JsonResult.ok();
    }
}
