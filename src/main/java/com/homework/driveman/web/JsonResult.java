package com.homework.driveman.web;

import com.homework.driveman.exception.ServiceException;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 统一响应结果封装类 — 所有 Controller 接口都返回此格式
 * @param <T>  data 字段的类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class JsonResult<T> implements Serializable {

    /** 业务状态码（参照 ServiceCode 枚举） */
    private Integer state;
    /** 提示信息（仅失败时携带） */
    private String message;
    /** 响应数据（仅成功时携带） */
    private T data;

    /** 请求成功，携带数据 */
    public static <T> JsonResult<T> ok(T data) {
        JsonResult<T> jsonResult = new JsonResult<>();
        jsonResult.state = ServiceCode.OK.getValue();
        jsonResult.data = data;
        return jsonResult;
    }

    /** 请求成功，无数据返回 */
    public static JsonResult<Void> ok() {
        return ok(null);
    }

    /** 请求失败，从 ServiceException 中提取状态码和消息 */
    public static <T> JsonResult<T> fail(ServiceException e) {
        return fail(e.getServiceCode(), e.getMessage());
    }

    /** 请求失败，指定状态码和消息 */
    public static <T> JsonResult<T> fail(ServiceCode serviceCode, String message) {
        JsonResult<T> jsonResult = new JsonResult<>();
        jsonResult.state = serviceCode.getValue();
        jsonResult.message = message;
        return jsonResult;
    }

}
