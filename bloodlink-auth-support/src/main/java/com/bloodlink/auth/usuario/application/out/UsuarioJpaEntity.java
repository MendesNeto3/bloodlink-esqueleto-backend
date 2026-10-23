package com.bloodlink.auth.usuario.application.out;

import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.StatusUsuario;
import com.bloodlink.auth.usuario.Usuario;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario")
class UsuarioJpaEntity {

    @Id
    UUID id;

    @Column(nullable = false, length = 120)
    String email;

    @Column(nullable = false, length = 120)
    String nome;

    /** Nulo enquanto a conta do doador está PENDENTE. */
    @Column(name = "senha_hash", length = 100)
    String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    Papel papel;

    @Column(name = "centro_id", length = 36)
    String centroId;

    @Column(nullable = false)
    boolean ativo;

    @Column(name = "criado_em", nullable = false)
    Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    Instant atualizadoEm;

    @Column(name = "doador_id")
    UUID doadorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    StatusUsuario status;

    protected UsuarioJpaEntity() { }

    static UsuarioJpaEntity de(Usuario u) {
        UsuarioJpaEntity e = new UsuarioJpaEntity();
        e.id = u.id();
        e.email = u.email();
        e.nome = u.nome();
        e.senhaHash = u.senhaHash();
        e.papel = u.papel();
        e.centroId = u.centroId();
        e.ativo = u.ativo();
        e.criadoEm = u.criadoEm();
        e.atualizadoEm = u.atualizadoEm();
        e.doadorId = u.doadorId();
        e.status = u.status();
        return e;
    }

    Usuario paraDominio() {
        return new Usuario(id, email, nome, senhaHash, papel, centroId, ativo, criadoEm, atualizadoEm, doadorId, status);
    }
}