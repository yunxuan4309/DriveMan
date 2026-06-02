package com.homework.driveman.vo;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class StudentInfoVO {
    private Integer studentId;
    private String realName;
    private String phone;
    private BigDecimal totalHours;   // 该学员总学时
}