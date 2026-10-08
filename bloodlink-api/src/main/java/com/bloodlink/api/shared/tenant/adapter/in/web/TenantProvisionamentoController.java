package com.bloodlink.api.shared.tenant.adapter.in.web;

import com.bloodlink.api.shared.tenant.application.port.out.CancelarProvisionamentoUseCase;
import com.bloodlink.api.shared.tenant.application.port.out.ConsultarProvisionamentoUseCase;
import com.bloodlink.api.shared.tenant.application.port.out.ListarProvisionamentosUseCase;
import com.bloodlink.api.shared.tenant.application.port.out.RetomarProvisionamentoUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants/provisionamentos")
public class TenantProvisionamentoController {

    private final RetomarProvisionamentoUseCase retomarProvisionamentoUseCase;
    private final CancelarProvisionamentoUseCase cancelarProvisionamentoUseCase;
    private final ConsultarProvisionamentoUseCase consultarProvisionamentoUseCase;
    private final ListarProvisionamentosUseCase listarProvisionamentosUseCase;

    public TenantProvisionamentoController(RetomarProvisionamentoUseCase retomarProvisionamentoUseCase,
                                           CancelarProvisionamentoUseCase cancelarProvisionamentoUseCase,
                                           ConsultarProvisionamentoUseCase consultarProvisionamentoUseCase,
                                           ListarProvisionamentosUseCase listarProvisionamentosUseCase) {
        this.retomarProvisionamentoUseCase = retomarProvisionamentoUseCase;
        this.cancelarProvisionamentoUseCase = cancelarProvisionamentoUseCase;
        this.consultarProvisionamentoUseCase = consultarProvisionamentoUseCase;
        this.listarProvisionamentosUseCase = listarProvisionamentosUseCase;
    }

    @GetMapping("/{schema}")
    public ResponseEntity<ProvisionamentoResponse> consultar(@PathVariable String schema) {
        return ResponseEntity.ok(ProvisionamentoResponse.de(consultarProvisionamentoUseCase.consultar(schema)));
    }

    @PatchMapping("/{schema}/retomar")
    public ResponseEntity<Void> retomar(@PathVariable String schema) {
        retomarProvisionamentoUseCase.retomar(schema);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{schema}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable String schema) {
        cancelarProvisionamentoUseCase.cancelar(schema);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<PaginaProvisionamentosResponse> listar (
            @RequestParam(required = false)String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size ) {
                return ResponseEntity.ok(PaginaProvisionamentosResponse.de(
                        listarProvisionamentosUseCase.listar(status, page, size)));
    }
}
