package com.bloodlink.tenant;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Pattern;

public class TenantResolverFilter implements Filter {

    private static final Logger log =  LoggerFactory.getLogger(TenantResolverFilter.class);

    public static final String HEADER_TENANT = "X-Tenant-Id";
    private static final String PREFIXO_SCHEMA = "centro_schema";
    private static final Pattern CENTRO_ID_VALIDO = Pattern.compile("[a-zA-Z0-9]{8}");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String centroId = ((HttpServletRequest) request).getHeader(HEADER_TENANT);
            if (centroId != null && !centroId.isBlank()) {
                if(CENTRO_ID_VALIDO.matcher(centroId).matches()) {
                    TenantContext.set(PREFIXO_SCHEMA + centroId);
                } else {
                    log.warn("X-Tenant-Id fora do formato esperado, ignorando: {}", centroId);
                }
            }

            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
