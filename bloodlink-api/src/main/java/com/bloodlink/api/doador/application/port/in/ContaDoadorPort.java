package com.bloodlink.api.doador.application.port.in;

import java.util.UUID;

public interface ContaDoadorPort {
    /** Pede ao Auth a conta de acesso do doador (pendente de ativação) e o envio do link. */
    void solicitarConta(SolicitacaoContaDoador solicitacao);

    record SolicitacaoContaDoador(String nome, String email, String centroId, UUID doadorId) {
        /** Nome e e-mail são dados pessoais: só o id do doador vai para o log. */
        @Override
        public String toString() {
            return "SolicitacaoContaDoador[doadorId=" + doadorId + "]";
        }
    }
}
