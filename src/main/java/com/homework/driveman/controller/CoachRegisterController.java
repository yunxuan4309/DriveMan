package com.homework.driveman.controller;

import com.homework.driveman.dto.CoachRegisterDTO;
import com.homework.driveman.service.IUserService;
import com.homework.driveman.web.JsonResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "教练注册")
@RestController
@RequestMapping("/coach")
public class CoachRegisterController {

    @Autowired
    private IUserService userService;

    @Operation(summary = "教练注册", description = "提交注册信息，待管理员审核")
    @PostMapping("/register")
    public JsonResult<Void> register(@RequestBody @Valid CoachRegisterDTO dto) {
        userService.coachRegister(dto);
        return JsonResult.ok();
    }
}