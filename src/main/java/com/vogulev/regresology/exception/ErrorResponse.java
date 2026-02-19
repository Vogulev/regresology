package com.vogulev.regresology.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class ErrorResponse {

    private final String message;
    private final Map<String, String> fieldErrors;

    public ErrorResponse(String message) {
        this.message = message;
        this.fieldErrors = null;
    }

    public ErrorResponse(String message, Map<String, String> fieldErrors) {
        this.message = message;
        this.fieldErrors = fieldErrors;
    }
}
