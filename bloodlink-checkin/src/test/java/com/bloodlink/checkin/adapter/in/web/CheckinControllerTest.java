package com.bloodlink.checkin.adapter.in.web;

import com.bloodlink.checkin.application.exception.ConflitoException;
import com.bloodlink.checkin.application.exception.OperacaoCheckinException;
import com.bloodlink.checkin.application.exception.RecursoNaoEncontradoException;
import com.bloodlink.checkin.application.exception.RegraDeNegocioException;
import com.bloodlink.checkin.application.port.in.CheckinUseCase;
import com.bloodlink.checkin.domain.Checkin;
import com.bloodlink.checkin.domain.DetalheCheckin;
import com.bloodlink.checkin.domain.ItemFila;
import com.bloodlink.checkin.domain.ResultadoTriagem;
import com.bloodlink.checkin.domain.ResumoFila;
import com.bloodlink.checkin.domain.SituacaoCheckin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CheckinControllerTest {

    private static final Instant AGORA = Instant.parse("2026-11-17T15:00:00Z");
    private static final UUID USUARIO = UUID.randomUUID();

    @Mock
    private CheckinUseCase useCase;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new CheckinController(useCase))
                .setControllerAdvice(new CheckinExceptionHandler()).build();
    }

    private MockHttpServletRequestBuilder logado(MockHttpServletRequestBuilder b) {
        return b.principal(new UsernamePasswordAuthenticationToken(USUARIO.toString(), null, List.of()));
    }

    private DetalheCheckin detalhe() {
        Checkin c = Checkin.novo(UUID.randomUUID(), UUID.randomUUID(), USUARIO, AGORA)
                .comTriagem(ResultadoTriagem.INAPTA, "motivo clínico sigiloso", USUARIO, AGORA);
        return new DetalheCheckin(c, SituacaoCheckin.INAPTO);
    }

    @Test
    @DisplayName("POST /api/checkin: 201, Location e resposta sem a observação clínica")
    void registrarChegada() throws Exception {
        DetalheCheckin d = detalhe();
        when(useCase.registrarChegada(eq(d.checkin().agendamentoId()), eq(USUARIO))).thenReturn(d);

        mvc.perform(logado(post("/api/checkin")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agendamentoId\":\"" + d.checkin().agendamentoId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/checkin/" + d.checkin().id()))
                .andExpect(jsonPath("$.situacao").value("INAPTO"))
                .andExpect(jsonPath("$.triagem").value("INAPTA"))
                .andExpect(jsonPath("$.triagemObservacao").doesNotExist());
    }

    @Test
    @DisplayName("corpo malformado ou ausente: 400 com mensagem em português")
    void corpoInvalido() throws Exception {
        mvc.perform(logado(post("/api/checkin")).contentType(MediaType.APPLICATION_JSON).content("{nao-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Corpo da requisição inválido ou malformado"));
        mvc.perform(logado(post("/api/checkin")).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("PATCH /api/checkin/{id}/triagem: 200 e repassa resultado, observação e usuário")
    void triagem() throws Exception {
        DetalheCheckin d = detalhe();
        when(useCase.registrarTriagem(eq(d.checkin().id()), eq("INAPTA"), eq("pressão"), eq(USUARIO))).thenReturn(d);

        mvc.perform(logado(patch("/api/checkin/" + d.checkin().id() + "/triagem"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resultado\":\"INAPTA\",\"observacao\":\"pressão\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(d.checkin().id().toString()));
    }

    @Test
    @DisplayName("GET /api/checkin/{id}: 200; id que não é UUID: 400")
    void buscar() throws Exception {
        DetalheCheckin d = detalhe();
        when(useCase.buscar(d.checkin().id())).thenReturn(d);

        mvc.perform(get("/api/checkin/" + d.checkin().id())).andExpect(status().isOk());
        mvc.perform(get("/api/checkin/abc")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Valor inválido para o parâmetro 'id'"));
    }

    @Test
    @DisplayName("GET /api/checkin: fila com rótulo do tipo sanguíneo e totais")
    void fila() throws Exception {
        ItemFila item = new ItemFila(UUID.randomUUID(), null, UUID.randomUUID(), "Maria", "AB_NEGATIVO",
                LocalTime.of(9, 0), "CONFIRMADO", SituacaoCheckin.NAO_CHEGOU, null, null, null, null);
        EnumMap<SituacaoCheckin, Long> totais = new EnumMap<>(SituacaoCheckin.class);
        totais.put(SituacaoCheckin.NAO_CHEGOU, 1L);
        when(useCase.fila(eq(LocalDate.of(2026, 11, 17)), eq("NAO_CHEGOU")))
                .thenReturn(new ResumoFila(LocalDate.of(2026, 11, 17), totais, List.of(item)));

        mvc.perform(get("/api/checkin").param("data", "2026-11-17").param("situacao", "NAO_CHEGOU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.totais.NAO_CHEGOU").value(1))
                .andExpect(jsonPath("$.itens[0].tipoSanguineoRotulo").value("AB-"))
                .andExpect(jsonPath("$.itens[0].hora").value("09:00"));

        mvc.perform(get("/api/checkin").param("data", "17/11/2026")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("exceções de negócio viram o status HTTP certo")
    void statusDasExcecoes() throws Exception {
        UUID id = UUID.randomUUID();
        when(useCase.buscar(id))
                .thenThrow(new RecursoNaoEncontradoException("Check-in não encontrado"))
                .thenThrow(new ConflitoException("já registrado"))
                .thenThrow(new RegraDeNegocioException("fora do dia"))
                .thenThrow(new OperacaoCheckinException("consultar o check-in"))
                .thenThrow(new IllegalArgumentException("inválido"))
                .thenThrow(new RuntimeException("segredo interno"));

        int[] esperados = {404, 409, 422, 500, 400, 500};
        for (int esperado : esperados) {
            mvc.perform(get("/api/checkin/" + id)).andExpect(status().is(esperado))
                    .andExpect(jsonPath("$.status").value(esperado));
        }
    }

    @Test
    @DisplayName("erro inesperado não vaza a mensagem interna")
    void semVazamento() throws Exception {
        UUID id = UUID.randomUUID();
        when(useCase.buscar(id)).thenThrow(new RuntimeException("senha do banco = 123"));

        mvc.perform(get("/api/checkin/" + id)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Erro interno inesperado"));
    }

    @Test
    @DisplayName("método não suportado: 405")
    void metodo() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/checkin/" + UUID.randomUUID()))
                .andExpect(status().isMethodNotAllowed());
    }
}
