package com.vogulev.regreso.service.impl;

import com.vogulev.regreso.dto.request.LoginRequest;
import com.vogulev.regreso.dto.request.RegisterRequest;
import com.vogulev.regreso.dto.response.AuthResponse;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.exception.BusinessException;
import com.vogulev.regreso.exception.ResourceNotFoundException;
import com.vogulev.regreso.repository.PractitionerRepository;
import com.vogulev.regreso.security.JwtService;
import com.vogulev.regreso.security.PractitionerDetails;
import com.vogulev.regreso.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final PractitionerRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email уже зарегистрирован");
        }

        Practitioner practitioner = Practitioner.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .build();

        repository.save(practitioner);

        return generateTokens(practitioner.getEmail());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        return generateTokens(request.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        String email = jwtService.extractEmail(refreshToken);

        Practitioner practitioner = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        PractitionerDetails userDetails = new PractitionerDetails(practitioner);

        if (!jwtService.isRefreshToken(refreshToken) || !jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new BusinessException("Невалидный refresh token");
        }

        return generateTokens(email);
    }

    private AuthResponse generateTokens(String email) {
        return AuthResponse.builder()
                .accessToken(jwtService.generateAccessToken(email))
                .refreshToken(jwtService.generateRefreshToken(email))
                .build();
    }
}
