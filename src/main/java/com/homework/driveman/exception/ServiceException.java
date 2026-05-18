package com.homework.driveman.exception;

import com.homework.driveman.web.ServiceCode;

/**
 * 业务异常 — 在 Service 层抛出，由 GlobalExceptionHandler 统一捕获处理
 * 携带 ServiceCode 状态码，前端可根据 state 值做相应处理
 */
public class ServiceException extends RuntimeException {

    private ServiceCode serviceCode;

    public ServiceException(ServiceCode serviceCode, String message) {
        super(message);
        this.serviceCode = serviceCode;
    }

    public ServiceCode getServiceCode() {
        return serviceCode;
    }

}
