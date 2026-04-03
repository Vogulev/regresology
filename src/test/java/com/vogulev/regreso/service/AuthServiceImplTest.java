package com.vogulev.regreso.service;

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
import com.vogulev.regreso.service.PasswordResetEmailService;
import com.vogulev.regreso.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock PractitionerRepository repository;
    @Mock PasswordResetCodeRepository passwordResetCodeRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock PasswordResetEmailService passwordResetEmailService;

    @InjectMocks
    AuthServiceImpl authService;

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register — новый email: сохраняет практика и возвращает токены")
    void register_withNewEmail_shouldSaveAndReturnTokens() {
        when(repository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtService.generateAccessToken("new@example.com")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("new@example.com")).thenReturn("refresh-token");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.register(registerRequest("new@example.com", "password123", "Иван"));

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        ArgumentCaptor<Practitioner> captor = ArgumentCaptor.forClass(Practitioner.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getFirstName()).isEqualTo("Иван");
    }

    @Test
    @DisplayName("register — email уже занят: выбрасывает BusinessException")
    void register_withExistingEmail_shouldThrowBusinessException() {
        when(repository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                registerRequest("existing@example.com", "password123", "Иван")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email");

        verify(repository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login — верные данные: возвращает токены")
    void login_withValidCredentials_shouldReturnTokens() {
        when(jwtService.generateAccessToken("user@example.com")).thenReturn("access-token");
        when(jwtService.generateRefreshToken("user@example.com")).thenReturn("refresh-token");

        AuthResponse response = authService.login(loginRequest("user@example.com", "pass"));

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("user@example.com", "pass"));
    }

    @Test
    @DisplayName("login — неверный пароль: исключение AuthenticationManager пробрасывается выше")
    void login_withBadCredentials_shouldPropagateException() {
        doThrow(new BadCredentialsException("bad"))
                .when(authenticationManager).authenticate(any());

        assertThatThrownBy(() -> authService.login(loginRequest("user@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── password reset ───────────────────────────────────────────────────────

    @Test
    @DisplayName("requestPasswordReset — существующий email: сохраняет код и отправляет письмо")
    void requestPasswordReset_withExistingEmail_shouldSaveCodeAndSendEmail() {
        Practitioner practitioner = practitioner("user@example.com");
        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(practitioner));
        when(passwordResetCodeRepository.findByPractitioner(practitioner)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "encoded:" + inv.getArgument(0));

        MessageResponse response = authService.requestPasswordReset(forgotPasswordRequest("user@example.com"));

        assertThat(response.getMessage()).contains("мы отправили код");
        verify(passwordResetCodeRepository).save(any(PasswordResetCode.class));
        verify(passwordResetEmailService).sendResetCode(eq("user@example.com"), eq("Test"), anyString());
    }

    @Test
    @DisplayName("requestPasswordReset — неизвестный email: возвращает тот же ответ и не отправляет письмо")
    void requestPasswordReset_withUnknownEmail_shouldNotSendEmail() {
        when(repository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        MessageResponse response = authService.requestPasswordReset(forgotPasswordRequest("ghost@example.com"));

        assertThat(response.getMessage()).contains("мы отправили код");
        verify(passwordResetCodeRepository, never()).save(any());
        verify(passwordResetEmailService, never()).sendResetCode(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("confirmPasswordReset — корректный код: обновляет пароль и помечает код использованным")
    void confirmPasswordReset_withValidCode_shouldUpdatePassword() {
        Practitioner practitioner = practitioner("user@example.com");
        PasswordResetCode resetCode = PasswordResetCode.builder()
                .practitioner(practitioner)
                .codeHash("encoded:123456")
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .build();

        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(practitioner));
        when(passwordResetCodeRepository.findByPractitioner(practitioner)).thenReturn(Optional.of(resetCode));
        when(passwordEncoder.matches("123456", "encoded:123456")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("encoded:new-password");

        MessageResponse response = authService.confirmPasswordReset(
                resetPasswordRequest("user@example.com", "123456", "new-password"));

        assertThat(response.getMessage()).contains("успешно");
        assertThat(practitioner.getPasswordHash()).isEqualTo("encoded:new-password");
        assertThat(resetCode.getConsumedAt()).isNotNull();
        verify(repository).save(practitioner);
        verify(passwordResetCodeRepository).save(resetCode);
    }

    @Test
    @DisplayName("confirmPasswordReset — истёкший код: BusinessException")
    void confirmPasswordReset_withExpiredCode_shouldThrowBusinessException() {
        Practitioner practitioner = practitioner("user@example.com");
        PasswordResetCode resetCode = PasswordResetCode.builder()
                .practitioner(practitioner)
                .codeHash("encoded:123456")
                .expiresAt(OffsetDateTime.now().minusMinutes(1))
                .build();

        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(practitioner));
        when(passwordResetCodeRepository.findByPractitioner(practitioner)).thenReturn(Optional.of(resetCode));

        assertThatThrownBy(() -> authService.confirmPasswordReset(
                resetPasswordRequest("user@example.com", "123456", "new-password")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("истёк");
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("refresh — валидный refresh-токен: возвращает новую пару токенов")
    void refresh_withValidRefreshToken_shouldReturnNewTokens() {
        Practitioner practitioner = practitioner("user@example.com");
        when(jwtService.extractEmail("refresh-token")).thenReturn("user@example.com");
        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(practitioner));
        when(jwtService.isRefreshToken("refresh-token")).thenReturn(true);
        when(jwtService.isTokenValid(eq("refresh-token"), any(PractitionerDetails.class))).thenReturn(true);
        when(jwtService.generateAccessToken("user@example.com")).thenReturn("new-access");
        when(jwtService.generateRefreshToken("user@example.com")).thenReturn("new-refresh");

        AuthResponse response = authService.refresh("refresh-token");

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
    }

    @Test
    @DisplayName("refresh — передан access-токен вместо refresh: BusinessException")
    void refresh_withAccessToken_shouldThrowBusinessException() {
        Practitioner practitioner = practitioner("user@example.com");
        when(jwtService.extractEmail("access-token")).thenReturn("user@example.com");
        when(repository.findByEmail("user@example.com")).thenReturn(Optional.of(practitioner));
        when(jwtService.isRefreshToken("access-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("access-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("refresh token");
    }

    @Test
    @DisplayName("refresh — пользователь не найден в БД: ResourceNotFoundException")
    void refresh_withUnknownEmail_shouldThrowResourceNotFoundException() {
        when(jwtService.extractEmail(anyString())).thenReturn("ghost@example.com");
        when(repository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("some-token"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private RegisterRequest registerRequest(String email, String password, String firstName) {
        RegisterRequest r = new RegisterRequest();
        r.setEmail(email);
        r.setPassword(password);
        r.setFirstName(firstName);
        return r;
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    private ForgotPasswordRequest forgotPasswordRequest(String email) {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(email);
        return request;
    }

    private ResetPasswordRequest resetPasswordRequest(String email, String code, String newPassword) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail(email);
        request.setCode(code);
        request.setNewPassword(newPassword);
        return request;
    }

    private Practitioner practitioner(String email) {
        return Practitioner.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash("hashed")
                .firstName("Test")
                .build();
    }
}
