package com.bloodlink.checkin.adapter.in.web;

import com.bloodlink.checkin.adapter.in.web.dto.CheckinResponse;
import com.bloodlink.checkin.adapter.in.web.dto.FilaResponse;
import com.bloodlink.checkin.adapter.in.web.dto.RegistrarChegadaRequest;
import com.bloodlink.checkin.adapter.in.web.dto.TriagemRequest;
import com.bloodlink.checkin.application.port.in.CheckinUseCase;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

    private final CheckinUseCase checkinUseCase;

    public CheckinController(CheckinUseCase checkinUseCase) {
        this.checkinUseCase = checkinUseCase;
    }

    @PostMapping
    public ResponseEntity<CheckinResponse> registrarChegada(@RequestBody RegistrarChegadaRequest request,
                                                            Authentication autenticacao) {
        if (request == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        var checkin = checkinUseCase.registrarChegada(request.agendamentoId(), usuarioId(autenticacao));
        return ResponseEntity.created(URI.create("/api/checkin/" + checkin.id()))
                .body(CheckinResponse.de(checkin));
    }

    @GetMapping
    public FilaResponse fila(@RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                             @RequestParam(required = false) String situacao) {
        return FilaResponse.de(checkinUseCase.fila(data, situacao));
    }

    @GetMapping("/{id}")
    public CheckinResponse buscar(@PathVariable UUID id) {
        return CheckinResponse.de(checkinUseCase.buscar(id));
    }

    @PatchMapping("/{id}/triagem")
    public CheckinResponse registrarTriagem(@PathVariable UUID id,
                                            @RequestBody TriagemRequest request,
                                            Authentication autenticacao) {
        if (request == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        return CheckinResponse.de(checkinUseCase.registrarTriagem(
                id, request.resultado(), request.observacao(), usuarioId(autenticacao)));
    }

    /** O nome da autenticação é o subject do token, que é o id do usuário. */
    private static UUID usuarioId(Authentication autenticacao) {
        return UUID.fromString(autenticacao.getName());
    }
}