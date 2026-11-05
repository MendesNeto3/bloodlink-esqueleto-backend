package com.bloodlink.api.agendamento.adapter.in.web;

import com.bloodlink.api.agendamento.adapter.in.web.dto.AgendamentoResponse;
import com.bloodlink.api.agendamento.adapter.in.web.dto.CancelarAgendamentoRequest;
import com.bloodlink.api.agendamento.application.usecase.AgendamentoUseCase;
import com.bloodlink.api.agendamento.domain.FiltroAgendamento;
import com.bloodlink.api.agendamento.domain.StatusAgendamento;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoUseCase agendamentoUseCase;

    public AgendamentoController(AgendamentoUseCase agendamentoUseCase) {
        this.agendamentoUseCase = agendamentoUseCase;
    }

    @GetMapping
    public ResponseEntity<List<AgendamentoResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
            @RequestParam(required = false) StatusAgendamento status,
            @RequestParam(required = false) UUID doadorId,
            @RequestParam(defaultValue = "100") int limite) {
        return ResponseEntity.ok(agendamentoUseCase.listar(new FiltroAgendamento(de, ate, status, doadorId), limite)
                .stream().map(AgendamentoResponse::de).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgendamentoResponse> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(AgendamentoResponse.de(agendamentoUseCase.buscar(id)));
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
}

