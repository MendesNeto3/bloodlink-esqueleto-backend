package com.bloodlink.auth.usuario.application.in;

import com.bloodlink.auth.shared.pagina.Pagina;
import com.bloodlink.auth.usuario.Solicitante;
import com.bloodlink.auth.usuario.Usuario;

public interface ListarUsuariosUseCase {

    Pagina<Usuario> listar(String email, String papel, String centroId, Boolean ativo,
                           int pagina, int tamanho, Solicitante solicitante);
}