package com.bloodlink.auth.usuario.application.out;

import com.bloodlink.auth.shared.pagina.Pagina;
import com.bloodlink.auth.usuario.Papel;
import com.bloodlink.auth.usuario.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositoryPort {

    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(UUID id);

    Optional<Usuario> buscarPorEmail(String email);

    List<Usuario> buscarPorNome(String nome, String centroId);

    boolean existePorPapel(Papel papel);

    Pagina<Usuario> listar(FiltroUsuario filtro, int pagina, int tamanho);
}