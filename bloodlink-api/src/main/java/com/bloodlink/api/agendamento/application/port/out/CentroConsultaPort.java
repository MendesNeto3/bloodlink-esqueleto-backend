package com.bloodlink.api.agendamento.application.port.out;

public interface CentroConsultaPort {

    /** Nome do hemocentro do tenant da requisição. Nunca falha: sem resposta do Centro, devolve "seu hemocentro". */
    String nomeDoCentroAtual();

    /**
     * Diz se o hemocentro aceita novos cadastros: false só quando o Centro informa que ele foi desativado
     * ou não existe. Sem resposta do Centro, devolve true: uma queda do Centro não pode travar o cadastro.
     */
    boolean aceitaNovosCadastros(String centroId);
}
