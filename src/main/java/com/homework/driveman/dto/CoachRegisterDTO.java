package com.homework.driveman.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "教练注册请求")
public class CoachRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度为4-20位")
    @Schema(description = "登录用户名", example = "coach_zhang")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为6-20位")
    @Schema(description = "密码", example = "123456")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Schema(description = "真实姓名", example = "张教练")
    private String realName;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$",
            message = "身份证号格式不正确")
    @Schema(description = "身份证号", example = "510101199505012345")
    private String idCard;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号", example = "13812340001")
    private String phone;

    @NotBlank(message = "准教车型不能为空")
    @Schema(description = "准教车型，逗号分隔", example = "C1,C2")
    private String vehicleType;

    @Schema(description = "执教年限", example = "5")
    private Integer coachYears;

    @Schema(description = "资质证书照片URL（可选）", example = "/uploads/cert/xxx.jpg")
    private String certificateUrl;

    @Schema(description = "头像URL（可选）", example = "/uploads/avatar/xxx.jpg")
    private String avatar;
}