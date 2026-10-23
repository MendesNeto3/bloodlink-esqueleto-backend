package com.bloodlink.auth.usuario.application.service;

import java.nio.charset.StandardCharsets;

import java.util.Locale;
import java.util.regex.Pattern;

final class ValidarUsuario {

    private static final Pattern EMAIL = Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");
    private static final Pattern HEX12 = Pattern.compile("^[a-f0-9]{12}$");
    private static final Pattern UUID_CANONICO =
            Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    /** Limite do BCrypt: bytes além do 72º seriam ignorados em silêncio. */
    private static final int MAX_BYTES_SENHA = 72;

    private ValidarUsuario() { }

    static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório");
        }
        String limpo = nome.trim().replaceAll("\\s+", " ");
        if (limpo.length() < 3 || limpo.length() > 120) {
            throw new IllegalArgumentException("O nome deve ter entre 3 e 120 caracteres");
        }
        return limpo;
    }

    static String validarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O e-mail é obrigatório");
        }
        String limpo = email.trim().toLowerCase(Locale.ROOT);
        if (limpo.length() > 120 || !EMAIL.matcher(limpo).matches()) {
            throw new IllegalArgumentException("E-mail inválido");
        }
        return limpo;
    }

    static void validarSenha(String senha) {
        if (senha == null || senha.isEmpty()) {
            throw new IllegalArgumentException("A senha é obrigatória");
        }
        if (senha.length() < 8) {
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_SENHA) {
            throw new IllegalArgumentException("A senha deve ter no máximo " + MAX_BYTES_SENHA + " bytes");
        }
        boolean temLetra = senha.chars().anyMatch(Character::isLetter);
        boolean temDigito = senha.chars().anyMatch(Character::isDigit);
        if (!temLetra || !temDigito) {
            throw new IllegalArgumentException("A senha deve conter letras e números");
        }
    }

    static String validarCentroId(String centroId) {
        if (centroId == null || centroId.isBlank()) {
            return null;
        }
        String limpo = centroId.trim().toLowerCase(Locale.ROOT);
        if (!HEX12.matcher(limpo).matches() && !UUID_CANONICO.matcher(limpo).matches()) {
            throw new IllegalArgumentException(
                    "centroId inválido: informe 12 caracteres hexadecimais ou um UUID");
        }
        return limpo;
    }
}