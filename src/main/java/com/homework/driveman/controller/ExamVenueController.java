package com.homework.driveman.controller;

import com.homework.driveman.entity.Venue;
import com.homework.driveman.service.IVenueService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 考场查询（兼容旧路径 /exam-venues → venueType=1）
 * 场地统一管理后，考场数据存储在 venue 表中（venue_type=1）
 */
@Tag(name = "场地管理")
@RestController
public class ExamVenueController {

    @Autowired
    private IVenueService venueService;

    @Operation(summary = "查询考场列表（兼容路径）",
            description = "返回所有考场（venue_type=1），旧版 /exam-venues 路径兼容")
    @GetMapping("/exam-venues")
    public JsonResult<List<Venue>> listExamVenues() {
        List<Venue> list = venueService.lambdaQuery()
                .eq(Venue::getVenueType, 1)
                .orderByAsc(Venue::getName)
                .list();
        return JsonResult.ok(list);
    }
}
