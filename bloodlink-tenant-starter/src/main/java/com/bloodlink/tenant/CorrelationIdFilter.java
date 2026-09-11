package com.bloodlink.tenant;

import com.bloodlink.common.correlation.CorrelationContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.regex.Pattern;

public class CorrelationIdFilter implements Filter {

    private static final Logger log =  LoggerFactory.getLogger(CorrelationIdFilter.class.getName());
    private static final String MDC_KEY = "correlationId";

    private static final Pattern UUID_VALIDO = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String recebido = ((HttpServletRequest)request).getHeader(CorrelationContext.HEADER);
            String correlationId = sanitizar(recebido);

            CorrelationContext.set(correlationId);
            MDC.put(MDC_KEY, correlationId);

            ((HttpServletResponse) response).setHeader(CorrelationContext.HEADER, correlationId);

            chain.doFilter(request, response);

        } finally {
            MDC.remove(MDC_KEY);
            CorrelationContext.clear();
        }
    }

    private String sanitizar(String recebido) {
        if (recebido != null && UUID_VALIDO.matcher(recebido).matches()) {
            return recebido;
        }

        if (recebido != null) {
            log.debug("X-Correlation-Id recebido fora do formato UUID, gerando um novo");
        }
        return CorrelationContext.gerar();
    }
}
