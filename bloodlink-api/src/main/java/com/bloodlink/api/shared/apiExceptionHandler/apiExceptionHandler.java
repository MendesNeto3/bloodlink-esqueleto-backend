package com.bloodlink.api.shared.apiExceptionHandler;

import com.bloodlink.api.doador.adapter.in.web.dto.DoadorController;
import com.bloodlink.api.shared.Exception.ConflitoException;
import com.bloodlink.api.shared.Exception.RecursoNaoEncontradoException;
import com.bloodlink.api.shared.Exception.RegraDeNegocioException;
import com.bloodlink.api.shared.tenant.exception.FalhaInternaException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = { DoadorController.class })
public class apiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(apiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumentoInvalido(IllegalArgumentException e,
                                                                 HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tipoDeParametroInvalido(MethodArgumentTypeMismatchException e,
                                                                       HttpServletRequest request) {
        String mensagem = "Valor inválido para o parâmetro '" + e.getName() + "': " + e.getValue();
        return resposta(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> corpoIlegivel(HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado", request);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(RecursoNaoEncontradoException e,
                                                             HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<Map<String, Object>> conflito(ConflitoException e, HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> regraDeNegocio(RegraDeNegocioException e,
                                                              HttpServletRequest request) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), request);
    }

    @ExceptionHandler(FalhaInternaException.class)
    public ResponseEntity<Map<String, Object>> falhaInterna(FalhaInternaException e, HttpServletRequest request) {
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> erroInesperado(Exception e, HttpServletRequest request) {
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", request);
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String mensagem,
                                                         HttpServletRequest request) {
        return ResponseEntity.status(status).body(body(status, mensagem, request));
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