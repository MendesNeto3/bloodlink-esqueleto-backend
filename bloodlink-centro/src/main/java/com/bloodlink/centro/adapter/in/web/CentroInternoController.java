package com.bloodlink.centro.adapter.in.web;

import com.bloodlink.centro.adapter.in.web.dto.CentroInternoResponse;
import com.bloodlink.centro.application.port.in.CentroUseCase;
import com.bloodlink.centro.application.service.ChaveDeServicoInvalidaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Rota entre serviços (a API chama para obter o nome do hemocentro). Não é roteada pelo gateway. */
@RestController
@RequestMapping("/internal/centros")
public class CentroInternoController {

    private static final String HEADER_CHAVE = "X-Servico-Chave";

    private final CentroUseCase centroUseCase;
    private final byte[] chaveServico;

    public CentroInternoController(CentroUseCase centroUseCase,
                                   @Value("${bloodlink.servico.chave:}") String chaveServico) {
        this.centroUseCase = centroUseCase;
        this.chaveServico = chaveServico.getBytes(StandardCharsets.UTF_8);
    }

    @GetMapping("/{centroId}")
    public CentroInternoResponse buscar(@PathVariable String centroId,
                                        @RequestHeader(value = HEADER_CHAVE, required = false) String chaveInformada) {
        exigirChaveDeServico(chaveInformada);
        var centro = centroUseCase.buscarPorCentroId(centroId);
        return new CentroInternoResponse(centro.centroId(), centro.nome(), centro.ativo());
    }

    /** Sem chave configurada, a rota fica fechada para todos. Comparação em tempo constante. */
    private void exigirChaveDeServico(String informada) {
        if (chaveServico.length == 0 || informada == null
                || !MessageDigest.isEqual(chaveServico, informada.getBytes(StandardCharsets.UTF_8))) {
            throw new ChaveDeServicoInvalidaException();
        }
    }
}