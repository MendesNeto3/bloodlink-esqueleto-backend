package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.adapter.out.persistence.DoacaoRepositoryPort;
import com.bloodlink.api.doador.adapter.out.persistence.ImpedimentoRepositoryPort;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.in.ListarDoadoresUseCase;
import com.bloodlink.api.doador.application.port.out.*;
import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.shared.Pagina.Pagina;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListarDoadorService implements ListarDoadoresUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListarDoadorService.class);
    private static final int TAMANHO_MAXIMO = 100;

    private final DoadorRepositoryPort doadores;
    private final DoacaoRepositoryPort doacoes;
    private final ImpedimentoRepositoryPort impedimentos;
    private final AvaliadorElegibilidade avaliador;

    public ListarDoadorService(DoadorRepositoryPort doadores, DoacaoRepositoryPort doacoes,
                               ImpedimentoRepositoryPort impedimentos,
                               AvaliadorElegibilidade avaliador) {
        this.doadores = doadores;
        this.doacoes = doacoes;
        this.impedimentos = impedimentos;
        this.avaliador = avaliador;
    }

    @Override
    public Pagina<DoadorResumo> listar(FiltroDoador filtro, int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException(
                    "Paginação inválida: página >= 0 e tamanho entre 1 e " + TAMANHO_MAXIMO);
        }
        try {
            Pagina<Doador> pag = doadores.listar(filtro, pagina, tamanho);
            List<UUID> ids = pag.conteudo().stream().map(Doador::id).toList();

            Map<UUID, List<LocalDate>> datas = doacoes.datasPorDoadores(ids);
            Map<UUID, List<Impedimento>> imps = impedimentos.naoEncerradosPorDoadores(ids);

            // mapear mantém página, tamanho e totais; só troca o conteúdo
            return pag.mapear(d -> montar(d,
                    datas.getOrDefault(d.id(), List.of()),
                    imps.getOrDefault(d.id(), List.of())));
        } catch (RuntimeException e) {
            log.error("Falha ao listar doadores - causa: {}", MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new OperacaoDoadorException("listar os doadores");
        }
    }

    private DoadorResumo montar(Doador d, List<LocalDate> datas, List<Impedimento> imps) {
        ResultadoElegibilidade r = avaliador.avaliar(d, datas, imps);
        LocalDate ultima = datas.isEmpty() ? null : Collections.max(datas);
        return new DoadorResumo(d.id(), d.nome(), d.tipoSanguineo(), d.ativo(),
                ultima, datas.size(), r.status(), r.elegivelEm());
    }
}