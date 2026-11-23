package com.bloodlink.api.documento.adapter.out.antivirus;

import com.bloodlink.api.documento.application.port.out.AnalisadorArquivoPort;
import com.bloodlink.api.documento.application.port.out.ResultadoAnalise;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Padrão em desenvolvimento: sem verificação. Produção deve ligar bloodlink.antivirus.habilitado. */
@Component
@ConditionalOnProperty(name = "bloodlink.antivirus.habilitado", havingValue = "false", matchIfMissing = true)
public class AnalisadorDesabilitado implements AnalisadorArquivoPort {

    @Override
    public ResultadoAnalise analisar(byte[] conteudo) {
        return ResultadoAnalise.LIMPO;
    }
}
