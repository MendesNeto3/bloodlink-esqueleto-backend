package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.agendamento.application.port.out.CentroConsultaPort;
import com.bloodlink.api.doador.application.port.in.ContaDoadorPort;
import com.bloodlink.api.doador.application.port.out.AutocadastroComando;
import com.bloodlink.api.doador.doadorException.RegistroDoadorException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.doador.domain.OrigemCadastro;
import com.bloodlink.api.doador.domain.SexoBiologico;
import com.bloodlink.api.doador.domain.TipoSanguineo;
import com.bloodlink.api.shared.tenant.application.port.out.TenantProvisionamentoRepositoryPort;
import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import com.bloodlink.api.shared.tenant.domain.TenantProvisionamento;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutocadastroDoadorServiceTest {

    private static final String CENTRO = "95b82d103312";
    private static final String SCHEMA = "centro_" + CENTRO;

    @Mock
    private TenantProvisionamentoRepositoryPort provisionamentos;
    @Mock
    private AutocadastroPersistenciaService persistencia;
    @Mock
    private ContaDoadorPort contas;
    @Mock
    private CentroConsultaPort centros;

    private AutocadastroDoadorService service;

    @BeforeEach
    void preparar() {
        Clock clock = Clock.fixed(Instant.now(), ZoneOffset.UTC);
        service = new AutocadastroDoadorService(provisionamentos, persistencia, contas, centros, clock);
        lenient().when(centros.aceitaNovosCadastros(anyString())).thenReturn(true);
        TenantContext.clear();
    }

    @AfterEach
    void limpar() {
        TenantContext.clear();
    }

    private static LocalDate hoje() {
        return LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    }

    private static AutocadastroComando.RespostasFicha ficha() {
        return new AutocadastroComando.RespostasFicha(true, true, true, false, false, false, false, false, null);
    }

    private static AutocadastroComando comando() {
        return new AutocadastroComando(CENTRO, "  Maria   Souza ", "529.982.247-25", hoje().minusYears(30),
                "feminino", "o+", new BigDecimal("62.5"), "(11) 98765-4321", " Maria@Exemplo.com ",
                "São Paulo", "sp", hoje().minusMonths(8), false, true, ficha());
    }

    /** Variante do comando válido com um campo trocado. */
    private static AutocadastroComando comando(UnaryOperator<Campos> alteracao) {
        AutocadastroComando c = comando();
        Campos x = new Campos(c);
        alteracao.apply(x);
        return x.montar();
    }

    private static final class Campos {
        String centroId, nome, cpf, sexo, tipo, telefone, email, cidade, uf;
        LocalDate nascimento, ultimaDoacao;
        BigDecimal peso;
        Boolean consentimento, aceite;
        AutocadastroComando.RespostasFicha ficha;

        Campos(AutocadastroComando c) {
            centroId = c.centroId(); nome = c.nome(); cpf = c.cpf(); sexo = c.sexo(); tipo = c.tipoSanguineo();
            telefone = c.telefone(); email = c.email(); cidade = c.cidade(); uf = c.uf();
            nascimento = c.dataNascimento(); ultimaDoacao = c.ultimaDoacao(); peso = c.pesoKg();
            consentimento = c.consentimentoResponsavel(); aceite = c.aceiteArmazenamento(); ficha = c.ficha();
        }

        AutocadastroComando montar() {
            return new AutocadastroComando(centroId, nome, cpf, nascimento, sexo, tipo, peso, telefone, email,
                    cidade, uf, ultimaDoacao, consentimento, aceite, ficha);
        }
    }

    private void centroPronto() {
        when(provisionamentos.buscarPorSchema(SCHEMA)).thenReturn(Optional.of(provisionamento(StatusProvisionamento.CONCLUIDO)));
    }

    private static TenantProvisionamento provisionamento(StatusProvisionamento status) {
        return new TenantProvisionamento(UUID.randomUUID(), UUID.randomUUID(), SCHEMA, status,
                Instant.EPOCH, Instant.EPOCH, null);
    }

    private static AutocadastroPersistenciaService.Resultado convite(UUID id) {
        return new AutocadastroPersistenciaService.Resultado(id, "Maria Souza", "maria@exemplo.com", true);
    }

    // ---------- caminho feliz ----------

    @Test
    @DisplayName("cadastra no schema do hemocentro, normaliza os dados e pede o link ao Auth")
    void sucesso() {
        centroPronto();
        AtomicReference<String> schemaDuranteGravacao = new AtomicReference<>();
        UUID doadorId = UUID.randomUUID();
        when(persistencia.registrarOuLocalizar(any(Doador.class), any(FichaPreDoacao.class))).thenAnswer(i -> {
            schemaDuranteGravacao.set(TenantContext.get());
            return convite(doadorId);
        });

        service.cadastrar(comando());

        ArgumentCaptor<Doador> doador = ArgumentCaptor.forClass(Doador.class);
        ArgumentCaptor<FichaPreDoacao> ficha = ArgumentCaptor.forClass(FichaPreDoacao.class);
        verify(persistencia).registrarOuLocalizar(doador.capture(), ficha.capture());
        Doador d = doador.getValue();
        assertThat(d.nome()).isEqualTo("Maria Souza");
        assertThat(d.cpf()).isEqualTo("52998224725");
        assertThat(d.email()).isEqualTo("maria@exemplo.com");
        assertThat(d.telefone()).isEqualTo("11987654321");
        assertThat(d.sexo()).isEqualTo(SexoBiologico.FEMININO);
        assertThat(d.tipoSanguineo()).isEqualTo(TipoSanguineo.O_POSITIVO);
        assertThat(d.uf()).isEqualTo("SP");
        assertThat(d.origemCadastro()).isEqualTo(OrigemCadastro.AUTOCADASTRO);
        assertThat(d.ativo()).isTrue();
        assertThat(d.consentimentoLgpdEm()).isNotNull();
        assertThat(ficha.getValue().doadorId()).isEqualTo(d.id());

        assertThat(schemaDuranteGravacao.get()).isEqualTo(SCHEMA);
        assertThat(TenantContext.get()).isEqualTo("public");   // contexto limpo depois

        ArgumentCaptor<ContaDoadorPort.SolicitacaoContaDoador> pedido =
                ArgumentCaptor.forClass(ContaDoadorPort.SolicitacaoContaDoador.class);
        verify(contas).solicitarConta(pedido.capture());
        assertThat(pedido.getValue().doadorId()).isEqualTo(doadorId);
        assertThat(pedido.getValue().centroId()).isEqualTo(CENTRO);
        assertThat(pedido.getValue().email()).isEqualTo("maria@exemplo.com");
    }

    @Test
    @DisplayName("aceita o id do centro em maiúsculas e tipo sanguíneo pelo nome do enum")
    void aceitaVariacoes() {
        centroPronto();
        when(persistencia.registrarOuLocalizar(any(), any())).thenReturn(convite(UUID.randomUUID()));

        service.cadastrar(comando(c -> { c.centroId = " 95B82D103312 "; c.tipo = "O_POSITIVO"; return c; }));

        verify(persistencia).registrarOuLocalizar(any(), any());
    }

    @Test
    @DisplayName("campos opcionais podem faltar: telefone, cidade, UF e última doação")
    void opcionais() {
        centroPronto();
        when(persistencia.registrarOuLocalizar(any(), any())).thenReturn(convite(UUID.randomUUID()));

        service.cadastrar(comando(c -> {
            c.telefone = null; c.cidade = null; c.uf = null; c.ultimaDoacao = null; return c;
        }));

        verify(persistencia).registrarOuLocalizar(any(), any());
    }

    // ---------- validações: nada é tocado quando algo está errado ----------

    @Test
    @DisplayName("comando nulo e dados inválidos são recusados antes de consultar banco ou Auth")
    void validacoes() {
        AutocadastroComando[] invalidos = {
                null,
                comando(c -> { c.centroId = null; return c; }),
                comando(c -> { c.centroId = "xyz"; return c; }),
                comando(c -> { c.centroId = "95b82d10331"; return c; }),
                comando(c -> { c.centroId = "../../public"; return c; }),
                comando(c -> { c.nome = "  "; return c; }),
                comando(c -> { c.cpf = "111.111.111-11"; return c; }),
                comando(c -> { c.nascimento = null; return c; }),
                comando(c -> { c.nascimento = hoje().minusYears(10); return c; }),
                comando(c -> { c.sexo = null; return c; }),
                comando(c -> { c.sexo = "OUTRO"; return c; }),
                comando(c -> { c.tipo = null; return c; }),
                comando(c -> { c.tipo = "Não sei"; return c; }),
                comando(c -> { c.peso = null; return c; }),
                comando(c -> { c.peso = new BigDecimal("10"); return c; }),
                comando(c -> { c.telefone = "123"; return c; }),
                comando(c -> { c.email = null; return c; }),
                comando(c -> { c.email = "  "; return c; }),
                comando(c -> { c.email = "invalido"; return c; }),
                comando(c -> { c.uf = "XX"; return c; }),
                comando(c -> { c.ultimaDoacao = hoje().plusDays(1); return c; }),
                comando(c -> { c.aceite = false; return c; }),
                comando(c -> { c.aceite = null; return c; }),
                comando(c -> { c.ficha = null; return c; }),
                comando(c -> { c.nascimento = hoje().minusYears(17); c.consentimento = false; return c; }),
                comando(c -> { c.nascimento = hoje().minusYears(17); c.consentimento = null; return c; })
        };
        for (AutocadastroComando invalido : invalidos) {
            assertThatThrownBy(() -> service.cadastrar(invalido)).isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(provisionamentos, persistencia, contas);
    }

    @Test
    @DisplayName("adolescente de 17 anos com consentimento do responsável é aceito")
    void adolescenteComConsentimento() {
        centroPronto();
        when(persistencia.registrarOuLocalizar(any(), any())).thenReturn(convite(UUID.randomUUID()));

        service.cadastrar(comando(c -> { c.nascimento = hoje().minusYears(17); c.consentimento = true; return c; }));

        verify(persistencia).registrarOuLocalizar(any(), any());
    }

    // ---------- hemocentro ----------

    @Test
    @DisplayName("hemocentro inexistente ou com schema não concluído: não disponível, nada gravado")
    void hemocentroIndisponivel() {
        when(provisionamentos.buscarPorSchema(SCHEMA)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cadastrar(comando())).isInstanceOf(RegraDeNegocioException.class);

        for (StatusProvisionamento status : new StatusProvisionamento[]{StatusProvisionamento.PENDENTE,
                StatusProvisionamento.EM_ANDAMENTO, StatusProvisionamento.CANCELADO, StatusProvisionamento.REJEITADO}) {
            when(provisionamentos.buscarPorSchema(SCHEMA)).thenReturn(Optional.of(provisionamento(status)));
            assertThatThrownBy(() -> service.cadastrar(comando())).isInstanceOf(RegraDeNegocioException.class);
        }
        verifyNoInteractions(persistencia, contas);
    }

    @Test
    @DisplayName("hemocentro desativado: não disponível para novos cadastros, nada gravado")
    void hemocentroDesativado() {
        centroPronto();
        when(centros.aceitaNovosCadastros(CENTRO)).thenReturn(false);

        assertThatThrownBy(() -> service.cadastrar(comando())).isInstanceOf(RegraDeNegocioException.class);
        verifyNoInteractions(persistencia, contas);
    }

    @Test
    @DisplayName("falha ao consultar o hemocentro vira RegistroDoadorException")
    void falhaAoConsultarHemocentro() {
        when(provisionamentos.buscarPorSchema(anyString())).thenThrow(new RuntimeException("banco fora"));

        assertThatThrownBy(() -> service.cadastrar(comando())).isInstanceOf(RegistroDoadorException.class);
        verifyNoInteractions(persistencia, contas);
    }

    // ---------- persistência e Auth ----------

    @Test
    @DisplayName("CPF repetido em corrida (violação de restrição): responde normal, sem convite e sem erro")
    void violacaoDeIntegridade() {
        centroPronto();
        when(persistencia.registrarOuLocalizar(any(), any())).thenThrow(new DataIntegrityViolationException("uk_cpf"));

        service.cadastrar(comando());

        verifyNoInteractions(contas);
        assertThat(TenantContext.get()).isEqualTo("public");
    }

    @Test
    @DisplayName("CPF já cadastrado com outro e-mail: resultado sem convite, Auth não é chamado")
    void semConvite() {
        centroPronto();
        when(persistencia.registrarOuLocalizar(any(), any()))
                .thenReturn(new AutocadastroPersistenciaService.Resultado(null, null, null, false));

        service.cadastrar(comando());

        verifyNoInteractions(contas);
    }

    @Test
    @DisplayName("falha inesperada ao gravar vira RegistroDoadorException e o contexto do tenant é limpo")
    void falhaAoGravar() {
        centroPronto();
        when(persistencia.registrarOuLocalizar(any(), any())).thenThrow(new RuntimeException("falha"));

        assertThatThrownBy(() -> service.cadastrar(comando())).isInstanceOf(RegistroDoadorException.class);
        assertThat(TenantContext.get()).isEqualTo("public");
        verifyNoInteractions(contas);
    }

    @Test
    @DisplayName("Auth responde conflito (conta já existe): engolido, para não revelar quem está cadastrado")
    void conflitoDoAuthEEngolido() {
        centroPronto();
        when(persistencia.registrarOuLocalizar(any(), any())).thenReturn(convite(UUID.randomUUID()));
        doThrow(new ConflitoException("conta existente")).when(contas).solicitarConta(any());

        service.cadastrar(comando());   // não lança

        verify(contas).solicitarConta(any());
    }

    @Test
    @DisplayName("outras falhas do Auth sobem, para o doador poder tentar de novo")
    void falhaDoAuthSobe() {
        centroPronto();
        when(persistencia.registrarOuLocalizar(any(), any())).thenReturn(convite(UUID.randomUUID()));
        doThrow(new IllegalStateException("auth fora")).when(contas).solicitarConta(any());

        assertThatThrownBy(() -> service.cadastrar(comando())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("toString do comando e das respostas não expõe dados pessoais nem de saúde")
    void toStringsSeguros() {
        String texto = comando().toString();

        assertThat(texto).doesNotContain("Maria").doesNotContain("529").doesNotContain("exemplo.com");
        assertThat(ficha().toString()).isEqualTo("RespostasFicha[***]");
    }

    @Test
    @DisplayName("nada é gravado quando o hemocentro é inválido, mesmo com tudo certo no resto")
    void naoGravaSemHemocentro() {
        assertThatThrownBy(() -> service.cadastrar(comando(c -> { c.centroId = "inexistente0"; return c; })))
                .isInstanceOf(IllegalArgumentException.class);

        verify(persistencia, never()).registrarOuLocalizar(any(), any());
    }
}
