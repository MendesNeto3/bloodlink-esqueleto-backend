package com.bloodlink.centro.adapter.in.web;

import com.bloodlink.centro.application.service.CentroNaoEncontradoException;
import com.bloodlink.centro.config.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = CentroController.class)
public class CentroExceptionHandler {

    @ExceptionHandler(CentroNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleCentroNaoEncontrado(
            CentroNaoEncontradoException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex, request);
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, Exception ex, HttpServletRequest request) {
        ErrorResponse body = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
