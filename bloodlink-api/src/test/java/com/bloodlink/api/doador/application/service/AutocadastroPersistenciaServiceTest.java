package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.doador.adapter.out.persistence.FichaPreDoacaoRepositoryPort;
import com.bloodlink.api.doador.application.port.in.DoadorRepositoryPort;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.doador.domain.OrigemCadastro;
import com.bloodlink.api.doador.domain.SexoBiologico;
import com.bloodlink.api.doador.domain.TipoSanguineo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutocadastroPersistenciaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T15:00:00Z");
    private static final String CPF = "52998224725";

    @Mock
    private DoadorRepositoryPort doadores;
    @Mock
    private FichaPreDoacaoRepositoryPort fichas;

    private Doador doador(String email, boolean ativo) {
        return new Doador(UUID.randomUUID(), "Maria Souza", CPF, LocalDate.of(1990, 5, 10), SexoBiologico.FEMININO,
                TipoSanguineo.O_POSITIVO, new BigDecimal("62.5"), null, email, false, ativo, AGORA, AGORA,
                null, null, null, OrigemCadastro.AUTOCADASTRO, AGORA);
    }

    private FichaPreDoacao ficha(UUID doadorId) {
        return new FichaPreDoacao(doadorId, true, true, true, false, false, false, false, false, null, AGORA);
    }

    private AutocadastroPersistenciaService service() {
        return new AutocadastroPersistenciaService(doadores, fichas);
    }

    @Test
    @DisplayName("CPF novo: grava doador e ficha e manda convite")
    void cpfNovo() {
        Doador novo = doador("maria@exemplo.com", true);
        when(doadores.buscarPorCpf(CPF)).thenReturn(Optional.empty());
        when(doadores.salvar(novo)).thenReturn(novo);

        AutocadastroPersistenciaService.Resultado r = service().registrarOuLocalizar(novo, ficha(novo.id()));

        assertThat(r.enviarConvite()).isTrue();
        assertThat(r.doadorId()).isEqualTo(novo.id());
        assertThat(r.email()).isEqualTo("maria@exemplo.com");
        verify(fichas).salvar(any(FichaPreDoacao.class));
    }

    @Test
    @DisplayName("CPF existente com o mesmo e-mail (sem diferenciar maiúsculas): reenvia o convite ao cadastro que vale")
    void mesmoEmail() {
        Doador existente = doador("Maria@Exemplo.com", true);
        Doador novo = doador("maria@exemplo.com", true);
        when(doadores.buscarPorCpf(CPF)).thenReturn(Optional.of(existente));

        AutocadastroPersistenciaService.Resultado r = service().registrarOuLocalizar(novo, ficha(novo.id()));

        assertThat(r.enviarConvite()).isTrue();
        assertThat(r.doadorId()).isEqualTo(existente.id());
        verify(doadores, never()).salvar(any());
        verify(fichas, never()).salvar(any());
    }

    @Test
    @DisplayName("CPF existente com outro e-mail: nenhum convite, nada gravado, e nada que revele o cadastro")
    void outroEmail() {
        Doador existente = doador("maria@exemplo.com", true);
        Doador novo = doador("outra@pessoa.com", true);
        when(doadores.buscarPorCpf(CPF)).thenReturn(Optional.of(existente));

        AutocadastroPersistenciaService.Resultado r = service().registrarOuLocalizar(novo, ficha(novo.id()));

        assertThat(r.enviarConvite()).isFalse();
        assertThat(r.doadorId()).isNull();
        verify(doadores, never()).salvar(any());
    }

    @Test
    @DisplayName("CPF existente inativo ou sem e-mail cadastrado: nenhum convite")
    void inativoOuSemEmail() {
        Doador novo = doador("maria@exemplo.com", true);
        when(doadores.buscarPorCpf(CPF)).thenReturn(Optional.of(doador("maria@exemplo.com", false)));
        assertThat(service().registrarOuLocalizar(novo, ficha(novo.id())).enviarConvite()).isFalse();

        when(doadores.buscarPorCpf(CPF)).thenReturn(Optional.of(doador(null, true)));
        assertThat(service().registrarOuLocalizar(novo, ficha(novo.id())).enviarConvite()).isFalse();
    }

    @Test
    @DisplayName("toString do resultado não expõe nome nem e-mail")
    void toStringSeguro() {
        AutocadastroPersistenciaService.Resultado r =
                new AutocadastroPersistenciaService.Resultado(UUID.randomUUID(), "Maria Souza", "m@x.com", true);

        assertThat(r.toString()).doesNotContain("Maria").doesNotContain("m@x.com");
    }
}
