package com.bloodlink.api.agendamento.adapter.in.web;

import com.bloodlink.api.agendamento.adapter.in.web.dto.AgendamentoResponse;
import com.bloodlink.api.agendamento.adapter.in.web.dto.CancelarAgendamentoRequest;
import com.bloodlink.api.agendamento.adapter.in.web.dto.HorariosResponse;
import com.bloodlink.api.agendamento.adapter.in.web.dto.SolicitarAgendamentoRequest;
import com.bloodlink.api.agendamento.application.usecase.AgendamentoUseCase;
import com.bloodlink.api.agendamento.application.usecase.ConsultarHorarioUseCase;
import com.bloodlink.api.agendamento.domain.Agendamento;
import com.bloodlink.api.agendamento.domain.OrigemAgendamento;
import com.bloodlink.api.agendamento.domain.SolicitarAgendamentoComando;
import com.bloodlink.common.seguranca.JwtAuthenticationFilter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agendamentos/me")
public class AgendamentoMeController {

    private final AgendamentoUseCase agendamentoUseCase;
    private final ConsultarHorarioUseCase horarioUseCase;

    public AgendamentoMeController(AgendamentoUseCase agendamentoUseCase,
                                   ConsultarHorarioUseCase horariosUseCase) {
        this.agendamentoUseCase = agendamentoUseCase;
        this.horarioUseCase = horariosUseCase;
    }

    @GetMapping("/horarios")
    public ResponseEntity<HorariosResponse> horarios(
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(HorariosResponse.de(horarioUseCase.consultar(data)));
    }

    @PostMapping
    public ResponseEntity<AgendamentoResponse> solicitar(
            @RequestHeader(JwtAuthenticationFilter.HEADER_DOADOR) UUID doadorId,
            @RequestBody SolicitarAgendamentoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        Agendamento agendamentoCriado = agendamentoUseCase.solicitar(new SolicitarAgendamentoComando(
                doadorId, request.data(), request.hora(), request.observacao(), OrigemAgendamento.DOADOR, doadorId));
        return ResponseEntity.status(HttpStatus.CREATED).body(AgendamentoResponse.de(agendamentoCriado));
    }

    @GetMapping
    public ResponseEntity<List<AgendamentoResponse>> meusAgendamentos(
            @RequestHeader(JwtAuthenticationFilter.HEADER_DOADOR) UUID doadorId) {
        return ResponseEntity.ok(agendamentoUseCase.listarDoDoador(doadorId).stream()
                .map(AgendamentoResponse::de).toList());
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<AgendamentoResponse> cancelar (
            @RequestHeader(JwtAuthenticationFilter.HEADER_DOADOR) UUID doadorId,
            @PathVariable UUID id,
            @RequestBody(required = false) CancelarAgendamentoRequest request) {
        String motivo = request == null ? null : request.motivo();
        return ResponseEntity.ok(AgendamentoResponse.de(agendamentoUseCase.cancelarDoDoador(doadorId, id, motivo)));
    }
}