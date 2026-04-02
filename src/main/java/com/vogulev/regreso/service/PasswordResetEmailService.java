package com.vogulev.regreso.service;

public interface PasswordResetEmailService {

    void sendResetCode(String email, String firstName, String code);
}
