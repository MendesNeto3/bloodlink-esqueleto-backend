package com.bloodlink.api.shared.apiExceptionHandler;

import com.bloodlink.api.shared.log.MascaraLog;
import com.bloodlink.api.shared.tenant.exception.FalhaInternaException;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(basePackages = {
        "com.bloodlink.api.doador",
        "com.bloodlink.api.reserva",
        "com.bloodlink.api.documento",
        "com.bloodlink.api.agendamento",
        "com.bloodlink.api.centro"
})
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumentoInvalido(IllegalArgumentException e,
                                                                 HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tipoDeParametroInvalido(MethodArgumentTypeMismatchException e,
                                                                       HttpServletRequest request) {
        String valor = MascaraLog.texto(String.valueOf(e.getValue()));
        String mensagem = "Valor inválido para o parâmetro '" + e.getName() + "': " + valor;
        return resposta(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> corpoIlegivel(HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou malformado", request);
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Map<String, Object>> entradaAusente(HttpServletRequest request) {
        return resposta(HttpStatus.BAD_REQUEST, "Requisição incompleta: falta um parâmetro obrigatório", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> metodoNaoSuportado(HttpServletRequest request) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP não suportado para esta rota", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> rotaInexistente(HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, "Recurso não encontrado", request);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(RecursoNaoEncontradoException e,
                                                             HttpServletRequest request) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<Map<String, Object>> conflito(ConflitoException e,
                                                        HttpServletRequest request) {
        return resposta(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> regraDeNegocio(RegraDeNegocioException e,
                                                              HttpServletRequest request) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), request);
    }

    @ExceptionHandler(FalhaInternaException.class)
    public ResponseEntity<Map<String, Object>> falhaInterna(FalhaInternaException e,
                                                            HttpServletRequest request) {
        log.error("Falha interna em {} {} - causa: {}", request.getMethod(),
                MascaraLog.uri(request.getRequestURI()), MascaraLog.causa(e.getCause()));
        log.debug("Detalhe da falha interna", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> erroInesperado(Exception e,
                                                              HttpServletRequest request) {
        log.error("Erro inesperado em {} {} - causa: {}", request.getMethod(),
                MascaraLog.uri(request.getRequestURI()), MascaraLog.causa(e));
        log.debug("Detalhe do erro inesperado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", request);
    }

    private ResponseEntity<Map<String, Object>> resposta(HttpStatus status, String mensagem,
                                                         HttpServletRequest request) {
        return ResponseEntity.status(status).body(corpo(status, mensagem, request));
    }

    private Map<String, Object> corpo(HttpStatus status, String mensagem, HttpServletRequest request) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", Instant.now().toString());
        response.put("status", status.value());
        response.put("error", mensagem);
        response.put("path", MascaraLog.uri(request.getRequestURI()));
        return response;
    }
}