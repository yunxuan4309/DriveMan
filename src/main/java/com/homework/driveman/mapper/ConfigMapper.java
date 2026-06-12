package com.homework.driveman.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.homework.driveman.entity.Config;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/** 系统配置表 Mapper — 继承 MyBatis-Plus BaseMapper 提供 CRUD */
@Repository
public interface ConfigMapper extends BaseMapper<Config> {

    /**
     * 分页查询配置项，支持关键字模糊匹配 config_key / config_value / description
     */
    @Select("<script>" +
            "SELECT * FROM config " +
            "WHERE 1 = 1 " +
            "  <if test='keyword != null and keyword != \"\"'>" +
            "    AND (config_key LIKE CONCAT('%', #{keyword}, '%') " +
            "         OR config_value LIKE CONCAT('%', #{keyword}, '%') " +
            "         OR description LIKE CONCAT('%', #{keyword}, '%'))" +
            "  </if> " +
            "ORDER BY config_key" +
            "</script>")
    Page<Config> selectPageWithKeyword(Page<?> page, @Param("keyword") String keyword);
}
