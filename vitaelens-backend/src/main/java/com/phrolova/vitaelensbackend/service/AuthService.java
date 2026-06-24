package com.phrolova.vitaelensbackend.service;

import com.phrolova.vitaelensbackend.dto.request.LoginRequest;
import com.phrolova.vitaelensbackend.dto.request.RegisterRequest;
import com.phrolova.vitaelensbackend.dto.response.LoginResponse;

public interface AuthService {
    void register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}
