package com.homework.driveman.controller;

import com.homework.driveman.config.RequireRole;
import com.homework.driveman.entity.File;
import com.homework.driveman.service.IFileService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * 文件上传下载控制器
 * 上传文件存储在本地 {uploadPath}/{fileType}/ 目录，通过 /uploads/** 直接访问
 */
@Tag(name = "文件管理")
@RestController
@RequestMapping("/files")
public class FileController {

    @Autowired
    private IFileService fileService;

    @Operation(summary = "上传文件",
            description = "fileType: id_card_front / id_card_back / physical_exam / registration_pdf / admission_ticket")
    @PostMapping("/upload")
    public JsonResult<File> upload(@RequestParam Integer userId,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam String fileType) {
        File saved = fileService.upload(userId, file, fileType);
        return JsonResult.ok(saved);
    }

    @Operation(summary = "根据ID查询文件记录")
    @GetMapping("/{id}")
    public JsonResult<File> getById(@PathVariable Integer id) {
        File file = fileService.getById(id);
        return JsonResult.ok(file);
    }

    @Operation(summary = "查询某个用户的所有文件")
    @GetMapping("/user/{userId}")
    public JsonResult<List<File>> listByUser(@PathVariable Integer userId) {
        List<File> list = fileService.lambdaQuery()
                .eq(File::getUserId, userId)
                .orderByDesc(File::getCreateTime)
                .list();
        return JsonResult.ok(list);
    }

    @Operation(summary = "下载文件（返回文件流）")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Integer id) {
        File file = fileService.getById(id);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Path.of(fileService.getAbsolutePath(file));
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String encodedFileName = URLEncoder.encode(file.getFileName(), StandardCharsets.UTF_8)
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFileName)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @RequireRole(3)
    @Operation(summary = "删除文件记录（逻辑删除，磁盘文件保留）")
    @DeleteMapping("/{id}")
    public JsonResult<Void> delete(@PathVariable Integer id) {
        fileService.removeById(id);
        return JsonResult.ok();
    }
}
