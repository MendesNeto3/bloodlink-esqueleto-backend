package com.bloodlink.api.documento.adapter.out.antivirus;

import com.bloodlink.api.documento.application.port.out.AnalisadorArquivoPort;
import com.bloodlink.api.documento.application.port.out.ResultadoAnalise;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Envia o arquivo ao ClamAV (clamd) pelo comando INSTREAM, em pedaços de até 64 KB. O nome do vírus
 * encontrado nunca vai para o log junto do arquivo; só o resultado.
 */
@Component
@ConditionalOnProperty(name = "bloodlink.antivirus.habilitado", havingValue = "true")
public class ClamAvAnalisador implements AnalisadorArquivoPort {

    private static final Logger log = LoggerFactory.getLogger(ClamAvAnalisador.class);
    private static final int TAMANHO_PEDACO = 64 * 1024;
    private static final int RESPOSTA_MAXIMA = 1024;

    private final String host;
    private final int porta;
    private final int timeoutMs;

    public ClamAvAnalisador(@Value("${bloodlink.antivirus.host:localhost}") String host,
                            @Value("${bloodlink.antivirus.porta:3310}") int porta,
                            @Value("${bloodlink.antivirus.timeout-segundos:30}") int timeoutSegundos) {
        this.host = host;
        this.porta = porta;
        this.timeoutMs = timeoutSegundos * 1000;
    }

    @Override
    public ResultadoAnalise analisar(byte[] conteudo) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, porta), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            DataOutputStream saida = new DataOutputStream(socket.getOutputStream());
            saida.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));
            for (int inicio = 0; inicio < conteudo.length; inicio += TAMANHO_PEDACO) {
                int tamanho = Math.min(TAMANHO_PEDACO, conteudo.length - inicio);
                saida.writeInt(tamanho);          // 4 bytes, big-endian, como o clamd exige
                saida.write(conteudo, inicio, tamanho);
            }
            saida.writeInt(0);                    // fim do fluxo
            saida.flush();
            return interpretar(ler(socket.getInputStream()));
        } catch (IOException | RuntimeException e) {
            log.error("Falha ao consultar o antivírus - causa: {}", MascaraLog.causa(e));
            log.debug("Detalhe da falha no antivírus", e);
            return ResultadoAnalise.INDISPONIVEL;
        }
    }

    private static String ler(InputStream entrada) throws IOException {
        byte[] buffer = new byte[RESPOSTA_MAXIMA];
        int total = 0;
        int lido;
        while (total < buffer.length && (lido = entrada.read(buffer, total, buffer.length - total)) != -1) {
            total += lido;
        }
        return new String(buffer, 0, total, StandardCharsets.UTF_8);
    }

    /** "stream: OK" = limpo; "... FOUND" = infectado; qualquer outra coisa (ERROR, vazio) = indisponível. */
    static ResultadoAnalise interpretar(String resposta) {
        String limpa = resposta == null ? "" : resposta.replace("\0", "").trim();
        if (limpa.endsWith("FOUND")) {
            return ResultadoAnalise.INFECTADO;
        }
        if (limpa.endsWith("OK")) {
            return ResultadoAnalise.LIMPO;
        }
        return ResultadoAnalise.INDISPONIVEL;
    }
}
