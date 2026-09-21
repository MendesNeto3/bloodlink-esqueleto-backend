package com.bloodlink.centro.adapter.in.web;

import com.bloodlink.centro.application.service.CentroNaoEncontradoException;
import com.bloodlink.centro.usuario.application.service.AutenticacaoIndisponivelException;
import com.bloodlink.centro.usuario.application.service.CadastroIndisponivelException;
import com.bloodlink.centro.usuario.application.service.EmailJaCadastradoException;
import com.bloodlink.centro.usuario.application.service.UsuarioNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CentroExceptionHandler {

    @ExceptionHandler(CentroNaoEncontradoException.class)
    public ResponseEntity<String> handleCentroNaoEncontradoException(CentroNaoEncontradoException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<String>  handleUsuarioNaoEncontradoException(UsuarioNaoEncontradoException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<String> handleEmailJaCadastrado(EmailJaCadastradoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(CadastroIndisponivelException.class)
    public ResponseEntity<String> handleCadastroIndisponivel(CadastroIndisponivelException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
    }

    @ExceptionHandler(AutenticacaoIndisponivelException.class)
    public ResponseEntity<String> handleAutenticacaoIndisponivel(AutenticacaoIndisponivelException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
    }
}
