package com.bloodlink.tenant;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
public class TenantResolverFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantResolverFilter.class);

    public static final String HEADER_TENANT = "X-Tenant-Id";
    private static final String PREFIXO_SCHEMA = "centro_";
    // os 12 hexadecimais que vêm depois de "centro_" no nome do schema
    private static final Pattern TENANT_ID_VALIDO = Pattern.compile("[a-f0-9]{12}");
    // rotas que trabalham no schema public e não precisam de tenant
    private static final List<String> CAMINHOS_SEM_TENANT = List.of("/api/tenants/", "/actuator/");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String caminho = request.getRequestURI();
        return CAMINHOS_SEM_TENANT.stream().anyMatch(caminho::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String tenantId = request.getHeader(HEADER_TENANT);
        if (tenantId == null || !TENANT_ID_VALIDO.matcher(tenantId.trim()).matches()) {
            log.warn("Requisição a {} sem {} válido", request.getRequestURI(), HEADER_TENANT);
            responderErro(request, response);
            return;
        }
        try {
            TenantContext.set(PREFIXO_SCHEMA + tenantId.trim());
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void responderErro(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":400,"
                + "\"error\":\"Cabeçalho " + HEADER_TENANT + " ausente ou inválido\","
                + "\"body\":\"" + escapar(request.getRequestURI()) + "\"}");
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}