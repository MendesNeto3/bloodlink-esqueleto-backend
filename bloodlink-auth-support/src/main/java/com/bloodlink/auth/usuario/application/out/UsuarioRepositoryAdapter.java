package com.bloodlink.auth.usuario.application.out;

import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.pagina.Pagina;
import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import jakarta.persistence.criteria.Predicate;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort{

    private static final String CONSTRAINT_EMAIL = "uk_usuario_email";

    private final UsuarioJpaRepository userJpaRepository;

    public UsuarioRepositoryAdapter(UsuarioJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        try {
            return userJpaRepository.saveAndFlush(UsuarioJpaEntity.de(usuario)).paraDominio();
        } catch (RuntimeException ex) {
            if (violaConstraint(ex, CONSTRAINT_EMAIL)) {
                throw new ConflitoException("Já existe um usuário com este e-mail");
            }
            throw ex;
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return userJpaRepository.findById(id).map(UsuarioJpaEntity::paraDominio);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return userJpaRepository.findByEmail(email).map(UsuarioJpaEntity::paraDominio);
    }

    @Override
    public List<Usuario> buscarPorNome(String nome, String centroId) {
        List<UsuarioJpaEntity> encontrados = (centroId == null)
                ? userJpaRepository.findTop2ByNomeIgnoreCase(nome)
                : userJpaRepository.findTop2ByNomeIgnoreCaseAndCentroId(nome, centroId);
        return encontrados.stream().map(UsuarioJpaEntity::paraDominio).toList();
    }

    @Override
    public boolean existePorPapel(Papel papel) {
        return userJpaRepository.existsByPapel(papel);
    }

    @Override
    public Pagina<Usuario> listar(FiltroUsuario filtro, int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by("nome")
                .ascending().and(Sort.by("id").ascending()));
        Page<UsuarioJpaEntity> resultado = userJpaRepository.findAll(especificacao(filtro), pageable);
        return new Pagina<>(
                resultado.getContent().stream().map(UsuarioJpaEntity::paraDominio).toList(),
                resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages());

    }

    private Specification<UsuarioJpaEntity> especificacao(FiltroUsuario filtro) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (filtro.email() != null) {
                predicados.add(cb.like(cb.lower(root.get("email")),
                        "%" + escaparLike(filtro.email().toLowerCase(Locale.ROOT)) + "%", '\\'));
            }
            if (filtro.papel() != null) {
                predicados.add(cb.equal(root.get("papel"), filtro.papel()));
            }
            if (filtro.centroId() != null) {
                predicados.add(cb.equal(root.get("centroId"), filtro.centroId()));
            }
            if (filtro.ativo() != null) {
                predicados.add(cb.equal(root.get("ativo"), filtro.ativo()));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static boolean violaConstraint(Throwable erro, String constraint) {
        for (Throwable t = erro; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().toLowerCase(Locale.ROOT).contains(constraint)) {
                return true;
            }
        }
        return false;
    }
}

