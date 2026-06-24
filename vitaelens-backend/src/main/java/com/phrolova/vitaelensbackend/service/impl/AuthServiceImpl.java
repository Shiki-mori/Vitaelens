package com.phrolova.vitaelensbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.phrolova.vitaelensbackend.auth.JwtUtil;
import com.phrolova.vitaelensbackend.common.ErrorCode;
import com.phrolova.vitaelensbackend.dto.request.LoginRequest;
import com.phrolova.vitaelensbackend.dto.request.RegisterRequest;
import com.phrolova.vitaelensbackend.dto.response.LoginResponse;
import com.phrolova.vitaelensbackend.entity.User;
import com.phrolova.vitaelensbackend.exception.BizException;
import com.phrolova.vitaelensbackend.mapper.UserMapper;
import com.phrolova.vitaelensbackend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());

        if (userMapper.selectOne(wrapper) != null){
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());

        userMapper.insert(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, request.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        return response;
    }
}
