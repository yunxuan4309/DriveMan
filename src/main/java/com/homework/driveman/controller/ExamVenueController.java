package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.ExamVenue;
import com.homework.driveman.service.IExamVenueService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 考场信息管理控制器 — 管理员维护考场基础信息
 * 管理员可以新增/修改/删除/查询考场
 */
@Tag(name = "考场信息管理")
@RestController
@RequestMapping("/exam-venues")
public class ExamVenueController {

    @Autowired
    private IExamVenueService examVenueService;

    @RequireRole(3)
    @Operation(summary = "查询所有考场", description = "返回全部考场列表，按名称排序")
    @GetMapping
    public JsonResult<List<ExamVenue>> list() {
        List<ExamVenue> list = examVenueService.lambdaQuery()
                .orderByAsc(ExamVenue::getName)
                .list();
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "根据ID查询考场")
    @GetMapping("/{id}")
    public JsonResult<ExamVenue> getById(@PathVariable Integer id) {
        return JsonResult.ok(examVenueService.getById(id));
    }

    @RequireRole(3)
    @Operation(summary = "新增考场")
    @PostMapping
    public JsonResult<Void> create(@RequestBody ExamVenue examVenue) {
        examVenueService.save(examVenue);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改考场信息")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody ExamVenue examVenue) {
        examVenue.setId(id);
        examVenueService.updateById(examVenue);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除考场", description = "逻辑删除考场")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        examVenueService.removeById(id);
        return JsonResult.ok();
    }
}
