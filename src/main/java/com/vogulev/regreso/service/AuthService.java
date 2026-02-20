package com.vogulev.regreso.service;

import com.vogulev.regreso.dto.request.LoginRequest;
import com.vogulev.regreso.dto.request.RegisterRequest;
import com.vogulev.regreso.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(String refreshToken);
}
