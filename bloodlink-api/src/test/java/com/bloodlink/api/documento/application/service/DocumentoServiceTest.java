package com.bloodlink.api.documento.application.service;

import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.documento.application.port.out.AnalisadorArquivoPort;
import com.bloodlink.api.documento.application.port.out.ArmazenamentoArquivoPort;
import com.bloodlink.api.documento.application.port.out.DocumentoRepositoryPort;
import com.bloodlink.api.documento.application.port.out.EnviarDocumentoComando;
import com.bloodlink.api.documento.application.port.out.FiltroDocumento;
import com.bloodlink.api.documento.application.port.out.ResultadoAnalise;
import com.bloodlink.api.documento.domain.ArquivoBaixado;
import com.bloodlink.api.documento.domain.CategoriaDocumento;
import com.bloodlink.api.documento.domain.Documento;
import com.bloodlink.api.documento.domain.DocumentoComDoador;
import com.bloodlink.api.documento.exception.OperacaoDocumentoException;
import com.bloodlink.auth.shared.exceptions.RecursoNaoEncontradoException;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;
import com.bloodlink.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T15:00:00Z");
    private static final UUID DOADOR = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();
    private static final String SCHEMA = "centro_95b82d103312";

    @Mock
    private DocumentoRepositoryPort documentos;
    @Mock
    private ArmazenamentoArquivoPort armazenamento;
    @Mock
    private DoadorRepositoryPort doadores;
    @Mock
    private AnalisadorArquivoPort analisador;

    private DocumentoService service;

    @BeforeEach
    void preparar() {
        TenantContext.set(SCHEMA);
        service = new DocumentoService(documentos, armazenamento, doadores, analisador,
                Clock.fixed(AGORA, ZoneOffset.UTC));
        lenient().when(analisador.analisar(any())).thenReturn(ResultadoAnalise.LIMPO);
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
    }

    private EnviarDocumentoComando doEquipe(UUID doador, String categoria, byte[] conteudo) {
        return new EnviarDocumentoComando(doador, categoria, "exame.pdf", conteudo, USUARIO, "ENFERMEIRO", false);
    }

    private EnviarDocumentoComando doDoador(String categoria, byte[] conteudo) {
        return new EnviarDocumentoComando(DOADOR, categoria, "meu exame.pdf", conteudo, USUARIO, "DOADOR", true);
    }

    private Documento documentoDe(UUID doador) {
        return new Documento(UUID.randomUUID(), doador, CategoriaDocumento.EXAME, "exame.pdf",
                "application/pdf", 10, SCHEMA + "/x/y.pdf", USUARIO, "ENFERMEIRO", AGORA);
    }

    private void doadorExiste() {
        when(doadores.buscarPorId(DOADOR)).thenReturn(Optional.of(mock(Doador.class)));
    }

    // ---------- enviar ----------

    @Test
    @DisplayName("envio da equipe: grava o arquivo com chave gerada no servidor e depois os metadados")
    void enviarDaEquipe() {
        doadorExiste();
        when(documentos.salvar(any(Documento.class))).thenAnswer(i -> i.getArgument(0));
        byte[] pdf = FormatoArquivoTest.pdf();

        Documento salvo = service.enviar(doEquipe(DOADOR, "exame", pdf));

        assertThat(salvo.categoria()).isEqualTo(CategoriaDocumento.EXAME);
        assertThat(salvo.nomeArquivo()).isEqualTo("exame.pdf");
        assertThat(salvo.tipoConteudo()).isEqualTo("application/pdf");
        assertThat(salvo.tamanhoBytes()).isEqualTo(pdf.length);
        assertThat(salvo.enviadoEm()).isEqualTo(AGORA);
        assertThat(salvo.enviadoPorPapel()).isEqualTo("ENFERMEIRO");
        assertThat(salvo.chaveArmazenamento())
                .startsWith(SCHEMA + "/" + DOADOR + "/")
                .endsWith(salvo.id() + ".pdf");
        verify(armazenamento).gravar(salvo.chaveArmazenamento(), pdf, "application/pdf");
        verify(documentos, never()).contarPorDoador(any());   // cota só vale para o doador
    }

    @Test
    @DisplayName("documento sem doador (interno) vai para a pasta 'geral' e dispensa consulta de doador")
    void enviarInterno() {
        when(documentos.salvar(any(Documento.class))).thenAnswer(i -> i.getArgument(0));

        Documento salvo = service.enviar(doEquipe(null, "triagem", FormatoArquivoTest.png()));

        assertThat(salvo.chaveArmazenamento()).startsWith(SCHEMA + "/geral/").endsWith(".png");
        verifyNoInteractions(doadores);
    }

    @Test
    @DisplayName("o nome do arquivo é limpo e a extensão vem do formato real, não do nome enviado")
    void nomeEExtensaoSeguros() {
        when(documentos.salvar(any(Documento.class))).thenAnswer(i -> i.getArgument(0));
        EnviarDocumentoComando cmd = new EnviarDocumentoComando(null, "TRIAGEM", "../../virus.exe",
                FormatoArquivoTest.jpeg(), USUARIO, "ADMIN_CENTRO", false);

        Documento salvo = service.enviar(cmd);

        assertThat(salvo.nomeArquivo()).isEqualTo("virus.jpg");
        assertThat(salvo.tipoConteudo()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("envio do próprio doador: respeita a cota e grava")
    void enviarDoDoador() {
        doadorExiste();
        when(documentos.contarPorDoador(DOADOR)).thenReturn(19L);
        when(documentos.salvar(any(Documento.class))).thenAnswer(i -> i.getArgument(0));

        Documento salvo = service.enviar(doDoador("termo", FormatoArquivoTest.pdf()));

        assertThat(salvo.categoria()).isEqualTo(CategoriaDocumento.TERMO);
        assertThat(salvo.enviadoPorPapel()).isEqualTo("DOADOR");
    }

    @Test
    @DisplayName("doador com 20 documentos não envia mais, e nada é gravado")
    void cotaEsgotada() {
        doadorExiste();
        when(documentos.contarPorDoador(DOADOR)).thenReturn(20L);

        assertThatThrownBy(() -> service.enviar(doDoador("EXAME", FormatoArquivoTest.pdf())))
                .isInstanceOf(RegraDeNegocioException.class);
        verifyNoInteractions(armazenamento);
        verify(documentos, never()).salvar(any());
    }

    @Test
    @DisplayName("doador só envia EXAME ou TERMO")
    void doadorNaoEnviaTriagemNemCertificado() {
        for (String categoria : new String[]{"TRIAGEM", "CERTIFICADO"}) {
            assertThatThrownBy(() -> service.enviar(doDoador(categoria, FormatoArquivoTest.pdf())))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(armazenamento);
    }

    @Test
    @DisplayName("envio do doador sem id de doador é recusado")
    void doadorSemId() {
        EnviarDocumentoComando cmd = new EnviarDocumentoComando(null, "EXAME", "a.pdf",
                FormatoArquivoTest.pdf(), USUARIO, "DOADOR", true);

        assertThatThrownBy(() -> service.enviar(cmd)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejeita comando nulo, arquivo vazio ou nulo")
    void rejeitaSemArquivo() {
        assertThatThrownBy(() -> service.enviar(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.enviar(doEquipe(DOADOR, "EXAME", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.enviar(doEquipe(DOADOR, "EXAME", new byte[0])))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(armazenamento);
    }

    @Test
    @DisplayName("rejeita arquivo acima de 10 MB e aceita exatamente 10 MB")
    void limiteDeTamanho() {
        byte[] exato = new byte[DocumentoService.TAMANHO_MAXIMO_BYTES];
        System.arraycopy(FormatoArquivoTest.pdf(), 0, exato, 0, 8);
        byte[] grande = new byte[DocumentoService.TAMANHO_MAXIMO_BYTES + 1];
        System.arraycopy(FormatoArquivoTest.pdf(), 0, grande, 0, 8);
        when(documentos.salvar(any(Documento.class))).thenAnswer(i -> i.getArgument(0));

        assertThatThrownBy(() -> service.enviar(doEquipe(null, "TRIAGEM", grande)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.enviar(doEquipe(null, "TRIAGEM", exato)).tamanhoBytes())
                .isEqualTo(DocumentoService.TAMANHO_MAXIMO_BYTES);
    }

    @Test
    @DisplayName("rejeita formato inválido, categoria inválida e envio sem identificar o remetente")
    void rejeitaDadosInvalidos() {
        assertThatThrownBy(() -> service.enviar(doEquipe(null, "EXAME", "texto".getBytes())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.enviar(doEquipe(null, "OUTRA", FormatoArquivoTest.pdf())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.enviar(doEquipe(null, null, FormatoArquivoTest.pdf())))
                .isInstanceOf(IllegalArgumentException.class);
        EnviarDocumentoComando semRemetente = new EnviarDocumentoComando(null, "EXAME", "a.pdf",
                FormatoArquivoTest.pdf(), null, "ENFERMEIRO", false);
        assertThatThrownBy(() -> service.enviar(semRemetente)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(armazenamento);
    }

    @Test
    @DisplayName("doador inexistente: 'não encontrado' e nada gravado")
    void doadorInexistente() {
        when(doadores.buscarPorId(DOADOR)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enviar(doEquipe(DOADOR, "EXAME", FormatoArquivoTest.pdf())))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(armazenamento);
    }

    @Test
    @DisplayName("falha ao consultar o doador vira OperacaoDocumentoException")
    void falhaAoConsultarDoador() {
        when(doadores.buscarPorId(DOADOR)).thenThrow(new RuntimeException("banco fora"));

        assertThatThrownBy(() -> service.enviar(doEquipe(DOADOR, "EXAME", FormatoArquivoTest.pdf())))
                .isInstanceOf(OperacaoDocumentoException.class);
        verifyNoInteractions(armazenamento);
    }

    @Test
    @DisplayName("falha ao contar documentos vira OperacaoDocumentoException")
    void falhaAoContar() {
        doadorExiste();
        when(documentos.contarPorDoador(DOADOR)).thenThrow(new RuntimeException("banco fora"));

        assertThatThrownBy(() -> service.enviar(doDoador("EXAME", FormatoArquivoTest.pdf())))
                .isInstanceOf(OperacaoDocumentoException.class);
        verifyNoInteractions(armazenamento);
    }

    @Test
    @DisplayName("falha ao gravar o arquivo: não grava metadados")
    void falhaAoGravarArquivo() {
        doadorExiste();
        doThrow(new RuntimeException("s3 fora")).when(armazenamento).gravar(anyString(), any(), anyString());

        assertThatThrownBy(() -> service.enviar(doEquipe(DOADOR, "EXAME", FormatoArquivoTest.pdf())))
                .isInstanceOf(OperacaoDocumentoException.class);
        verify(documentos, never()).salvar(any());
    }

    @Test
    @DisplayName("falha ao gravar os metadados: remove o arquivo órfão do armazenamento")
    void removeOrfaoQuandoBancoFalha() {
        doadorExiste();
        when(documentos.salvar(any(Documento.class))).thenThrow(new RuntimeException("banco fora"));

        assertThatThrownBy(() -> service.enviar(doEquipe(DOADOR, "EXAME", FormatoArquivoTest.pdf())))
                .isInstanceOf(OperacaoDocumentoException.class);

        ArgumentCaptor<String> gravada = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> removida = ArgumentCaptor.forClass(String.class);
        verify(armazenamento).gravar(gravada.capture(), any(), anyString());
        verify(armazenamento).remover(removida.capture());
        assertThat(removida.getValue()).isEqualTo(gravada.getValue());
    }

    @Test
    @DisplayName("se a remoção do órfão também falhar, o erro original é o que sobe")
    void remocaoDoOrfaoFalha() {
        doadorExiste();
        when(documentos.salvar(any(Documento.class))).thenThrow(new RuntimeException("banco fora"));
        doThrow(new RuntimeException("s3 fora")).when(armazenamento).remover(anyString());

        assertThatThrownBy(() -> service.enviar(doEquipe(DOADOR, "EXAME", FormatoArquivoTest.pdf())))
                .isInstanceOf(OperacaoDocumentoException.class)
                .hasMessage("Não foi possível enviar o documento no momento");
    }

    @Test
    @DisplayName("arquivo infectado é recusado e nada é gravado")
    void arquivoInfectado() {
        doadorExiste();
        when(analisador.analisar(any())).thenReturn(ResultadoAnalise.INFECTADO);

        assertThatThrownBy(() -> service.enviar(doEquipe(DOADOR, "EXAME", FormatoArquivoTest.pdf())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O arquivo foi recusado pela verificação de segurança");
        verifyNoInteractions(armazenamento);
        verify(documentos, never()).salvar(any());
    }

    @Test
    @DisplayName("verificador indisponível: o arquivo não é guardado sem verificação")
    void verificadorIndisponivel() {
        doadorExiste();
        when(analisador.analisar(any())).thenReturn(ResultadoAnalise.INDISPONIVEL);

        assertThatThrownBy(() -> service.enviar(doEquipe(DOADOR, "EXAME", FormatoArquivoTest.pdf())))
                .isInstanceOf(OperacaoDocumentoException.class);
        verifyNoInteractions(armazenamento);
    }

    @Test
    @DisplayName("a verificação de vírus só roda depois das validações baratas (formato, categoria, doador, cota)")
    void virusSoDepoisDasValidacoes() {
        assertThatThrownBy(() -> service.enviar(doEquipe(null, "EXAME", "texto".getBytes())))
                .isInstanceOf(IllegalArgumentException.class);
        when(doadores.buscarPorId(DOADOR)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.enviar(doEquipe(DOADOR, "EXAME", FormatoArquivoTest.pdf())))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verifyNoInteractions(analisador);
    }

    // ---------- listar ----------

    @Test
    @DisplayName("listar junta o nome do doador numa única consulta")
    void listarComNomes() {
        Doador doador = mock(Doador.class);
        when(doador.nome()).thenReturn("Maria Souza");
        Documento comDoador = documentoDe(DOADOR);
        Documento interno = documentoDe(null);
        when(documentos.listar(any(FiltroDocumento.class), eq(50))).thenReturn(List.of(comDoador, interno));
        when(doadores.buscarPorIds(Set.of(DOADOR))).thenReturn(Map.of(DOADOR, doador));

        List<DocumentoComDoador> lista = service.listar(null, 50);

        assertThat(lista).extracting(DocumentoComDoador::doadorNome).containsExactly("Maria Souza", null);
    }

    @Test
    @DisplayName("listar sem nenhum doador não consulta doadores; doador removido fica com nome nulo")
    void listarSemDoadores() {
        when(documentos.listar(any(FiltroDocumento.class), anyInt())).thenReturn(List.of(documentoDe(null)));
        assertThat(service.listar(null, 10)).hasSize(1);
        verifyNoInteractions(doadores);

        when(documentos.listar(any(FiltroDocumento.class), anyInt())).thenReturn(List.of(documentoDe(DOADOR)));
        when(doadores.buscarPorIds(any())).thenReturn(Map.of());
        assertThat(service.listar(null, 10).get(0).doadorNome()).isNull();
    }

    @Test
    @DisplayName("listar com filtro nulo usa filtro vazio")
    void listarFiltroNulo() {
        when(documentos.listar(any(FiltroDocumento.class), anyInt())).thenReturn(List.of());

        service.listar(null, 10);

        ArgumentCaptor<FiltroDocumento> filtro = ArgumentCaptor.forClass(FiltroDocumento.class);
        verify(documentos).listar(filtro.capture(), eq(10));
        assertThat(filtro.getValue()).isEqualTo(new FiltroDocumento(null, null, null));
    }

    @Test
    @DisplayName("listar rejeita limite fora de 1 a 200 e converte falha do banco")
    void listarLimiteEFalha() {
        assertThatThrownBy(() -> service.listar(null, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listar(null, 201)).isInstanceOf(IllegalArgumentException.class);

        when(documentos.listar(any(FiltroDocumento.class), anyInt())).thenThrow(new RuntimeException("falha"));
        assertThatThrownBy(() -> service.listar(null, 10)).isInstanceOf(OperacaoDocumentoException.class);
    }

    @Test
    @DisplayName("listarDoDoador filtra pelo doador, limita a 20 e não consulta nomes")
    void listarDoDoador() {
        when(documentos.listar(any(FiltroDocumento.class), anyInt())).thenReturn(List.of(documentoDe(DOADOR)));

        List<DocumentoComDoador> lista = service.listarDoDoador(DOADOR);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).doadorNome()).isNull();
        verify(documentos).listar(new FiltroDocumento(null, DOADOR, null), DocumentoService.LIMITE_POR_DOADOR);
        verifyNoInteractions(doadores);
    }

    @Test
    @DisplayName("listarDoDoador rejeita id nulo e converte falha do banco")
    void listarDoDoadorErros() {
        assertThatThrownBy(() -> service.listarDoDoador(null)).isInstanceOf(IllegalArgumentException.class);

        when(documentos.listar(any(FiltroDocumento.class), anyInt())).thenThrow(new RuntimeException("falha"));
        assertThatThrownBy(() -> service.listarDoDoador(DOADOR)).isInstanceOf(OperacaoDocumentoException.class);
    }

    // ---------- baixar ----------

    @Test
    @DisplayName("baixar devolve nome, tipo e bytes")
    void baixar() {
        Documento d = documentoDe(DOADOR);
        byte[] bytes = FormatoArquivoTest.pdf();
        when(documentos.buscarPorId(d.id())).thenReturn(Optional.of(d));
        when(armazenamento.ler(d.chaveArmazenamento())).thenReturn(bytes);

        ArquivoBaixado arquivo = service.baixar(d.id(), null);

        assertThat(arquivo.nome()).isEqualTo("exame.pdf");
        assertThat(arquivo.tipoConteudo()).isEqualTo("application/pdf");
        assertThat(arquivo.bytes()).isEqualTo(bytes);
    }

    @Test
    @DisplayName("doador baixa o próprio documento")
    void baixarProprio() {
        Documento d = documentoDe(DOADOR);
        when(documentos.buscarPorId(d.id())).thenReturn(Optional.of(d));
        when(armazenamento.ler(anyString())).thenReturn(FormatoArquivoTest.pdf());

        assertThat(service.baixar(d.id(), DOADOR)).isNotNull();
    }

    @Test
    @DisplayName("documento de outro doador responde 'não encontrado' e não lê o arquivo")
    void baixarDeOutroDoador() {
        Documento d = documentoDe(UUID.randomUUID());
        when(documentos.buscarPorId(d.id())).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.baixar(d.id(), DOADOR)).isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(armazenamento);
    }

    @Test
    @DisplayName("documento interno (sem doador) também é 'não encontrado' para o doador")
    void baixarInternoComoDoador() {
        Documento d = documentoDe(null);
        when(documentos.buscarPorId(d.id())).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.baixar(d.id(), DOADOR)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("id inexistente e id nulo")
    void baixarInexistente() {
        UUID id = UUID.randomUUID();
        when(documentos.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.baixar(id, null)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.baixar(null, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("falha no banco ou no armazenamento vira OperacaoDocumentoException")
    void baixarComFalhas() {
        UUID id = UUID.randomUUID();
        when(documentos.buscarPorId(id)).thenThrow(new RuntimeException("banco fora"));
        assertThatThrownBy(() -> service.baixar(id, null)).isInstanceOf(OperacaoDocumentoException.class);

        Documento d = documentoDe(DOADOR);
        UUID id2 = d.id();
        when(documentos.buscarPorId(id2)).thenReturn(Optional.of(d));
        when(armazenamento.ler(anyString())).thenThrow(new RuntimeException("s3 fora"));
        assertThatThrownBy(() -> service.baixar(id2, null)).isInstanceOf(OperacaoDocumentoException.class);
    }

    @Test
    @DisplayName("toString dos objetos não expõe nome de arquivo nem conteúdo")
    void toStringsSeguros() {
        Documento d = documentoDe(DOADOR);

        assertThat(d.toString()).doesNotContain("exame.pdf");
        assertThat(new ArquivoBaixado("exame.pdf", "application/pdf", new byte[]{1, 2}).toString())
                .doesNotContain("exame.pdf");
        assertThat(doDoador("EXAME", FormatoArquivoTest.pdf()).toString()).doesNotContain("meu exame");
    }
}
