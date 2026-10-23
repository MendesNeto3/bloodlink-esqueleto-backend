package com.bloodlink.auth.usuario;

import java.time.Instant;
import java.util.UUID;

public record Usuario(
        UUID id,
        String email,
        String nome,
        String senhaHash,
        Papel papel,
        String centroId,
        boolean ativo,
        Instant criadoEm,
        Instant atualizadoEm,
        UUID doadorId,
        StatusUsuario status
) {

    /** Construtor da equipe (sem doador, já ativo): mantém compatíveis os chamadores antigos, como o AdminInicialRunner. */
    public Usuario(UUID id, String email, String nome, String senhaHash, Papel papel, String centroId,
                   boolean ativo, Instant criadoEm, Instant atualizadoEm) {
        this(id, email, nome, senhaHash, papel, centroId, ativo, criadoEm, atualizadoEm, null, StatusUsuario.ATIVO);
    }

    /** Conta de doador criada sem senha; só passa a logar depois de {@link #ativadoComSenha}. */
    public static Usuario doadorPendente(UUID id, String email, String nome, String centroId, UUID doadorId, Instant agora) {
        return new Usuario(id, email, nome, null, Papel.DOADOR, centroId, true, agora, agora, doadorId, StatusUsuario.PENDENTE);
    }

    public boolean pendente() {
        return status == StatusUsuario.PENDENTE;
    }

    /** Pode autenticar: conta ativa (não inativada) e com a ativação concluída. */
    public boolean podeLogar() {
        return ativo && status == StatusUsuario.ATIVO && senhaHash != null;
    }

    public Usuario ativadoComSenha(String novoHash, Instant agora) {
        return new Usuario(id, email, nome, novoHash, papel, centroId, ativo, criadoEm, agora, doadorId, StatusUsuario.ATIVO);
    }

    public Usuario comSenha(String novoHash, Instant agora) {
        return new Usuario(id, email, nome, novoHash, papel, centroId, ativo, criadoEm, agora, doadorId, status);
    }

    public Usuario inativado(Instant agora) {
        return new Usuario(id, email, nome, senhaHash, papel, centroId, false, criadoEm, agora, doadorId, status);
    }

    public Usuario reativado(Instant agora) {
        return new Usuario(id, email, nome, senhaHash, papel, centroId, true, criadoEm, agora, doadorId, status);
    }

    /** O toString padrão de um record imprimiria o hash da senha e o e-mail; aqui nada sensível sai. */
    @Override
    public String toString() {
        return "Usuario[id=" + id + ", papel=" + papel + ", centroId=" + centroId
                + ", ativo=" + ativo + ", status=" + status + "]";
    }
}