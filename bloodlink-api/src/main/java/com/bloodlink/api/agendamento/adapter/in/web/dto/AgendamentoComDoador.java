package com.bloodlink.api.agendamento.adapter.in.web.dto;

import com.bloodlink.api.agendamento.domain.Agendamento;

public record AgendamentoComDoador(Agendamento agendamento,
                                   String doadorNome,
                                   String doadorTelefone,
                                   String doadorTipoSanguineo) {

}
