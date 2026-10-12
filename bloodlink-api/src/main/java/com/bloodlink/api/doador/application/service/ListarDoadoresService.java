package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.application.port.in.ListarDoadoresUseCase;
import com.bloodlink.api.doador.application.port.out.FiltroDoador;
import com.bloodlink.api.doador.doadorException.ListagemDoadoresException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.doador.domain.TipoSanguineo;
import com.bloodlink.api.shared.Conversor.ConversorEnum;
import com.bloodlink.api.shared.Pagina.Pagina;
import com.bloodlink.api.shared.Pagina.Paginacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ListarDoadoresService implements ListarDoadoresUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListarDoadoresService.class);
    private static final int TAMANHO_MAXIMO_NOME = 120;

    private final DoadorRepositoryPort repository;

    public ListarDoadoresService(DoadorRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    public Pagina<Doador> listar(String nome, String tipoSanguineo, Boolean ativo, int pagina, int tamanho) {
        Paginacao.validar(pagina, tamanho);
        FiltroDoador filtro = montarFiltro(nome, tipoSanguineo, ativo);

        try {
            Pagina<Doador> resultado = repository.listar(filtro, pagina, tamanho);
            log.debug("Listagem de doadores: página {}, tamanho {}, {} no total",
                    pagina, tamanho, resultado.totalElementos()); // não loga o filtro de nome (dado pessoal)
            return resultado;
        } catch (RuntimeException e) {
            log.error("Falha ao listar doadores (página {}, tamanho {})", pagina, tamanho, e);
            throw new ListagemDoadoresException(e);
        }
    }

    private FiltroDoador montarFiltro(String nome, String tipoSanguineo, Boolean ativo) {
        TipoSanguineo tipo = ConversorEnum.opcional(TipoSanguineo.class, tipoSanguineo, "tipoSanguineo");
        String nomeFiltro = (nome == null || nome.isBlank()) ? null : nome.trim();
        if (nomeFiltro != null && nomeFiltro.length() > TAMANHO_MAXIMO_NOME) {
            throw new IllegalArgumentException(
                    "O filtro 'nome' deve ter no máximo " + TAMANHO_MAXIMO_NOME + " caracteres");
        }
        return new FiltroDoador(nomeFiltro, tipo, ativo);
    }
}
