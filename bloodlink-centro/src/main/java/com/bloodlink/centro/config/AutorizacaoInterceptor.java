package com.bloodlink.centro.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.method.HandlerMethod;

import java.util.Set;

@Component
public class AutorizacaoInterceptor implements HandlerInterceptor  {

    private static final String HEADER_PAPEL = "X-User-Role";
    private static final Logger log = LoggerFactory.getLogger(AutorizacaoInterceptor.class);


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        log.info("AutorizacaoInterceptor.preHandle CHAMADO para {} | handler class = {}",
                request.getRequestURI(), handler.getClass().getName());

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            log.warn("Handler NAO é HandlerMethod, liberando sem checagem: {}", handler.getClass().getName());
            return true;
        }

        ExigePapel anotacao = handlerMethod.getMethodAnnotation(ExigePapel.class);
        log.info("Anotacao ExigePapel encontrada: {}", anotacao);
        if (anotacao == null) {
            return true;
        }

        Set<String> papeisPermitidos = Set.of(anotacao.value());
        String papelRecebido = request.getHeader(HEADER_PAPEL);

        if (papelRecebido == null || !papeisPermitidos.contains(papelRecebido)) {
            log.warn("Acesso negado: rota {} exige {}, recebeu '{}'",
                    request.getRequestURI(), papeisPermitidos, papelRecebido);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Papel não autorizado para esta operação");
            return false;
        }

        return true;
    }
    }