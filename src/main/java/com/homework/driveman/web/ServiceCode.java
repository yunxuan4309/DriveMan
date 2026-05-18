package com.homework.driveman.web;

/**
 * 业务状态码枚举
 * 以 2 开头的表示成功，以 4 开头的表示客户端错误，以 5/6 开头的表示服务端错误
 */
public enum ServiceCode {

    /** 成功 */
    OK(20000),
    /** 请求参数格式有误 */
    ERROR_BAD_REQUEST(40000),
    /** 数据不存在 */
    ERROR_NOT_FOUND(40400),
    /** 数据冲突 */
    ERROR_CONFLICT(40900),
    /** 未通过认证，或未找到认证信息 */
    ERROR_UNAUTHORIZED(40100),
    /** 账号被禁用 */
    ERROR_UNAUTHORIZED_DISABLED(40101),
    /** 禁止访问，无此操作权限 */
    ERROR_FORBIDDEN(40300),
    /** 插入数据错误 */
    ERROR_INSERT(50000),
    /** 删除数据错误 */
    ERROR_DELETE(50100),
    /** 修改数据错误 */
    ERROR_UPDATE(50200),
    /** JWT 已过期 */
    ERROR_JWT_EXPIRED(60000),
    /** JWT 格式错误 */
    ERROR_JWT_MALFORMED(60100),
    /** JWT 签名验证失败 */
    ERROR_JWT_SIGNATURE(60200),
    /** 未知错误 */
    ERROR_UNKNOWN(99999);

    private Integer value;

    ServiceCode(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

}
