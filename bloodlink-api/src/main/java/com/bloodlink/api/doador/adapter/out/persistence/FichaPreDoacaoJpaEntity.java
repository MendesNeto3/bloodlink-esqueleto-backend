package com.bloodlink.api.doador.adapter.out.persistence;

import com.bloodlink.api.doador.application.service.FichaPreDoacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ficha_pre_doacao")
public class FichaPreDoacaoJpaEntity {

    @Id
    @Column(name = "doador_id")
    private UUID doadorId;

    @Column(name = "bem_de_saude", nullable = false) private boolean bemDeSaude;
    @Column(name = "dormiu_6h", nullable = false) private boolean dormiu6h;
    @Column(name = "alimentado_hidratado", nullable = false) private boolean alimentadoHidratado;
    @Column(name = "alcool_12h", nullable = false) private boolean alcool12h;
    @Column(name = "procedimento_recente", nullable = false) private boolean procedimentoRecente;
    @Column(name = "gestante_amamentando", nullable = false) private boolean gestanteAmamentando;
    @Column(name = "doenca_relevante", nullable = false) private boolean doencaRelevante;
    @Column(name = "medicamento_ou_vacina", nullable = false) private boolean medicamentoOuVacina;

    @Column(length = 1000)
    private String observacoes;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    protected FichaPreDoacaoJpaEntity() {
    }

    public static FichaPreDoacaoJpaEntity de(FichaPreDoacao f) {
        var e = new FichaPreDoacaoJpaEntity();
        e.doadorId = f.doadorId();
        e.bemDeSaude = f.bemDeSaude();
        e.dormiu6h = f.dormiu6h();
        e.alimentadoHidratado = f.alimentadoHidratado();
        e.alcool12h = f.alcool12h();
        e.procedimentoRecente = f.procedimentoRecente();
        e.gestanteAmamentando = f.gestanteAmamentando();
        e.doencaRelevante = f.doencaRelevante();
        e.medicamentoOuVacina = f.medicamentoOuVacina();
        e.observacoes = f.observacoes();
        e.atualizadaEm = f.atualizadaEm();
        return e;
    }

    public FichaPreDoacao paraDominio() {
        return new FichaPreDoacao(doadorId, bemDeSaude, dormiu6h, alimentadoHidratado, alcool12h,
                procedimentoRecente, gestanteAmamentando, doencaRelevante, medicamentoOuVacina,
                observacoes, atualizadaEm);
    }

    @Override
    public String toString() {
        return "FichaPreDoacaoJpaEntity[doadorId=" + doadorId + "]";
    }
}
