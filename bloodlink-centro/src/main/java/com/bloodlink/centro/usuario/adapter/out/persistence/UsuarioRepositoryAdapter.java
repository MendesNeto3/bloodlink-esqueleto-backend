package com.bloodlink.centro.usuario.adapter.out.persistence;

import com.bloodlink.centro.usuario.application.port.out.UsuarioRepositoryPort;
import com.bloodlink.centro.usuario.domain.Usuario;
import com.bloodlink.common.correlation.Papel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository usuarioJpaRepository;

    public UsuarioRepositoryAdapter(UsuarioJpaRepository usuarioJpaRepository) {
        this.usuarioJpaRepository = usuarioJpaRepository;
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioJpaRepository.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        UsuarioJpaEntity entity = new UsuarioJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setEmail(usuario.email());
        entity.setSenhaHash(usuario.senhaHash());
        entity.setPapel(usuario.papel().name());
        entity.setCentroId(usuario.centroId());

        UsuarioJpaEntity salvo = usuarioJpaRepository.save(entity);
        return toDomain(salvo);
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return usuarioJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Usuario> usariosPorCentro(UUID centroId) {
        return usuarioJpaRepository.findById(centroId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Usuario toDomain(UsuarioJpaEntity entity) {
        return new Usuario(entity.getId(), entity.getEmail(), entity.getSenhaHash(),
                Papel.valueOf(entity.getPapel()), entity.getCentroId());
    }
}
