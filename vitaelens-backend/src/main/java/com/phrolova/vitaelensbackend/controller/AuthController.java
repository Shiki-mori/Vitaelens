package com.phrolova.vitaelensbackend.controller;

import com.phrolova.vitaelensbackend.common.Result;
import com.phrolova.vitaelensbackend.dto.request.LoginRequest;
import com.phrolova.vitaelensbackend.dto.request.RegisterRequest;
import com.phrolova.vitaelensbackend.dto.response.LoginResponse;
import com.phrolova.vitaelensbackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request){
        authService.register(request);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request){
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }
}
