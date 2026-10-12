package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.domain.Cpf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.regex.Pattern;

final class ValidacoesDoador {

    private static final int IDADE_MINIMA_CADASTRO = 16;
    private static final int MAIORIDADE = 18;
    private static final BigDecimal PESO_MINIMO = new BigDecimal("20");
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("300");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private ValidacoesDoador() {}

    static String Validarnome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O campo 'nome' é obrigatório");
        }
        String limpo = nome.trim().replaceAll("\\s+", " ");
        if (limpo.length() < 3 || limpo.length() > 120) {
            throw new IllegalArgumentException("O campo 'nome' deve ter entre 3 e 120 caracteres");
        }
        return limpo;
    }

    static String validarCpf(String cpf) {
        String digitos = Cpf.normalizar(cpf);
        if (!Cpf.valido(digitos)) {
            throw new IllegalArgumentException("CPF inválido");
        }
        return digitos;
    }

    static LocalDate validarDataNascimento(LocalDate nascimento, LocalDate hoje) {
        if (nascimento == null) {
            throw new IllegalArgumentException("O campo 'dataNascimento' é obrigatório");
        }
        if (nascimento.isAfter(hoje)) {
            throw new IllegalArgumentException("A data de nascimento não pode estar no futuro");
        }
        if (Period.between(nascimento, hoje).getYears() > 120) {
            throw new IllegalArgumentException("Data de nascimento inválida");
        }
        return nascimento;
    }

    static BigDecimal validarPeso(BigDecimal peso) {
        if (peso == null) {
            throw new IllegalArgumentException("O campo 'pesoKg' é obrigatório");
        }
        if (peso.compareTo(PESO_MINIMO) < 0 || peso.compareTo(PESO_MAXIMO) > 0) {
            throw new IllegalArgumentException("O campo 'pesoKg' deve estar entre 20 e 300");
        }
        return peso.setScale(2, RoundingMode.HALF_UP);
    }

    static String validarTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return null;
        }
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() != 10 && digitos.length() != 11) {
            throw new IllegalArgumentException("Telefone inválido: informe DDD e número");
        }
        return digitos;
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

    /** Cadastro exige 16+ anos; entre 16 e 17 exige consentimento do responsável. */
    static void idadeEConsentimento(LocalDate nascimento, boolean consentimento, LocalDate hoje) {
        int idade = Period.between(nascimento, hoje).getYears();
        if (idade < IDADE_MINIMA_CADASTRO) {
            throw new IllegalArgumentException("O doador deve ter ao menos 16 anos para o cadastro");
        }
        if (idade < MAIORIDADE && !consentimento) {
            throw new IllegalArgumentException("Doadores de 16 e 17 anos precisam de consentimento do responsável");
        }
    }
}
