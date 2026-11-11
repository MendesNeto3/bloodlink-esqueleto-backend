package com.bloodlink.checkin.adapter.in.web.dto;

import com.bloodlink.checkin.application.exception.ConflitoException;
import com.bloodlink.checkin.application.exception.OperacaoCheckinException;
import com.bloodlink.checkin.application.exception.RecursoNaoEncontradoException;
import com.bloodlink.checkin.application.exception.RegraDeNegocioException;
import com.bloodlink.common.log.MascaraLog;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.bloodlink.checkin")
public class CheckinExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CheckinExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumentoInvalido(IllegalArgumentException e, HttpServletRequest r) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage(), r);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tipoDeParametroInvalido(MethodArgumentTypeMismatchException e,
                                                                       HttpServletRequest r) {
        return resposta(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + e.getName() + "'", r);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> corpoIlegivel(HttpServletRequest r) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado", r);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> entradaAusente(HttpServletRequest r) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição incompleta: falta um parâmetro obrigatório", r);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> metodoNaoSuportado(HttpServletRequest r) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não suportado para esta rota", r);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> rotaInexistente(HttpServletRequest r) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado", r);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest r) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), r);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<Map<String, Object>> conflito(ConflitoException e, HttpServletRequest r) {
        return resposta(HttpStatus.CONFLICT, e.getMessage(), r);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> regraDeNegocio(RegraDeNegocioException e, HttpServletRequest r) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), r);
    }

    @ExceptionHandler(OperacaoCheckinException.class)
    public ResponseEntity<Map<String, Object>> falhaDeOperacao(OperacaoCheckinException e, HttpServletRequest r) {
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), r);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> erroInesperado(Exception e, HttpServletRequest r) {
        log.error("Erro inesperado em {} {} - causa: {}", r.getMethod(), MascaraLog.uri(r.getRequestURI()),
                MascaraLog.causa(e));
        log.debug("Detalhe do erro inesperado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", r);
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String mensagem, HttpServletRequest r) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", Instant.now().toString());
        corpo.put("status", status.value());
        corpo.put("error", mensagem);
        corpo.put("path", MascaraLog.uri(r.getRequestURI()));
        return ResponseEntity.status(status).body(corpo);
    }
}