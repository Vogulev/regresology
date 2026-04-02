package com.vogulev.regreso.service.impl;

import com.vogulev.regreso.service.PasswordResetEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetEmailServiceImpl implements PasswordResetEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.password-reset.mail-from:${spring.mail.username:no-reply@regreso.app}}")
    private String mailFrom;

    @Value("${app.password-reset.code-ttl-minutes:15}")
    private long codeTtlMinutes;

    @Override
    public void sendResetCode(String email, String firstName, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("Восстановление пароля Regreso");
        message.setText("""
                Здравствуйте, %s!
                                
                Код для восстановления пароля: %s
                                
                Код действует %d минут.
                Если вы не запрашивали восстановление пароля, просто проигнорируйте это письмо.
                """.formatted(firstName, code, codeTtlMinutes));
        mailSender.send(message);
    }
}
