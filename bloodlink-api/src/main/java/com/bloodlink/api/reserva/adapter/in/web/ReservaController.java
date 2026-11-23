package com.bloodlink.api.reserva.adapter.in.web;

import com.bloodlink.api.reserva.adapter.in.web.dto.ReservaRequest;
import com.bloodlink.api.reserva.adapter.in.web.dto.ReservaResponse;
import com.bloodlink.api.reserva.application.port.in.ReservaUseCase;
import com.bloodlink.api.reserva.application.port.out.CriarReservaComando;
import com.bloodlink.api.reserva.domain.SituacaoReserva;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaUseCase reservaUseCase;

    public ReservaController(ReservaUseCase reservaUseCase) {
        this.reservaUseCase = reservaUseCase;
    }

    @PostMapping
    public ResponseEntity<ReservaResponse> criar(@RequestBody ReservaRequest request, Authentication autenticacao) {
        if (request == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        var criada = reservaUseCase.criar(new CriarReservaComando(request.hospital(), request.tipoSanguineo(),
                request.unidades(), request.validaAte(), usuarioId(autenticacao)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservaResponse.de(criada));
    }

    @GetMapping
    public ResponseEntity<List<ReservaResponse>> listar(
            @RequestParam(required = false) SituacaoReserva situacao,
            @RequestParam(defaultValue = "100") int limite) {
        return ResponseEntity.ok(reservaUseCase.listar(situacao, limite).stream()
                .map(ReservaResponse::de).toList());
    }

    @PatchMapping("/{id}/liberar")
    public ResponseEntity<ReservaResponse> liberar(@PathVariable UUID id) {
        return ResponseEntity.ok(ReservaResponse.de(reservaUseCase.liberar(id)));
    }

    /** O nome da autenticação é o subject do token, que é o id do usuário. */
    private static UUID usuarioId(Authentication autenticacao) {
        return UUID.fromString(autenticacao.getName());
    }
}
