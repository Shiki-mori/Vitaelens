package com.phrolova.vitaelensbackend.controller;

import com.phrolova.vitaelensbackend.common.Result;
import com.phrolova.vitaelensbackend.dto.request.LoginRequest;
import com.phrolova.vitaelensbackend.dto.request.RegisterRequest;
import com.phrolova.vitaelensbackend.dto.response.LoginResponse;
import com.phrolova.vitaelensbackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag( name="用户认证模块", description = "用户认证相关接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     * @param request  注册请求参数
     * @return  注册响应参数
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request){
        authService.register(request);
        return Result.success();
    }

    /**
     * 用户登录
     * @param request 登录请求参数
     * @return 登录响应参数
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request){
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }
}
