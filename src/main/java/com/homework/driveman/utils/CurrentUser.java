package com.homework.driveman.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 当前登录用户上下文 — 从 JWT Token 解析后存入，供 Controller/Service 获取
 */
@Data
@AllArgsConstructor
public class CurrentUser {

    private Integer userId;
    private String username;
    /** 1-学员, 2-教练, 3-管理员 */
    private Integer role;

    /** 是否拥有指定角色 */
    public boolean hasRole(int... roles) {
        for (int r : roles) {
            if (this.role == r) return true;
        }
        return false;
    }
}
