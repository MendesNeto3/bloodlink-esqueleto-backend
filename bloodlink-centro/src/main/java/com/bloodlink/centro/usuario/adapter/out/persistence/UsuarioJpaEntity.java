package com.bloodlink.centro.usuario.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash",  nullable = false, length = 60)
    private String senhaHash;

    @Column(nullable = false, length = 20)
    private String papel;

    @Column(name = "centro_id", nullable = false)
    private UUID centroId;
}
