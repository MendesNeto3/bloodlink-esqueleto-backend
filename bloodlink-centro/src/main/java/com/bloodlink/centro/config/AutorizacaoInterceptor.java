package com.bloodlink.centro.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.HandlerMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class AutorizacaoInterceptor implements HandlerInterceptor  {

    private static final String HEADER_PAPEL = "X-User-Role";
    private static final Logger log = LoggerFactory.getLogger(AutorizacaoInterceptor.class);


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        ExigePapel anotacao = handlerMethod.getMethodAnnotation(ExigePapel.class);
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
