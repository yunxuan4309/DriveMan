package com.homework.driveman.config;

import java.lang.annotation.*;

/**
 * 角色权限注解 — 标注在 Controller 方法上，指定允许访问的角色
 * 示例: @RequireRole(3) 仅管理员 | @RequireRole({1,3}) 学员和管理员
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    /** 允许访问的角色值: 1-学员, 2-教练, 3-管理员 */
    int[] value();
}
