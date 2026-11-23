package com.bloodlink.api.documento.application.port.in;

import com.bloodlink.api.documento.application.port.out.EnviarDocumentoComando;
import com.bloodlink.api.documento.application.port.out.FiltroDocumento;
import com.bloodlink.api.documento.domain.ArquivoBaixado;
import com.bloodlink.api.documento.domain.Documento;
import com.bloodlink.api.documento.domain.DocumentoComDoador;

import java.util.List;
import java.util.UUID;

public interface DocumentoUseCase {

    Documento enviar(EnviarDocumentoComando comando);

    List<DocumentoComDoador> listar(FiltroDocumento filtro, int limite);

    /** Documentos do próprio doador (do mais recente para o mais antigo). */
    List<DocumentoComDoador> listarDoDoador(UUID doadorId);

    /**
     * restritoAoDoador nulo = equipe (qualquer documento). Preenchido = só se o documento for desse doador;
     * de outro doador responde "não encontrado", para não revelar que o documento existe.
     */
    ArquivoBaixado baixar(UUID id, UUID restritoAoDoador);
}
