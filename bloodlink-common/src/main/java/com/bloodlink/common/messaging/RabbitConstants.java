package com.bloodlink.common.messaging;

public final class RabbitConstants {

    public static final String EXCHANGE_NOTIFICACOES = "bloodlink.notificacoes";
    public static final String ROUTING_KEY_AGENDAMENTO_CONFIRMADO = "agendamento.confirmado";
    public static final String ROUTING_KEY_LEMBRETE_DOACAO = "lembrete.doacao";
    public static final String ROUTING_KEY_AGENDAMENTO_STATUS_ATUALIZADO = "agendamento.status-atualizado";

    public static final String QUEUE_AGENDAMENTO_CONFIRMADO = "notificacao.agendamento-confirmado";
    public static final String QUEUE_LEMBRETE_DOACAO = "notificacao.lembrete-doacao";
    public static final String QUEUE_CHECKIN_AGENDAMENTO_CONFIRMADO = "checkin.agendamento-confirmado";
    public static final String QUEUE_CHECKIN_AGENDAMENTO_STATUS_ATUALIZADO = "checkin.agendamento-status-atualizado";

    public static final String EXCHANGE_CENTRO_REGISTRADO = "bloodlink.centro.registrado";
    public static final String QUEUE_CENTRO_REGISTRADO_API = "api.centro-registrado";
    public static final String QUEUE_CENTRO_REGISTRADO_ESTOQUE = "estoque.centro-registrado";
    public static final String QUEUE_CENTRO_REGISTRADO_CHECKIN = "checkin.centro-registrado";
    public static final String QUEUE_CENTRO_REGISTRADO_API_DLQ = QUEUE_CENTRO_REGISTRADO_API + ".dlq";

    private RabbitConstants() {}
}

