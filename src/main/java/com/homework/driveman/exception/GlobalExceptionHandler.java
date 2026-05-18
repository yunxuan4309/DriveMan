package com.homework.driveman.exception;

import com.homework.driveman.web.JsonResult;
import com.homework.driveman.web.ServiceCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器 — 统一捕获 ServiceException 和未知异常
 * 返回统一格式的 JsonResult，避免将堆栈信息暴露给前端
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler
    public JsonResult<Void> handleServiceException(ServiceException e) {
        log.debug("业务异常: {}", e.getMessage());
        return JsonResult.fail(e);
    }

    @ExceptionHandler
    public JsonResult<Void> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return JsonResult.fail(ServiceCode.ERROR_UNKNOWN, "服务器内部错误，请稍后重试");
    }

}
