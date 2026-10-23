package com.bloodlink.api.doador.adapter.in.web;

import com.bloodlink.api.doador.adapter.in.web.dto.FichaPreDoacaoRequest;
import com.bloodlink.api.doador.adapter.in.web.dto.FichaPreDoacaoResponse;
import com.bloodlink.api.doador.application.port.in.FichaPreDoacaoUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/doadores/{id}/ficha")
public class FichaPreDoacaoController {

    private final FichaPreDoacaoUseCase useCase;

    public FichaPreDoacaoController(FichaPreDoacaoUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public ResponseEntity<FichaPreDoacaoResponse> consultar(@PathVariable UUID id) {
        return ResponseEntity.ok(FichaPreDoacaoResponse.de(useCase.consultar(id)));
    }

    @PutMapping
    public ResponseEntity<FichaPreDoacaoResponse> salvar(@PathVariable UUID id,
                                                         @RequestBody FichaPreDoacaoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("O corpo da requisição é obrigatório");
        }
        return ResponseEntity.ok(FichaPreDoacaoResponse.de(useCase.salvar(request.paraComando(id))));
    }
}

