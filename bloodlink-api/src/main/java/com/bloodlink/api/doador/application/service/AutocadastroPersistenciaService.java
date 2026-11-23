package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.adapter.out.persistence.FichaPreDoacaoRepositoryPort;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.domain.Doador;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Parte transacional do autocadastro: doador e ficha são gravados juntos ou nenhum dos dois.
 * Fica em um bean à parte porque o TenantContext precisa estar definido ANTES de abrir a transação.
 */
@Service
public class AutocadastroPersistenciaService {

    /** Quem deve receber o link de ativação (id, nome e e-mail do cadastro que vale). */
    public record Resultado(UUID doadorId, String nome, String email, boolean enviarConvite) {
        static Resultado nenhum() {
            return new Resultado(null, null, null, false);
        }

        @Override
        public String toString() {
            return "Resultado[doadorId=" + doadorId + ", enviarConvite=" + enviarConvite + "]";
        }
    }

    private final DoadorRepositoryPort doadores;
    private final FichaPreDoacaoRepositoryPort fichas;

    public AutocadastroPersistenciaService(DoadorRepositoryPort doadores, FichaPreDoacaoRepositoryPort fichas) {
        this.doadores = doadores;
        this.fichas = fichas;
    }

    @Transactional
    public Resultado registrarOuLocalizar(Doador novo, FichaPreDoacao ficha) {
        Optional<Doador> existente = doadores.buscarPorCpf(novo.cpf());
        if (existente.isPresent()) {
            Doador atual = existente.get();
            // CPF já cadastrado: o link só vai para o e-mail que JÁ está no cadastro. Quem não
            // souber o e-mail cadastrado não recebe nada e nem descobre se o CPF existe.
            boolean mesmoEmail = atual.ativo() && atual.email() != null
                    && atual.email().equalsIgnoreCase(novo.email());
            return mesmoEmail
                    ? new Resultado(atual.id(), atual.nome(), atual.email(), true)
                    : Resultado.nenhum();
        }
        Doador salvo = doadores.salvar(novo);
        fichas.salvar(ficha);
        return new Resultado(salvo.id(), salvo.nome(), salvo.email(), true);
    }
}
