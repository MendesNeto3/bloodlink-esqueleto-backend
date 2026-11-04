package com.bloodlink.api.doador.adapter.out.persistence;

import com.bloodlink.api.doador.doadorException.OperacaoDoadorException;
import com.bloodlink.api.doador.application.port.in.ContaDoadorPort;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class ContaDoadorAuthAdapter implements ContaDoadorPort {

    private static final Logger log = LoggerFactory.getLogger(ContaDoadorAuthAdapter.class);
    private static final String HEADER_CHAVE = "X-Servico-Chave";

    private final RestClient client;
    private final String chave;

    public ContaDoadorAuthAdapter(@Value("${bloodlink.auth.url:http://localhost:8085}") String urlAuth,
                                  @Value("${bloodlink.servico.chave:}") String chave) {
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(5));
        this.client = RestClient.builder().baseUrl(urlAuth).requestFactory(fabrica).build();
        this.chave = chave;
    }

    @Override
    public void solicitarConta(SolicitacaoContaDoador solicitacao) {
        try {
            client.post()
                    .uri("/internal/contas-doador")
                    .header(HEADER_CHAVE, chave)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("nome", solicitacao.nome(), "email", solicitacao.email(),
                            "centroId", solicitacao.centroId(), "doadorId", solicitacao.doadorId()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            if (status == 409) {
                throw new ConflitoException(
                        "Este doador já tem uma conta ativa, ou o e-mail pertence a outra conta");
            }
            if (status == 400 || status == 422) {
                throw new RegraDeNegocioException("Não foi possível criar a conta com os dados deste doador");
            }
            log.error("Auth recusou a criação da conta do doador {} (HTTP {}): verifique a chave de serviço",
                    solicitacao.doadorId(), status);
            throw new OperacaoDoadorException("enviar o convite");
        } catch (RuntimeException e) {
            log.error("Falha ao chamar o Auth para o doador {} - causa: {}", solicitacao.doadorId(), MascaraLog.causa(e));
            log.debug("Detalhe da falha ao chamar o Auth", e);
            throw new OperacaoDoadorException("enviar o convite");
        }
    }
}