package com.usedcar.dto;

/** API エラーレスポンス */
public record ApiErrorResponse(
    int status,
    String error,
    String message,
    java.util.Map<String, String> fieldErrors,
    java.time.LocalDateTime timestamp
) {

    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(status, error, message, null, java.time.LocalDateTime.now());
    }

    public static ApiErrorResponse validation(int status, String error, String message,
                                              java.util.Map<String, String> fieldErrors) {
        return new ApiErrorResponse(status, error, message, fieldErrors, java.time.LocalDateTime.now());
    }
}
