package com.vogulev.regreso.service.impl;

import com.vogulev.regreso.dto.request.ForgotPasswordRequest;
import com.vogulev.regreso.dto.request.LoginRequest;
import com.vogulev.regreso.dto.request.RegisterRequest;
import com.vogulev.regreso.dto.request.ResetPasswordRequest;
import com.vogulev.regreso.dto.response.AuthResponse;
import com.vogulev.regreso.dto.response.MessageResponse;
import com.vogulev.regreso.entity.PasswordResetCode;
import com.vogulev.regreso.entity.Practitioner;
import com.vogulev.regreso.exception.BusinessException;
import com.vogulev.regreso.exception.ResourceNotFoundException;
import com.vogulev.regreso.repository.PasswordResetCodeRepository;
import com.vogulev.regreso.repository.PractitionerRepository;
import com.vogulev.regreso.security.JwtService;
import com.vogulev.regreso.security.PractitionerDetails;
import com.vogulev.regreso.service.AuthService;
import com.vogulev.regreso.service.PasswordResetEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final String PASSWORD_RESET_REQUEST_MESSAGE =
            "Если аккаунт с таким email существует, мы отправили код для восстановления пароля";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PractitionerRepository repository;
    private final PasswordResetCodeRepository passwordResetCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetEmailService passwordResetEmailService;

    @Value("${app.password-reset.code-ttl-minutes:15}")
    private long passwordResetCodeTtlMinutes = 15;

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
    public MessageResponse requestPasswordReset(ForgotPasswordRequest request) {
        repository.findByEmail(request.getEmail())
                .ifPresent(this::createAndSendPasswordResetCode);

        return new MessageResponse(PASSWORD_RESET_REQUEST_MESSAGE);
    }

    @Override
    public MessageResponse confirmPasswordReset(ResetPasswordRequest request) {
        Practitioner practitioner = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Неверный код восстановления"));

        PasswordResetCode resetCode = passwordResetCodeRepository.findByPractitioner(practitioner)
                .orElseThrow(() -> new BusinessException("Неверный код восстановления"));

        OffsetDateTime now = OffsetDateTime.now();
        if (resetCode.isConsumed() || resetCode.isExpired(now)) {
            throw new BusinessException("Код восстановления недействителен или истёк");
        }

        if (!passwordEncoder.matches(request.getCode(), resetCode.getCodeHash())) {
            throw new BusinessException("Неверный код восстановления");
        }

        practitioner.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        resetCode.setConsumedAt(now);

        repository.save(practitioner);
        passwordResetCodeRepository.save(resetCode);

        return new MessageResponse("Пароль успешно обновлён");
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

    private void createAndSendPasswordResetCode(Practitioner practitioner) {
        String code = generateResetCode();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(passwordResetCodeTtlMinutes);

        PasswordResetCode resetCode = passwordResetCodeRepository.findByPractitioner(practitioner)
                .orElseGet(() -> PasswordResetCode.builder().practitioner(practitioner).build());

        resetCode.setCodeHash(passwordEncoder.encode(code));
        resetCode.setExpiresAt(expiresAt);
        resetCode.setConsumedAt(null);
        passwordResetCodeRepository.save(resetCode);

        passwordResetEmailService.sendResetCode(practitioner.getEmail(), practitioner.getFirstName(), code);
    }

    private String generateResetCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}
