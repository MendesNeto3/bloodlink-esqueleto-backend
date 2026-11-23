package com.bloodlink.api.doador.adapter.in.web;

import com.bloodlink.api.doador.adapter.in.web.dto.AutocadastroDoadorRequest;
import com.bloodlink.api.doador.adapter.in.web.dto.AutocadastroDoadorResponse;
import com.bloodlink.api.doador.application.port.in.AutocadastroDoadorUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rota pública (sem token): o hemocentro vem no corpo, não em cabeçalho. */
@RestController
@RequestMapping("/api/publico/doadores")
public class AutocadastroDoadorController {

    static final String MENSAGEM = "Cadastro recebido. Se os dados estiverem corretos, enviamos um link "
            + "para o seu e-mail: abra-o para criar sua senha e ativar a conta.";

    private final AutocadastroDoadorUseCase autocadastro;

    public AutocadastroDoadorController(AutocadastroDoadorUseCase autocadastro) {
        this.autocadastro = autocadastro;
    }

    @PostMapping("/autocadastro")
    public ResponseEntity<AutocadastroDoadorResponse> cadastrar(@RequestBody AutocadastroDoadorRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        autocadastro.cadastrar(request.paraComando());
        return ResponseEntity.accepted().body(new AutocadastroDoadorResponse(MENSAGEM));
    }
}
