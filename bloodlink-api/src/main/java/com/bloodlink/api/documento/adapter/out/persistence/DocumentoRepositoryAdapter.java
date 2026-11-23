package com.bloodlink.api.documento.adapter.out.persistence;

import com.bloodlink.api.doador.adapter.out.persistence.DoadorJpaEntity;
import com.bloodlink.api.documento.application.port.out.DocumentoRepositoryPort;
import com.bloodlink.api.documento.application.port.out.FiltroDocumento;
import com.bloodlink.api.documento.domain.Documento;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Component
public class DocumentoRepositoryAdapter implements DocumentoRepositoryPort {

    private final DocumentoJpaRepository jpa;

    public DocumentoRepositoryAdapter(DocumentoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Documento salvar(Documento documento) {
        return jpa.save(DocumentoJpaEntity.de(documento)).paraDominio();
    }

    @Override
    public Optional<Documento> buscarPorId(UUID id) {
        return jpa.findById(id).map(DocumentoJpaEntity::paraDominio);
    }

    @Override
    public List<Documento> listar(FiltroDocumento filtro, int limite) {
        Specification<DocumentoJpaEntity> spec = (root, query, cb) -> cb.conjunction();
        if (filtro.categoria() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("categoria"), filtro.categoria()));
        }
        if (filtro.doadorId() != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("doadorId"), filtro.doadorId()));
        }
        if (filtro.busca() != null && !filtro.busca().isBlank()) {
            String padrao = "%" + escapar(filtro.busca().trim().toLowerCase(Locale.ROOT)) + "%";
            // procura no nome do arquivo OU no nome do doador dono do documento
            spec = spec.and((root, query, cb) -> {
                var doadores = query.subquery(UUID.class);
                var doador = doadores.from(DoadorJpaEntity.class);
                doadores.select(doador.<UUID>get("id"))
                        .where(cb.like(cb.lower(doador.get("nome")), padrao, '\\'));
                return cb.or(
                        cb.like(cb.lower(root.get("nomeArquivo")), padrao, '\\'),
                        root.get("doadorId").in(doadores));
            });
        }
        return jpa.findAll(spec, PageRequest.of(0, limite, Sort.by(Sort.Direction.DESC, "enviadoEm")))
                .stream().map(DocumentoJpaEntity::paraDominio).toList();
    }

    @Override
    public long contarPorDoador(UUID doadorId) {
        return jpa.countByDoadorId(doadorId);
    }

    /** % e _ digitados pelo usuário valem como texto, não como curinga. */
    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
