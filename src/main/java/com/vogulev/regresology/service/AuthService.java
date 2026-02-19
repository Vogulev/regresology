package com.vogulev.regresology.service;

import com.vogulev.regresology.dto.request.LoginRequest;
import com.vogulev.regresology.dto.request.RegisterRequest;
import com.vogulev.regresology.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshToken);
}
