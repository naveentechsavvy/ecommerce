package com.apparel.commerce.common;

import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** RFC 7807 problem responses with a stable machine-readable {@code code} property. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApi(ApiException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        pd.setProperty("code", ex.getCode());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setProperty("code", "VALIDATION_FAILED");
        return pd;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "The change conflicts with existing data (duplicate value or record still in use).");
        pd.setProperty("code", "DATA_CONFLICT");
        return pd;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
            "The record was modified by someone else. Reload and try again.");
        pd.setProperty("code", "CONCURRENT_MODIFICATION");
        return pd;
    }
}
