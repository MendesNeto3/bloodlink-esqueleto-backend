package com.bloodlink.centro.usuario.application.service;

public class AutenticacaoIndisponivelException extends RuntimeException { public AutenticacaoIndisponivelException() {
    super("Não foi possível autenticar no momento");
  }
}
