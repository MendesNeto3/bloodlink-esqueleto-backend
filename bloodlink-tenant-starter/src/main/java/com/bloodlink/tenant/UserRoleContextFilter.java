package com.bloodlink.tenant;

import com.bloodlink.common.correlation.Papel;
import com.bloodlink.common.correlation.UserRoleContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class UserRoleContextFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(UserRoleContextFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String recebido = ((HttpServletRequest) request).getHeader(UserRoleContext.HEADER);
            UserRoleContext.set(sanitizar(recebido));
            chain.doFilter(request, response);
        } finally {
            UserRoleContext.clear();
        }
    }

    private String sanitizar(String recebido) {
        if (recebido == null) {
            return null; // rota pública, sem papel — legítimo
        }
        try {
            return Papel.valueOf(recebido).name();
        } catch (IllegalArgumentException e) {
            log.warn("X-User-Role com valor desconhecido, ignorando: {}", recebido);
            return null;
        }
    }
}

