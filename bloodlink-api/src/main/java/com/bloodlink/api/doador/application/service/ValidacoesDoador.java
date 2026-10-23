package com.bloodlink.api.doador.application.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
final class ValidacoesDoador {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Pattern EMAIL = Pattern.compile("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");

    private static final Set<String> UFS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA",
            "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    static final int IDADE_MINIMA = 16;
    private static final int IDADE_MAXIMA_PLAUSIVEL = 120;
    private static final BigDecimal PESO_MIN = BigDecimal.valueOf(20);
    private static final BigDecimal PESO_MAX = BigDecimal.valueOf(300);

    private ValidacoesDoador() { }

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

    static String validarUf(String uf) {
        if (uf == null || uf.isBlank()) {
            return null;
        }
        String sigla = uf.trim().toUpperCase(Locale.ROOT);
        if (!UFS.contains(sigla)) {
            throw new IllegalArgumentException("UF inválida");
        }
        return sigla;
    }

    static String validarCidade(String cidade) {
        if (cidade == null || cidade.isBlank()) {
            return null;
        }
        String limpa = cidade.trim().replaceAll("\\s+", " ");
        if (limpa.length() < 2 || limpa.length() > 80) {
            throw new IllegalArgumentException("A cidade deve ter entre 2 e 80 caracteres");
        }
        return limpa;
    }

    static String validarCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            throw new IllegalArgumentException("O CPF é obrigatório");
        }
        String d = cpf.replaceAll("\\D", "");
        if (d.length() != 11 || d.chars().distinct().count() == 1
                || (d.charAt(9) - '0') != digito(d, 9) || (d.charAt(10) - '0') != digito(d, 10)) {
            throw new IllegalArgumentException("CPF inválido");
        }
        return d;
    }

    private static int digito(String d, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (d.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    static LocalDate validarNascimento(LocalDate nascimento) {
        if (nascimento == null) {
            throw new IllegalArgumentException("A data de nascimento é obrigatória");
        }
        LocalDate hoje = LocalDate.now(FUSO);
        if (nascimento.isAfter(hoje)) {
            throw new IllegalArgumentException("A data de nascimento não pode estar no futuro");
        }
        int idade = idade(nascimento);
        if (idade > IDADE_MAXIMA_PLAUSIVEL) {
            throw new IllegalArgumentException("Data de nascimento inválida");
        }
        if (idade < IDADE_MINIMA) {
            throw new IllegalArgumentException("O doador deve ter ao menos " + IDADE_MINIMA + " anos");
        }
        return nascimento;
    }

    static int idade(LocalDate nascimento) {
        return Period.between(nascimento, LocalDate.now(FUSO)).getYears();
    }

    static void exigirConsentimentoSeAdolescente(LocalDate nascimento, boolean consentimento) {
        if (idade(nascimento) < 18 && !consentimento) {
            throw new IllegalArgumentException(
                    "Doadores de 16 e 17 anos precisam do consentimento do responsável");
        }
    }

    static BigDecimal validarPeso(BigDecimal peso) {
        if (peso == null) {
            throw new IllegalArgumentException("O peso é obrigatório");
        }
        if (peso.compareTo(PESO_MIN) < 0 || peso.compareTo(PESO_MAX) > 0) {
            throw new IllegalArgumentException("O peso deve estar entre 20 e 300 kg");
        }
        return peso.setScale(1, RoundingMode.HALF_UP);
    }

    static String validarTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return null;
        }
        String d = telefone.replaceAll("\\D", "");
        if (d.length() != 10 && d.length() != 11) {
            throw new IllegalArgumentException("Telefone inválido: informe DDD e número");
        }
        return d;
    }

    static String validarEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String limpo = email.trim().toLowerCase(Locale.ROOT);
        if (limpo.length() > 120 || !EMAIL.matcher(limpo).matches()) {
            throw new IllegalArgumentException("E-mail inválido");
        }
        return limpo;
    }
}