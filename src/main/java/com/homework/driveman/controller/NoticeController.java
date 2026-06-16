package com.homework.driveman.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.Notice;
import com.homework.driveman.service.INoticeService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统公告管理控制器 — 管理员发布/管理公告
 * 公告支持设置过期时间，过期后前端不再展示
 */
@Tag(name = "系统公告管理")
@RestController
@RequestMapping("/notices")
public class NoticeController {

    @Autowired
    private INoticeService noticeService;

    @Operation(summary = "查询所有有效公告", description = "返回所有未过期、未删除的公告，按发布时间倒序")
    @GetMapping("/active")
    public JsonResult<List<Notice>> listActive() {
        // 查询未过期且未删除的公告
        List<Notice> list = noticeService.lambdaQuery()
                .eq(Notice::getIsDeleted, 0)
                .and(wrapper -> wrapper.isNull(Notice::getExpireTime)
                        .or(w -> w.ge(Notice::getExpireTime, LocalDateTime.now())))
                .orderByDesc(Notice::getPublishTime)
                .list();
        return JsonResult.ok(list);
    }

    @RequireRole(3)
    @Operation(summary = "分页查询所有公告（含过期）", description = "管理员分页查看全部公告，支持标题搜索")
    @GetMapping
    public JsonResult<Page<Notice>> listAll(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String title) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                .like(title != null && !title.isEmpty(), Notice::getTitle, title)
                .orderByDesc(Notice::getPublishTime);
        Page<Notice> noticePage = noticeService.page(new Page<>(page, size), wrapper);
        return JsonResult.ok(noticePage);
    }

    @RequireRole(3)
    @Operation(summary = "根据ID查询公告")
    @GetMapping("/{id}")
    public JsonResult<Notice> getById(@PathVariable Integer id) {
        Notice notice = noticeService.getById(id);
        return JsonResult.ok(notice);
    }

    @RequireRole(3)
    @Operation(summary = "发布公告", description = "管理员发布新公告，需指定标题、内容和过期时间（可选）")
    @PostMapping
    public JsonResult<Void> create(@RequestBody Notice notice) {
        // 设置发布时间为当前时间
        notice.setPublishTime(LocalDateTime.now());
        noticeService.save(notice);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "修改公告", description = "修改公告标题、内容或过期时间")
    @PutMapping("/{id}")
    public JsonResult<Void> update(@PathVariable Integer id, @RequestBody Notice notice) {
        notice.setId(id);
        noticeService.updateById(notice);
        return JsonResult.ok();
    }

    @RequireRole(3)
    @Operation(summary = "删除公告", description = "逻辑删除公告")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        noticeService.removeById(id);
        return JsonResult.ok();
    }
}