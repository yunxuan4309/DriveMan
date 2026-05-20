package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.File;
import org.springframework.web.multipart.MultipartFile;

/** 文件业务接口 — 上传/下载/删除 */
public interface IFileService extends IService<File> {

    /**
     * 上传文件到本地磁盘
     * @param userId  上传者 ID
     * @param file     待上传的文件
     * @param fileType 文件分类（id_card_front / physical_exam / registration_pdf / admission_ticket）
     * @return 文件实体（已持久化）
     */
    File upload(Integer userId, MultipartFile file, String fileType);

    /**
     * 获取文件的完整磁盘路径
     */
    String getAbsolutePath(File file);

    /**
     * 获取文件的 URL 访问路径
     */
    String getUrlPath(File file);
}
