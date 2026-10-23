package com.bloodlink.api.shared.log;

import java.util.regex.Pattern;
/** Mascara dados pessoais (LGPD) antes de irem para logs ou corpos de erro. */
public final class MascaraLog {

    private static final Pattern SEGMENTO_PII =
            Pattern.compile("(?i)(/(?:cpf|email|nome)/)[^/?#]+");
    private static final Pattern PARAM_PII =
            Pattern.compile("(?i)([?&](?:email|nome|cpf)=)[^&#]*");
    private static final Pattern CPF =
            Pattern.compile("\\b(\\d{3})[.]?\\d{3}[.]?\\d{3}[-]?(\\d{2})\\b");
    private static final Pattern EMAIL =
            Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");

    private MascaraLog() {}

    public static String uri(String uri) {
        if (uri == null) return null;
        String semPath = SEGMENTO_PII.matcher(uri).replaceAll("$1***");
        return PARAM_PII.matcher(semPath).replaceAll("$1***");
    }

    public static String email(String email) {
        if (email == null || email.isBlank()) return email;
        int at = email.indexOf('@');
        return at <= 0 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }

    public static String texto(String texto) {
        if (texto == null) return null;
        String semCpf = CPF.matcher(texto).replaceAll("$1.***.***-$2");
        return EMAIL.matcher(semCpf).replaceAll("***@***");
    }

    public static String causa(Throwable t) {
        if (t == null) return "desconhecida";
        Throwable raiz = t;
        while (raiz.getCause() != null && raiz.getCause() != raiz) {
            raiz = raiz.getCause();
        }
        String sqlState = raiz instanceof java.sql.SQLException s
                ? " sqlState=" + s.getSQLState() : "";
        return raiz.getClass().getSimpleName() + sqlState;
    }
}