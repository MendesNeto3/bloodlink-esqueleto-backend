package com.bloodlink.common.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

/**
 * Valida o JWT e passa a ser a ÚNICA fonte de X-Tenant-Id e X-User-Role.
 * Os valores enviados pelo cliente nesses dois cabeçalhos são sempre descartados.
 * Não é @Component: é registrado só dentro da cadeia do Spring Security.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_TENANT = "X-Tenant-Id";
    public static final String HEADER_PAPEL = "X-User-Role";
    public static final String HEADER_DOADOR = "X-Doador-Id";

    private static final String PREFIXO_BEARER = "bearer ";

    private static final String ADMIN_PLATAFORMA = "ADMIN_PLATAFORMA";
    private static final String DOADOR = "DOADOR";
    private static final Set<String> PAPEIS = Set.of(
            ADMIN_PLATAFORMA, "ADMIN_CENTRO", "RECEPCAO", "ENFERMEIRO", DOADOR);

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /** Cria o principal da autenticação a partir do token já validado. Padrão: o próprio subject. */
    @FunctionalInterface
    public interface FabricaDePrincipal {
        Object criar(String subject, String papel, String centroId);
    }

    private final JwtSupport jwtSupport;
    private final FabricaDePrincipal fabricaDePrincipal;

    public JwtAuthenticationFilter(JwtSupport jwtSupport) {
        this(jwtSupport, (subject, papel, centroId) -> subject);
    }

    public JwtAuthenticationFilter(JwtSupport jwtSupport, FabricaDePrincipal fabricaDePrincipal) {
        this.jwtSupport = jwtSupport;
        this.fabricaDePrincipal = fabricaDePrincipal;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Map<String, String> confiaveis = Map.of();

        Optional<JwtSupport.DadosToken> dados = jwtSupport.validar(extrairToken(request));
        if (dados.isPresent()) {
            Optional<Map<String, String>> cabecalhos = montarCabecalhos(dados.get());
            if (cabecalhos.isPresent()) {
                Optional<Object> principal = criarPrincipal(dados.get().subject(), cabecalhos.get());
                if (principal.isPresent()) {
                    confiaveis = cabecalhos.get();
                    autenticar(principal.get(), confiaveis.get(HEADER_PAPEL));
                }
            }
        }
        // Sem token válido segue anônimo (as rotas protegidas respondem 401),
        // mas os cabeçalhos de contexto do cliente continuam sendo descartados.
        chain.doFilter(new CabecalhosConfiaveis(request, confiaveis), response);
    }

    private Optional<Object> criarPrincipal(String subject, Map<String, String> cabecalhos) {
        try {
            return Optional.ofNullable(fabricaDePrincipal.criar(
                    subject, cabecalhos.get(HEADER_PAPEL), cabecalhos.get(HEADER_TENANT)));
        } catch (IllegalArgumentException e) {
            log.warn("Token incompatível com o principal do serviço rejeitado");
            return Optional.empty();
        }
    }

    private static void autenticar(Object principal, String papel) {
        var autenticacao = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + papel)));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacao);
        SecurityContextHolder.setContext(contexto);
    }

    /**
     * Converte os dados do token nos cabeçalhos confiáveis; vazio = token inconsistente (segue anônimo).
     * Regras: papel conhecido; só a plataforma dispensa centro; DOADOR exige doadorId (UUID) e os demais não o têm.
     */
    private Optional<Map<String, String>> montarCabecalhos(JwtSupport.DadosToken dados) {
        if (dados.papel() == null || dados.subject() == null) {
            log.warn("Token sem papel ou subject rejeitado");
            return Optional.empty();
        }
        String papel = dados.papel().trim().toUpperCase(Locale.ROOT);
        if (!PAPEIS.contains(papel)) {
            log.warn("Token com papel desconhecido rejeitado");
            return Optional.empty();
        }

        Map<String, String> cabecalhos = new LinkedHashMap<>();
        cabecalhos.put(HEADER_PAPEL, papel);

        if (!ADMIN_PLATAFORMA.equals(papel)) {
            Optional<String> tenant = TenantIds.normalizar(dados.centroId());
            if (tenant.isEmpty()) {
                log.warn("Token com centroId inválido rejeitado");
                return Optional.empty();
            }
            cabecalhos.put(HEADER_TENANT, tenant.get());
        }

        if (DOADOR.equals(papel)) {
            Optional<String> doadorId = normalizarUuid(dados.doadorId());
            if (doadorId.isEmpty()) {
                log.warn("Token de doador sem doadorId válido rejeitado");
                return Optional.empty();
            }
            cabecalhos.put(HEADER_DOADOR, doadorId.get());
        } else if (dados.doadorId() != null) {
            log.warn("Token de {} com doadorId rejeitado", papel);
            return Optional.empty();
        }
        return Optional.of(Map.copyOf(cabecalhos));
    }

    private static Optional<String> normalizarUuid(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(valor.trim()).toString());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String extrairToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, PREFIXO_BEARER, 0, PREFIXO_BEARER.length())) {
            return null;
        }
        return header.substring(PREFIXO_BEARER.length()).trim();
    }

    /** Mascara os três cabeçalhos de contexto e os substitui pelos valores vindos do token. */
    private static final class CabecalhosConfiaveis extends HttpServletRequestWrapper {

        private final Map<String, String> confiaveis;

        CabecalhosConfiaveis(HttpServletRequest request, Map<String, String> confiaveis) {
            super(request);
            this.confiaveis = confiaveis;
        }

        private static boolean controlado(String nome) {
            return HEADER_TENANT.equalsIgnoreCase(nome)
                    || HEADER_PAPEL.equalsIgnoreCase(nome)
                    || HEADER_DOADOR.equalsIgnoreCase(nome);
        }

        private String valorConfiavel(String nome) {
            return confiaveis.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(nome))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public String getHeader(String nome) {
            return controlado(nome) ? valorConfiavel(nome) : super.getHeader(nome);
        }

        @Override
        public Enumeration<String> getHeaders(String nome) {
            if (!controlado(nome)) {
                return super.getHeaders(nome);
            }
            String valor = valorConfiavel(nome);
            return valor == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(valor));
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> nomes = new LinkedHashSet<>();
            Enumeration<String> originais = super.getHeaderNames();
            if (originais != null) {
                for (String n : Collections.list(originais)) {
                    if (!controlado(n)) {
                        nomes.add(n);
                    }
                }
            }
            nomes.addAll(confiaveis.keySet());
            return Collections.enumeration(new ArrayList<>(nomes));
        }
    }
}