package com.bloodlink.common.messaging;

public final class RabbitConstants {

    public static final String EXCHANGE_NOTIFICACOES = "bloodlink.notificacoes";
    public static final String ROUTING_KEY_AGENDAMENTO_CONFIRMADO = "agendamento.confirmado";
    public static final String ROUTING_KEY_LEMBRETE_DOACAO = "lembrete.doacao";
    public static final String ROUTING_KEY_ESTOQUE_CRITICO = "estoque.critico";

    public static final String QUEUE_AGENDAMENTO_CONFIRMADO = "notificacao.agendamento-confirmado";
    public static final String QUEUE_AGENDAMENTO_CONFIRMADO_DLQ = QUEUE_AGENDAMENTO_CONFIRMADO + ".dlq";
    public static final String QUEUE_LEMBRETE_DOACAO = "notificacao.lembrete-doacao";
    public static final String QUEUE_LEMBRETE_DOACAO_DLQ = QUEUE_LEMBRETE_DOACAO + ".dlq";
    public static final String QUEUE_ESTOQUE_CRITICO = "notificacao.estoque-critico";
    public static final String QUEUE_ESTOQUE_CRITICO_DLQ = QUEUE_ESTOQUE_CRITICO + ".dlq";

    public static final String EXCHANGE_CENTRO_REGISTRADO = "bloodlink.centro.registrado";
    public static final String QUEUE_CENTRO_REGISTRADO_API = "api.centro-registrado";
    public static final String QUEUE_CENTRO_REGISTRADO_API_DLQ = QUEUE_CENTRO_REGISTRADO_API + ".dlq";

    private RabbitConstants() {}
}