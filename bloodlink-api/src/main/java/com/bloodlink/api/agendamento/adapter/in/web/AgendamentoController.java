package com.bloodlink.api.agendamento.adapter.in.web;

import com.bloodlink.api.agendamento.adapter.in.web.dto.*;
import com.bloodlink.api.agendamento.application.usecase.AgendamentoUseCase;
import com.bloodlink.api.agendamento.application.usecase.ConsultarHorarioUseCase;
import com.bloodlink.api.agendamento.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoUseCase agendamentoUseCase;
    private final ConsultarHorarioUseCase horarioUseCase;

    public AgendamentoController(AgendamentoUseCase agendamentoUseCase, ConsultarHorarioUseCase horarioUseCase) {
        this.agendamentoUseCase = agendamentoUseCase;
        this.horarioUseCase = horarioUseCase;
    }

    @GetMapping
    public ResponseEntity<List<AgendaAgendamentoResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false) UUID doadorId,
            @RequestParam(defaultValue = "100") int limite) {
        return ResponseEntity.ok(agendamentoUseCase
                .listarAgenda(new FiltroAgendamento(de, ate, status, doadorId), limite)
                .stream().map(AgendaAgendamentoResponse::de).toList());
    }

    @GetMapping("/horarios")
    public ResponseEntity<HorariosEquipeResponse> horarios(
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(HorariosEquipeResponse.de(horarioUseCase.consultarParaEquipe(data)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgendamentoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(AgendamentoResponse.de(agendamentoUseCase.buscar(id)));
    }

    @PostMapping
    public ResponseEntity<AgendamentoResponse> agendar(@RequestBody AgendarPelaEquipeRequest request,
                                                       Authentication autenticacao) {
        if (request == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        Agendamento criado = agendamentoUseCase.solicitar(new SolicitarAgendamentoComando(
                request.doadorId(), request.data(), request.hora(), request.observacao(),
                OrigemAgendamento.EQUIPE, usuarioId(autenticacao)));
        return ResponseEntity.created(URI.create("/api/agendamentos/" + criado.id()))
                .body(AgendamentoResponse.de(criado));
    }

    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<AgendamentoResponse> confirmar(@PathVariable UUID id) {
        return ResponseEntity.ok(AgendamentoResponse.de(agendamentoUseCase.confirmar(id)));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<AgendamentoResponse> cancelar(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelarAgendamentoRequest request) {
        String motivo = request == null ? null : request.motivo();
        return ResponseEntity.ok(AgendamentoResponse.de(agendamentoUseCase.cancelarPelaEquipe(id, motivo)));
    }

    @PatchMapping("/{id}/concluir")
    public ResponseEntity<AgendamentoResponse> concluir(
            @PathVariable UUID id,
            @RequestBody(required = false) ConcluirAgendamentoRequest request,
            Authentication autenticacao) {
        int volume = request == null || request.volumeMl() == null ? 0 : request.volumeMl();
        return ResponseEntity.ok(AgendamentoResponse.de(
                agendamentoUseCase.concluir(id, volume, usuarioId(autenticacao))));
    }

    @PatchMapping("/{id}/faltou")
    public ResponseEntity<AgendamentoResponse> faltou(@PathVariable UUID id) {
        return ResponseEntity.ok(AgendamentoResponse.de(agendamentoUseCase.registrarFalta(id)));
    }

    /** O nome da autenticação é o subject do token, que é o id do usuário. */
    private static UUID usuarioId(Authentication autenticacao) {
        return UUID.fromString(autenticacao.getName());
    }
}