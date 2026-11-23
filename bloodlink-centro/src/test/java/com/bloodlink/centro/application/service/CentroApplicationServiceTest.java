package com.bloodlink.centro.application.service;

import com.bloodlink.centro.application.port.out.CentroRepositoryPort;
import com.bloodlink.centro.application.port.out.SchemaProvisionerGateway;
import com.bloodlink.centro.domain.Centro;
import com.bloodlink.centro.domain.CentroComDistancia;
import com.bloodlink.centro.domain.DiaSemana;
import com.bloodlink.centro.domain.PerfilCentro;
import com.bloodlink.centro.domain.TipoCentro;
import com.bloodlink.common.cache.CacheJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CentroApplicationServiceTest {

    private static final String CHAVE_ATIVOS = "bloodlink:centros:ativos";

    @Mock
    private CentroRepositoryPort repo;
    @Mock
    private SchemaProvisionerGateway provisionador;
    @Mock
    private CacheJson cache;

    private CentroApplicationService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        service = new CentroApplicationService(repo, provisionador, cache);
        // cache sempre vazio: executa a origem
        when(cache.obterOu(eq(CHAVE_ATIVOS), any(), any(), any()))
                .thenAnswer(i -> ((Supplier<Object>) i.getArgument(3)).get());
        when(repo.salvar(any(Centro.class))).thenAnswer(i -> {
            Centro c = i.getArgument(0);
            return new Centro(c.id() == null ? UUID.randomUUID() : c.id(), c.nome(), c.schema(), c.ativo(), c.perfil());
        });
    }

    private Centro centro(String nome, String schema, boolean ativo, String cidade, String uf, Double lat, Double lng) {
        PerfilCentro p = new PerfilCentro(TipoCentro.FIXO, "Rua A", cidade, uf,
                lat == null ? null : BigDecimal.valueOf(lat), lng == null ? null : BigDecimal.valueOf(lng),
                Set.of(DiaSemana.SEG), null, null, 20);
        return new Centro(UUID.randomUUID(), nome, schema, ativo, p);
    }

    // ---------- registrar ----------

    @Test
    @DisplayName("registrar: gera schema centro_<12 hex>, salva ativo, limpa o cache e provisiona")
    void registrar() {
        Centro c = service.registrar("  Hemocentro Central ", null);

        assertThat(c.nome()).isEqualTo("Hemocentro Central");
        assertThat(c.schema()).matches("centro_[a-f0-9]{12}");
        assertThat(c.ativo()).isTrue();
        assertThat(c.perfil()).isEqualTo(PerfilCentro.vazio());
        verify(cache).apagar(CHAVE_ATIVOS);
        verify(provisionador).provisionarEmtodosOsServicos(c.id(), c.schema(), true);
    }

    @Test
    @DisplayName("registrar: dois centros nunca recebem o mesmo schema")
    void schemasDiferentes() {
        assertThat(service.registrar("A").schema()).isNotEqualTo(service.registrar("B").schema());
    }

    @Test
    @DisplayName("registrar: nome vazio ou acima de 150 caracteres é recusado sem tocar no banco")
    void registrarInvalido() {
        assertThatThrownBy(() -> service.registrar(null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registrar("  ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.registrar("x".repeat(151), null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo, provisionador);

        service.registrar("x".repeat(150), null);
        verify(repo).salvar(any());
    }

    // ---------- listagens ----------

    @Test
    @DisplayName("listarAtivos: só os ativos, via cache")
    void listarAtivos() {
        Centro ativo = centro("A", "centro_aaaaaaaaaaaa", true, null, null, null, null);
        Centro inativo = centro("B", "centro_bbbbbbbbbbbb", false, null, null, null, null);
        when(repo.listarTodos()).thenReturn(List.of(ativo, inativo));

        assertThat(service.listarAtivos()).containsExactly(ativo);
        assertThat(service.listar()).containsExactly(ativo, inativo);
    }

    @Test
    @DisplayName("listarPublicos sem coordenadas: ordem alfabética sem diferenciar maiúsculas, distância nula")
    void publicosSemCoordenadas() {
        when(repo.listarTodos()).thenReturn(List.of(
                centro("zeta", "centro_aaaaaaaaaaaa", true, null, null, null, null),
                centro("Alfa", "centro_bbbbbbbbbbbb", true, null, null, null, null),
                centro("beta", "centro_cccccccccccc", true, null, null, null, null)));

        List<CentroComDistancia> r = service.listarPublicos(null, null);

        assertThat(r).extracting(i -> i.centro().nome()).containsExactly("Alfa", "beta", "zeta");
        assertThat(r).allMatch(i -> i.distanciaKm() == null);
    }

    @Test
    @DisplayName("listarPublicos com coordenadas: do mais perto ao mais longe; centro sem coordenada vai por último")
    void publicosComCoordenadas() {
        when(repo.listarTodos()).thenReturn(List.of(
                centro("Rio", "centro_aaaaaaaaaaaa", true, "Rio de Janeiro", "RJ", -22.9068, -43.1729),
                centro("Sem coordenada", "centro_bbbbbbbbbbbb", true, null, null, null, null),
                centro("Campinas", "centro_cccccccccccc", true, "Campinas", "SP", -22.9056, -47.0608),
                centro("Inativo", "centro_dddddddddddd", false, "São Paulo", "SP", -23.55, -46.63)));

        List<CentroComDistancia> r = service.listarPublicos(-23.5505, -46.6333);

        assertThat(r).extracting(i -> i.centro().nome()).containsExactly("Campinas", "Rio", "Sem coordenada");
        assertThat(r.get(0).distanciaKm()).isBetween(80.0, 95.0);
        assertThat(r.get(1).distanciaKm()).isBetween(350.0, 370.0);
        assertThat(r.get(2).distanciaKm()).isNull();
    }

    @Test
    @DisplayName("listarPublicos: busca por nome, cidade ou endereço, sem acento e sem diferenciar maiúsculas")
    void publicosBusca() {
        when(repo.listarTodos()).thenReturn(List.of(
                centro("Hemocentro Central", "centro_aaaaaaaaaaaa", true, "São Paulo", "SP", null, null),
                centro("Banco de Sangue", "centro_bbbbbbbbbbbb", true, "Ribeirão Preto", "SP", null, null)));

        assertThat(service.listarPublicos(null, null, "SAO paulo")).extracting(i -> i.centro().nome())
                .containsExactly("Hemocentro Central");
        assertThat(service.listarPublicos(null, null, "ribeirao")).extracting(i -> i.centro().nome())
                .containsExactly("Banco de Sangue");
        assertThat(service.listarPublicos(null, null, "rua a")).hasSize(2);
        assertThat(service.listarPublicos(null, null, "  ")).hasSize(2);
        assertThat(service.listarPublicos(null, null, "inexistente")).isEmpty();
    }

    @Test
    @DisplayName("listarPublicos: lat/lng só juntas, dentro do intervalo; busca até 100 caracteres")
    void publicosValidacoes() {
        assertThatThrownBy(() -> service.listarPublicos(1.0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listarPublicos(null, 1.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listarPublicos(91.0, 0.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listarPublicos(0.0, 181.0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.listarPublicos(null, null, "x".repeat(101))).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    // ---------- buscarPorCentroId ----------

    @Test
    @DisplayName("buscarPorCentroId: formato inválido, inexistente e encontrado")
    void buscarPorCentroId() {
        Centro c = centro("A", "centro_95b82d103312", true, null, null, null, null);
        when(repo.buscarPorSchema("centro_95b82d103312")).thenReturn(Optional.of(c));
        when(repo.buscarPorSchema("centro_aaaaaaaaaaaa")).thenReturn(Optional.empty());

        assertThat(service.buscarPorCentroId(" 95b82d103312 ")).isEqualTo(c);
        assertThatThrownBy(() -> service.buscarPorCentroId("aaaaaaaaaaaa")).isInstanceOf(CentroNaoEncontradoException.class);
        for (String ruim : new String[]{null, "", "95B82D103312", "abc", "centro_95b82d103312"}) {
            assertThatThrownBy(() -> service.buscarPorCentroId(ruim)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ---------- atualizarNome ----------

    @Test
    @DisplayName("atualizarNome: o centro pode renomear o próprio; limpa lista pública e nome em cache")
    void atualizarNome() {
        Centro c = centro("Antigo", "centro_95b82d103312", true, null, null, null, null);
        when(repo.buscarPorId(c.id())).thenReturn(Optional.of(c));

        Centro r = service.atualizarNome(c.id(), "  Novo Nome ", "95b82d103312");

        assertThat(r.nome()).isEqualTo("Novo Nome");
        verify(cache).apagar(CHAVE_ATIVOS, "bloodlink:centro:nome:95b82d103312");
    }

    @Test
    @DisplayName("atualizarNome: a plataforma (solicitante nulo) pode renomear qualquer um")
    void atualizarNomePlataforma() {
        Centro c = centro("Antigo", "centro_95b82d103312", true, null, null, null, null);
        when(repo.buscarPorId(c.id())).thenReturn(Optional.of(c));

        assertThat(service.atualizarNome(c.id(), "Novo", null).nome()).isEqualTo("Novo");
    }

    @Test
    @DisplayName("atualizarNome: centro de outro solicitante responde 'não encontrado' e nada é salvo")
    void atualizarNomeDeOutro() {
        Centro c = centro("Antigo", "centro_95b82d103312", true, null, null, null, null);
        when(repo.buscarPorId(c.id())).thenReturn(Optional.of(c));

        assertThatThrownBy(() -> service.atualizarNome(c.id(), "Novo", "aaaaaaaaaaaa"))
                .isInstanceOf(CentroNaoEncontradoException.class);
        assertThatThrownBy(() -> service.atualizarNome(c.id(), "Novo", ""))
                .isInstanceOf(CentroNaoEncontradoException.class);
        verify(repo, never()).salvar(any());
    }

    @Test
    @DisplayName("atualizarNome: nome inválido e centro inexistente")
    void atualizarNomeErros() {
        UUID id = UUID.randomUUID();
        when(repo.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizarNome(id, " ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.atualizarNome(id, "Novo", null)).isInstanceOf(CentroNaoEncontradoException.class);
    }

    // ---------- atualizarPerfil ----------

    @Test
    @DisplayName("atualizarPerfil: sem capacidade no novo perfil, mantém a atual")
    void perfilMantemCapacidade() {
        Centro c = centro("A", "centro_95b82d103312", true, "Campinas", "SP", null, null);
        when(repo.buscarPorId(c.id())).thenReturn(Optional.of(c));
        PerfilCentro novo = new PerfilCentro(TipoCentro.POSTO, "Rua B", "Santos", "SP", null, null, null, null, null, null);

        Centro r = service.atualizarPerfil(c.id(), novo, "95b82d103312");

        assertThat(r.perfil().cidade()).isEqualTo("Santos");
        assertThat(r.perfil().capacidadeDiaria()).isEqualTo(20);
        verify(cache).apagar(CHAVE_ATIVOS);
    }

    @Test
    @DisplayName("atualizarPerfil: capacidade informada substitui; perfil nulo e centro alheio são recusados")
    void perfilDemais() {
        Centro c = centro("A", "centro_95b82d103312", true, null, null, null, null);
        when(repo.buscarPorId(c.id())).thenReturn(Optional.of(c));
        PerfilCentro novo = new PerfilCentro(null, null, null, null, null, null, null, null, null, 70);

        assertThat(service.atualizarPerfil(c.id(), novo, null).perfil().capacidadeDiaria()).isEqualTo(70);
        assertThatThrownBy(() -> service.atualizarPerfil(c.id(), null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.atualizarPerfil(c.id(), novo, "ffffffffffff"))
                .isInstanceOf(CentroNaoEncontradoException.class);
    }

    // ---------- desativar ----------

    @Test
    @DisplayName("desativar: salva como inativo e limpa o cache; inexistente falha")
    void desativar() {
        Centro c = centro("A", "centro_95b82d103312", true, null, null, null, null);
        when(repo.buscarPorId(c.id())).thenReturn(Optional.of(c));

        ArgumentCaptor<Centro> salvo = ArgumentCaptor.forClass(Centro.class);
        Centro r = service.desativar(c.id());

        verify(repo).salvar(salvo.capture());
        assertThat(salvo.getValue().ativo()).isFalse();
        assertThat(r.ativo()).isFalse();
        verify(cache).apagar(CHAVE_ATIVOS);

        UUID outro = UUID.randomUUID();
        when(repo.buscarPorId(outro)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.desativar(outro)).isInstanceOf(CentroNaoEncontradoException.class);
    }
}
