package com.homework.driveman.web;

//Author:[谢云轩]
//QQ:[1721476339]
//ID:[632307060623]
//Date:2025/11/19
//Time:9:54
public enum ServiceCode2
{
    OK(2000,"23323"),
    ERROR_INSERT(60100,"2322323223"),
    INSERT(60000,"333333");
    private String message;
    private Integer value;

    ServiceCode2( Integer value,String message) {
        this.message = message;
        this.value = value;
    }

    public String getMessage() {
        return message;
    }

    public Integer getValue() {
        return value;
    }
}
