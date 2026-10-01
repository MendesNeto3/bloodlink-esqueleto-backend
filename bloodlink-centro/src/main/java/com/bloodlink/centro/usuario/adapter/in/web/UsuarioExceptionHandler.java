package com.bloodlink.centro.usuario.adapter.in.web;

import com.bloodlink.centro.config.ErrorResponse;
import com.bloodlink.centro.usuario.application.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UsuarioExceptionHandler {

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleUsuarioNaoEncontradoException(
            UsuarioNaoEncontradoException ex, HttpStatus status, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex, request);
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErrorResponse> handleEmailJaCadastrado(
            EmailJaCadastradoException ex, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, ex, request);
    }

    @ExceptionHandler(CadastroIndisponivelException.class)
    public ResponseEntity<ErrorResponse> handleCadastroIndisponivel(
            CadastroIndisponivelException ex, HttpServletRequest request) {
        return buildError(HttpStatus.SERVICE_UNAVAILABLE, ex, request);
    }

    @ExceptionHandler(AutenticacaoIndisponivelException.class)
    public ResponseEntity<ErrorResponse> handleAutenticacaoIndisponivel(
            AutenticacaoIndisponivelException ex, HttpServletRequest request) {
        return buildError(HttpStatus.SERVICE_UNAVAILABLE, ex, request);
    }

    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    public ResponseEntity<ErrorResponse> handlerOperacaoNaoPermitida( HttpServletRequest request, OperacaoNaoPermitidaException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex, request);
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
