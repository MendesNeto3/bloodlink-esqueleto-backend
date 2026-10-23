package com.bloodlink.auth.usuario.application.in;

public record CadastrarUsuarioCommand
        (String nome,
         String email,
         String senha,
         String papel,
         String centroId ){
}
