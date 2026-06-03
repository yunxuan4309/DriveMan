package com.homework.driveman.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.homework.driveman.entity.File;
import org.springframework.web.multipart.MultipartFile;

/** 文件业务接口 — 上传/下载/查询/删除 */
public interface IFileService extends IService<File> {

    /**
     * 上传文件到本地磁盘（建议使用）
     * @param userId   文件归属人 ID
     * @param file     待上传的文件
     * @param fileType 文件分类（旧字段，向前兼容）
     * @param bizType  业务类型
     * @param bizId    业务记录 ID（可选）
     * @return 文件实体（已持久化）
     */
    File upload(Integer userId, MultipartFile file, String fileType, String bizType, Integer bizId);

    /**
     * 上传文件（旧签名，向后兼容）
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

    /**
     * 保存服务端生成文件的记录（用于PDF等自动生成的文件）
     * @param userId   文件归属人 ID
     * @param filePath 文件相对 uploadPath 的存储路径
     * @param fileName 原始文件名（展示用）
     * @param fileType 文件分类
     * @param bizType  业务类型
     * @param bizId    业务记录 ID（可选）
     * @return 文件实体（已持久化）
     */
    File saveRecord(Integer userId, String filePath, String fileName, String fileType, String bizType, Integer bizId);

    /**
     * 保存记录（旧签名，向后兼容）
     */
    File saveRecord(Integer userId, String filePath, String fileName, String fileType);

    /**
     * 校验当前用户是否有权访问该文件
     * @param file      目标文件
     * @param userId    当前登录用户 ID
     * @param userRole  当前登录用户角色
     * @return true=有权限
     */
    boolean hasAccess(File file, Integer userId, Integer userRole);
}
