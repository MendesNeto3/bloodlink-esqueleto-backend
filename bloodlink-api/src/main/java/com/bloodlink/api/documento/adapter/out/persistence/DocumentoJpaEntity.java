package com.bloodlink.api.documento.adapter.out.persistence;

import com.bloodlink.api.documento.domain.CategoriaDocumento;
import com.bloodlink.api.documento.domain.Documento;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documento")
public class DocumentoJpaEntity {

    @Id
    private UUID id;

    @Column(name = "doador_id", updatable = false)
    private UUID doadorId;

    /** A coluna "tipo" (V4) guarda a categoria. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, updatable = false, length = 50)
    private CategoriaDocumento categoria;

    @Column(name = "nome_arquivo", nullable = false, updatable = false, length = 150)
    private String nomeArquivo;

    @Column(name = "tipo_conteudo", nullable = false, updatable = false, length = 40)
    private String tipoConteudo;

    @Column(name = "tamanho_bytes", nullable = false, updatable = false)
    private long tamanhoBytes;

    @Column(name = "chave_armazenamento", nullable = false, updatable = false, length = 300)
    private String chaveArmazenamento;

    @Column(name = "enviado_por", nullable = false, updatable = false)
    private UUID enviadoPor;

    @Column(name = "enviado_por_papel", nullable = false, updatable = false, length = 20)
    private String enviadoPorPapel;

    @Column(name = "enviado_em", nullable = false, updatable = false)
    private Instant enviadoEm;

    protected DocumentoJpaEntity() {
    }

    public static DocumentoJpaEntity de(Documento d) {
        DocumentoJpaEntity e = new DocumentoJpaEntity();
        e.id = d.id();
        e.doadorId = d.doadorId();
        e.categoria = d.categoria();
        e.nomeArquivo = d.nomeArquivo();
        e.tipoConteudo = d.tipoConteudo();
        e.tamanhoBytes = d.tamanhoBytes();
        e.chaveArmazenamento = d.chaveArmazenamento();
        e.enviadoPor = d.enviadoPor();
        e.enviadoPorPapel = d.enviadoPorPapel();
        e.enviadoEm = d.enviadoEm();
        return e;
    }

    public Documento paraDominio() {
        return new Documento(id, doadorId, categoria, nomeArquivo, tipoConteudo, tamanhoBytes,
                chaveArmazenamento, enviadoPor, enviadoPorPapel, enviadoEm);
    }

    @Override
    public String toString() {
        return "DocumentoJpaEntity[id=" + id + "]";
    }
}
