package com.bloodlink.estoque.adapter.in.web;

import com.bloodlink.estoque.adapter.in.web.dto.BaixaRequest;
import com.bloodlink.estoque.adapter.in.web.dto.BaixaResponse;
import com.bloodlink.estoque.adapter.in.web.dto.EntradaLoteRequest;
import com.bloodlink.estoque.adapter.in.web.dto.LoteResponse;
import com.bloodlink.estoque.adapter.in.web.dto.ParametrosRequest;
import com.bloodlink.estoque.application.port.in.EstoqueUseCase;
import com.bloodlink.estoque.application.port.in.EstoqueUseCase.BaixaComando;
import com.bloodlink.estoque.application.port.in.EstoqueUseCase.EntradaLoteComando;
import com.bloodlink.estoque.domain.ResumoEstoque;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/estoque")
public class EstoqueController {

    private final EstoqueUseCase estoque;

    public EstoqueController(EstoqueUseCase estoque) {
        this.estoque = estoque;
    }

    @GetMapping("/resumo")
    public ResumoEstoque resumo() {
        return estoque.resumo();
    }

    @GetMapping("/lotes")
    public List<LoteResponse> lotes(@RequestParam(name = "tipo", required = false) String tipo,
                                    @RequestParam(name = "incluirVencidos", defaultValue = "false")
                                    boolean incluirVencidos) {
        return estoque.listarLotes(tipo, incluirVencidos).stream().map(LoteResponse::de).toList();
    }

    @PostMapping("/lotes")
    @ResponseStatus(HttpStatus.CREATED)
    public LoteResponse registrarEntrada(@RequestBody EntradaLoteRequest corpo, Authentication autenticacao) {
        return LoteResponse.de(estoque.registrarEntrada(new EntradaLoteComando(
                corpo.tipoSanguineo(), corpo.quantidadeUnidades(), corpo.dataColeta(),
                corpo.dataValidade(), usuarioId(autenticacao))));
    }

    @PostMapping("/baixas")
    public BaixaResponse darBaixa(@RequestBody BaixaRequest corpo, Authentication autenticacao) {
        return BaixaResponse.de(estoque.darBaixa(new BaixaComando(
                corpo.tipoSanguineo(), corpo.quantidadeUnidades(), usuarioId(autenticacao))));
    }

    @PatchMapping("/{tipo}/parametros")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void atualizarParametros(@PathVariable("tipo") String tipo, @RequestBody ParametrosRequest corpo) {
        estoque.atualizarParametros(tipo, corpo.limiarCritico(), corpo.metaUnidades());
    }

    private static UUID usuarioId(Authentication autenticacao) {
        return UUID.fromString(autenticacao.getName());
    }
}