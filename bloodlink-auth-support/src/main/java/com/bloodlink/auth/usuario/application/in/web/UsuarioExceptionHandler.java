package com.bloodlink.auth.usuario.application.in.web;

import com.bloodlink.auth.shared.exceptions.*;
import com.bloodlink.common.log.MascaraLog;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = { AuthController.class, UsuarioController.class })
public class UsuarioExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(UsuarioExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> requisicaoInvalida(IllegalArgumentException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> parametroInvalido(MethodArgumentTypeMismatchException e,
                                                                 HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + e.getName() + "'", req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> corpoInvalido(HttpMessageNotReadableException e, HttpServletRequest req) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou malformado", req);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<Map<String, Object>> credenciaisInvalidas(CredenciaisInvalidasException e,
                                                                    HttpServletRequest req) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage(), req);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<Map<String, Object>> acessoNegado(AcessoNegadoException e, HttpServletRequest req) {
        return resposta(HttpStatus.FORBIDDEN, e.getMessage(), req);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest req) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), req);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<Map<String, Object>> conflito(ConflitoException e, HttpServletRequest req) {
        return resposta(HttpStatus.CONFLICT, e.getMessage(), req);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> regraDeNegocio(RegraDeNegocioException e, HttpServletRequest req) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), req);
    }

    @ExceptionHandler(FalhaInternaException.class)
    public ResponseEntity<Map<String, Object>> falhaInterna(FalhaInternaException e, HttpServletRequest req) {
        // sem log aqui: o service já registrou a causa uma única vez
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception e, HttpServletRequest req) {
        log.error("Erro inesperado em {} {}: {}", req.getMethod(), MascaraLog.uri(req.getRequestURI()),
                MascaraLog.causa(e));
        log.debug("Detalhe do erro inesperado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", req);
    }

    private static ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String mensagem,
                                                                HttpServletRequest req) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", Instant.now().toString());
        corpo.put("status", status.value());
        corpo.put("error", mensagem);
        corpo.put("path", MascaraLog.uri(req.getRequestURI()));
        return ResponseEntity.status(status).body(corpo);
    }
}