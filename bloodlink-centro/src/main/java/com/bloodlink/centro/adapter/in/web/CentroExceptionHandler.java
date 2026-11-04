package com.bloodlink.centro.adapter.in.web;

import com.bloodlink.centro.application.service.CentroNaoEncontradoException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = CentroController.class)
public class CentroExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CentroExceptionHandler.class);

    @ExceptionHandler(CentroNaoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleCentroNaoEncontrado(
            CentroNaoEncontradoException ex, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // regras de validação do próprio domínio (nome, perfil, coordenadas); as mensagens são nossas
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleArgumentoInvalido(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // @Valid no corpo (por exemplo, nome em branco)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacao(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String campos = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .collect(Collectors.joining(", "));
        String mensagem = campos.isBlank() ? "Dados da requisição inválidos" : "Campo(s) inválido(s): " + campos;
        return buildError(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    // JSON malformado, enum inexistente (tipo, dia da semana) ou hora fora do formato HH:mm
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleCorpoIlegivel(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido ou mal formatado", request);
    }

    // id que não é UUID, lat/lng que não é número
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTipoParametro(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Parâmetro '" + ex.getName() + "' inválido", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleParametroAusente(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Parâmetro '" + ex.getParameterName() + "' é obrigatório", request);
    }

    // rede de segurança: só a classe da falha vai para o log, nunca a mensagem (pode ter dado pessoal)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleInesperado(RuntimeException ex, HttpServletRequest request) {
        log.error("Falha inesperada no módulo Centro: {}", ex.getClass().getSimpleName());
        log.debug("Detalhe da falha", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível concluir a operação no momento", request);
    }

    // mesmo formato dos outros módulos: timestamp, status, error (já com a mensagem) e path
    private ResponseEntity<Map<String, Object>> buildError(HttpStatus status, String mensagem, HttpServletRequest request) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", Instant.now().toString());
        corpo.put("status", status.value());
        corpo.put("error", mensagem);
        corpo.put("path", request.getRequestURI());
        return ResponseEntity.status(status).body(corpo);
    }
}