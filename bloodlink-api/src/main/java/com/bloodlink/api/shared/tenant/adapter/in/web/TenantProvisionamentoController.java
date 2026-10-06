package com.bloodlink.api.shared.tenant.adapter.in.web;

import com.bloodlink.api.shared.tenant.application.port.out.CancelarProvisionamentoUseCase;
import com.bloodlink.api.shared.tenant.application.port.out.ConsultarProvisionamentoUseCase;
import com.bloodlink.api.shared.tenant.application.port.out.RetomarProvisionamentoUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenants/provisionamentos")
public class TenantProvisionamentoController {

    private final RetomarProvisionamentoUseCase retomarProvisionamentoUseCase;
    private final CancelarProvisionamentoUseCase cancelarProvisionamentoUseCase;
    private final ConsultarProvisionamentoUseCase consultarProvisionamentoUseCase;

    public TenantProvisionamentoController(RetomarProvisionamentoUseCase retomarProvisionamentoUseCase,
                                           CancelarProvisionamentoUseCase cancelarProvisionamentoUseCase,
                                           ConsultarProvisionamentoUseCase consultarProvisionamentoUseCase) {
        this.retomarProvisionamentoUseCase = retomarProvisionamentoUseCase;
        this.cancelarProvisionamentoUseCase = cancelarProvisionamentoUseCase;
        this.consultarProvisionamentoUseCase = consultarProvisionamentoUseCase;
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
}
