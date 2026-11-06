package com.usedcar.web;

import com.usedcar.dto.ApiErrorResponse;
import com.usedcar.service.DuplicateChassisNumberException;
import com.usedcar.service.NotFoundException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Bean Validation 違反 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        // クラスレベルの制約 (@AssertTrue) は get("target") がオブジェクト名になる
        ex.getBindingResult().getGlobalErrors()
            .forEach(error -> fieldErrors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));

        String message = fieldErrors.isEmpty()
            ? "入力内容を確認してください"
            : "入力内容に不正な項目があります";
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ApiErrorResponse.validation(422, "Validation Failed", message, fieldErrors));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
        InvalidParameterException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex) {
        log.warn("不正なリクエスト: {}", ex.getMessage());
        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of(400, "Bad Request", "リクエストの内容が不正です"));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateChassisNumberException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateChassisNumberException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put("chassisNumber", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse.validation(409, "Conflict", ex.getMessage(), fieldErrors));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("データ整合性エラー: {}", ex.getMostSpecificCause().getMessage());
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put("chassisNumber", "既に登録されているデータと重複しています");
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiErrorResponse.validation(409, "Conflict", "データ整合性エラー", fieldErrors));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleJpaNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiErrorResponse.of(404, "Not Found", "指定の在庫データが存在しません"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("予期しないエラー", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiErrorResponse.of(500, "Internal Server Error",
                "サーバー内でエラーが発生しました。時間をおいて再度お試しください。"));
    }
}
