package com.bloodlink.api.agendamento.application.service;

import com.bloodlink.api.agendamento.application.port.out.AgendamentoEventosPort;
import com.bloodlink.api.agendamento.application.port.out.AgendamentoEventosPort.DadosAgendamentoEvento;
import com.bloodlink.api.agendamento.application.port.out.CentroConsultaPort;
import com.bloodlink.api.agendamento.application.port.out.LembreteRepositoryPort;
import com.bloodlink.api.agendamento.application.port.out.LembreteRepositoryPort.AgendamentoParaLembrete;
import com.bloodlink.api.agendamento.application.usecase.LembreteDoacaoUseCase;
import com.bloodlink.api.agendamento.domain.SituacaoLembrete;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LembreteDoacaoService implements LembreteDoacaoUseCase {

    private static final Logger log = LoggerFactory.getLogger(LembreteDoacaoService.class);

    private final LembreteRepositoryPort lembretes;
    private final AgendamentoEventosPort eventos;
    private final CentroConsultaPort centros;
    private final Clock clock;

    public LembreteDoacaoService(LembreteRepositoryPort lembretes, AgendamentoEventosPort eventos,
                                 CentroConsultaPort centros, Clock clock) {
        this.lembretes = lembretes;
        this.eventos = eventos;
        this.centros = centros;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ResultadoLembretes enviarLembretesDeHoje() {
        LocalDate hoje = LocalDate.now(clock);
        try {
            List<AgendamentoParaLembrete> pendentes = lembretes.reservarPendentes(hoje);
            if (pendentes.isEmpty()) {
                return new ResultadoLembretes(0, 0, 0);
            }
            String centroNome = centros.nomeDoCentroAtual();
            Contagem contagem = new Contagem();
            for (AgendamentoParaLembrete a : pendentes) {
                contagem.registrar(processar(a, centroNome));
            }
            ResultadoLembretes resultado = contagem.resultado();
            log.info("Lembretes de {}: {} enviados, {} sem e-mail, {} com falha",
                    hoje, resultado.enviados(), resultado.semEmail(), resultado.falhas());
            return resultado;
        } catch (RuntimeException e) {
            log.error("Falha ao enviar os lembretes de {} - causa: {}", hoje, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException("enviar os lembretes");
        }
    }

    /**
     * Decide o destino do lembrete de um agendamento. O marcador "lembrete enviado" só é gravado
     * depois de publicar: se a publicação falhar, o agendamento continua pendente para a próxima execução.
     */
    private SituacaoLembrete processar(AgendamentoParaLembrete a, String centroNome) {
        if (a.doadorEmail() == null || a.doadorEmail().isBlank()) {
            return SituacaoLembrete.SEM_EMAIL;
        }
        boolean publicado = eventos.lembrete(new DadosAgendamentoEvento(
                a.agendamentoId(), a.doadorId(), a.doadorNome(), a.doadorEmail(), centroNome,
                LocalDateTime.of(a.data(), a.hora())));
        if (!publicado) {
            return SituacaoLembrete.FALHA;
        }
        lembretes.marcarEnviado(a.agendamentoId(), clock.instant());
        return SituacaoLembrete.ENVIADO;
    }
}


