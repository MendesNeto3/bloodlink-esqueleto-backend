package com.bloodlink.api.shared.tenant.adapter.in.web;

import com.bloodlink.api.shared.tenant.exception.ProvisionamentoNaoEncontradoException;
import com.bloodlink.api.shared.tenant.exception.RetomadaProvisionamentoException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = TenantProvisionamentoController.class)
public class TenantExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> schemaInvalido(IllegalArgumentException e,
                                                              HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(body(HttpStatus.BAD_REQUEST, e.getMessage(), request));
    }

    @ExceptionHandler(ProvisionamentoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(ProvisionamentoNaoEncontradoException e,
                                                             HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(body(HttpStatus.NOT_FOUND, e.getMessage(), request));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> estadoInvalido(IllegalStateException e,
                                                              HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(body(HttpStatus.CONFLICT, e.getMessage(), request));
    }

    @ExceptionHandler(RetomadaProvisionamentoException.class)
    public ResponseEntity<Map<String, Object>> falhaAoRetomar(RetomadaProvisionamentoException e,
                                                              HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), request));
    }

    private Map<String, Object> body(HttpStatus status, String message, HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", status.value());
        response.put("error", message);
        response.put("body", request.getRequestURI());
        return response;
    }
}