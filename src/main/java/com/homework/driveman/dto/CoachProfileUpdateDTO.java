package com.homework.driveman.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "教练个人资料更新请求")
public class CoachProfileUpdateDTO {

    @Size(min = 2, max = 20, message = "真实姓名长度2-20位")
    @Schema(description = "真实姓名", example = "张教练")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13812345678")
    private String phone;

    @Schema(description = "地址", example = "重庆市南岸区")
    private String address;

    @Schema(description = "头像URL", example = "/uploads/avatar/xxx.jpg")
    private String avatar;

    @Schema(description = "执教年限", example = "5")
    private Integer coachYears;
}