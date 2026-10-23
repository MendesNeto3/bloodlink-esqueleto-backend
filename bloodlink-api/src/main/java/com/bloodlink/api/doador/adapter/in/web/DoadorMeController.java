package com.bloodlink.api.doador.adapter.in.web;

import com.bloodlink.api.doador.adapter.in.web.dto.AtualizarMeuPerfilRequest;
import com.bloodlink.api.doador.adapter.in.web.dto.DoadorResponse;
import com.bloodlink.api.doador.adapter.in.web.dto.FichaPreDoacaoRequest;
import com.bloodlink.api.doador.adapter.in.web.dto.FichaPreDoacaoResponse;
import com.bloodlink.api.doador.application.port.in.*;
import com.bloodlink.api.doador.application.port.out.DoacaoResponse;
import com.bloodlink.api.doador.application.port.out.ElegibilidadeResponse;
import com.bloodlink.common.seguranca.JwtAuthenticationFilter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doadores/me")
public class DoadorMeController {

    private final ConsultarDoadorUseCase consultarUseCase;
    private final ConsultarElegibilidadeUseCase elegibilidadeUseCase;
    private final DoacaoUseCase doacaoUseCase;
    private final FichaPreDoacaoUseCase fichaUseCase;
    private final AtualizarPerfilDoadorUseCase perfilUseCase;

    public DoadorMeController(ConsultarDoadorUseCase consultarUseCase,
                              ConsultarElegibilidadeUseCase elegibilidadeUseCase,
                              DoacaoUseCase doacaoUseCase,
                              FichaPreDoacaoUseCase fichaUseCase,
                              AtualizarPerfilDoadorUseCase perfilUseCase) {
        this.consultarUseCase = consultarUseCase;
        this.elegibilidadeUseCase = elegibilidadeUseCase;
        this.doacaoUseCase = doacaoUseCase;
        this.fichaUseCase = fichaUseCase;
        this.perfilUseCase = perfilUseCase;
    }

    @GetMapping
    public ResponseEntity<DoadorResponse> meuCadastro(
            @RequestHeader(JwtAuthenticationFilter.HEADER_DOADOR) UUID doadorId) {
        return ResponseEntity.ok(DoadorResponse.de(consultarUseCase.porId(doadorId)));
    }

    @PutMapping
    public ResponseEntity<DoadorResponse> atualizarMeuPerfil(
            @RequestHeader(JwtAuthenticationFilter.HEADER_DOADOR) UUID doadorId,
            @RequestBody AtualizarMeuPerfilRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        return ResponseEntity.ok(DoadorResponse.de(perfilUseCase.atualizar(request.paraComando(doadorId))));
    }

    @GetMapping("/elegibilidade")
    public ResponseEntity<ElegibilidadeResponse> minhaElegibilidade(
            @RequestHeader(JwtAuthenticationFilter.HEADER_DOADOR) UUID doadorId) {
        return ResponseEntity.ok(ElegibilidadeResponse.de(elegibilidadeUseCase.consultar(doadorId)));
    }

    @GetMapping("/doacoes")
    public ResponseEntity<List<DoacaoResponse>> meuHistorico(
            @RequestHeader(JwtAuthenticationFilter.HEADER_DOADOR) UUID doadorId) {
        return ResponseEntity.ok(doacaoUseCase.historico(doadorId).stream().map(DoacaoResponse::de).toList());
    }

    @GetMapping("/ficha")
    public ResponseEntity<FichaPreDoacaoResponse> minhaFicha(
            @RequestHeader(JwtAuthenticationFilter.HEADER_DOADOR) UUID doadorId) {
        return ResponseEntity.ok(FichaPreDoacaoResponse.de(fichaUseCase.consultar(doadorId)));
    }

    @PutMapping("/ficha")
    public ResponseEntity<FichaPreDoacaoResponse> atualizarMinhaFicha(
            @RequestHeader(JwtAuthenticationFilter.HEADER_DOADOR) UUID doadorId,
            @RequestBody FichaPreDoacaoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        return ResponseEntity.ok(FichaPreDoacaoResponse.de(fichaUseCase.salvar(request.paraComando(doadorId))));
    }
}