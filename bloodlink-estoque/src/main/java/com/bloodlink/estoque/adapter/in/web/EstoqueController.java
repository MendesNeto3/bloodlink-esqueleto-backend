package com.bloodlink.estoque.adapter.in.web;

import com.bloodlink.estoque.adapter.in.web.dto.BaixaRequest;
import com.bloodlink.estoque.adapter.in.web.dto.BaixaResponse;
import com.bloodlink.estoque.adapter.in.web.dto.EntradaLoteRequest;
import com.bloodlink.estoque.adapter.in.web.dto.LoteResponse;
import com.bloodlink.estoque.adapter.in.web.dto.MovimentacaoResponse;
import com.bloodlink.estoque.adapter.in.web.dto.ParametrosRequest;
import com.bloodlink.estoque.application.port.in.EstoqueUseCase;
import com.bloodlink.estoque.application.port.in.EstoqueUseCase.BaixaComando;
import com.bloodlink.estoque.application.port.in.EstoqueUseCase.EntradaLoteComando;
import com.bloodlink.estoque.application.port.in.EstoqueUseCase.ResultadoEntrada;
import com.bloodlink.estoque.domain.ResumoEstoque;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
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
    public ResponseEntity<ResumoEstoque> resumo() {
        return ResponseEntity.ok(estoque.resumo());
    }

    @GetMapping("/lotes")
    public ResponseEntity<List<LoteResponse>> lotes(@RequestParam(name = "tipo", required = false) String tipo,
                                                    @RequestParam(name = "incluirVencidos", defaultValue = "false")
                                                    boolean incluirVencidos) {
        return ResponseEntity.ok(estoque.listarLotes(tipo, incluirVencidos).stream().map(LoteResponse::de).toList());
    }

    /** 201 na primeira vez; 200 se a mesma Idempotency-Key repetir a mesma entrada. */
    @PostMapping("/lotes")
    public ResponseEntity<LoteResponse> registrarEntrada(
            @RequestBody EntradaLoteRequest corpo,
            @RequestHeader(name = "Idempotency-Key", required = false) String chaveIdempotencia,
            Authentication autenticacao) {
        ResultadoEntrada resultado = estoque.registrarEntrada(new EntradaLoteComando(
                corpo.tipoSanguineo(), corpo.quantidadeUnidades(), corpo.dataColeta(),
                corpo.dataValidade(), chaveIdempotencia, usuarioId(autenticacao)));
        return ResponseEntity.status(resultado.criado() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(LoteResponse.de(resultado.lote()));
    }

    @PostMapping("/baixas")
    public ResponseEntity<BaixaResponse> darBaixa(@RequestBody BaixaRequest corpo, Authentication autenticacao) {
        return ResponseEntity.ok(BaixaResponse.de(estoque.darBaixa(new BaixaComando(
                corpo.tipoSanguineo(), corpo.quantidadeUnidades(), corpo.motivo(), usuarioId(autenticacao)))));
    }

    @PatchMapping("/{tipo}/parametros")
    public ResponseEntity<Void> atualizarParametros(@PathVariable("tipo") String tipo,
                                                    @RequestBody ParametrosRequest corpo) {
        estoque.atualizarParametros(tipo, corpo.limiarCritico(), corpo.metaUnidades());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/movimentacoes")
    public ResponseEntity<List<MovimentacaoResponse>> movimentacoes(
            @RequestParam(name = "tipo", required = false) String tipo,
            @RequestParam(name = "de", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(name = "ate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return ResponseEntity.ok(estoque.listarMovimentacoes(tipo, de, ate).stream()
                .map(MovimentacaoResponse::de).toList());
    }

    private static UUID usuarioId(Authentication autenticacao) {
        return UUID.fromString(autenticacao.getName());
    }
}