package com.bloodlink.api.documento.adapter.out.antivirus;

import com.bloodlink.api.documento.application.port.out.ResultadoAnalise;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ClamAvAnalisadorTest {

    @Test
    @DisplayName("interpreta as respostas do clamd")
    void interpretar() {
        assertThat(ClamAvAnalisador.interpretar("stream: OK\0")).isEqualTo(ResultadoAnalise.LIMPO);
        assertThat(ClamAvAnalisador.interpretar("stream: Eicar-Test-Signature FOUND\0"))
                .isEqualTo(ResultadoAnalise.INFECTADO);
        assertThat(ClamAvAnalisador.interpretar("INSTREAM size limit exceeded. ERROR\0"))
                .isEqualTo(ResultadoAnalise.INDISPONIVEL);
        assertThat(ClamAvAnalisador.interpretar("")).isEqualTo(ResultadoAnalise.INDISPONIVEL);
        assertThat(ClamAvAnalisador.interpretar(null)).isEqualTo(ResultadoAnalise.INDISPONIVEL);
    }

    @Test
    @DisplayName("sem servidor escutando, o resultado é INDISPONIVEL e nada é lançado")
    void semServidor() throws IOException {
        int portaLivre;
        try (ServerSocket s = new ServerSocket(0)) {
            portaLivre = s.getLocalPort();
        }

        ResultadoAnalise r = new ClamAvAnalisador("localhost", portaLivre, 2).analisar("abc".getBytes());

        assertThat(r).isEqualTo(ResultadoAnalise.INDISPONIVEL);
    }

    @Test
    @DisplayName("fala o protocolo INSTREAM: comando, pedaços com tamanho de 4 bytes e terminador zero")
    void protocolo() throws Exception {
        try (ServerSocket servidor = new ServerSocket(0)) {
            ByteArrayOutputStream recebido = new ByteArrayOutputStream();
            Thread clamd = new Thread(() -> {
                try (var cliente = servidor.accept()) {
                    DataInputStream in = new DataInputStream(cliente.getInputStream());
                    byte[] comando = new byte[10];
                    in.readFully(comando);
                    recebido.write(comando);
                    while (true) {
                        int tamanho = in.readInt();
                        if (tamanho == 0) {
                            break;
                        }
                        byte[] pedaco = new byte[tamanho];
                        in.readFully(pedaco);
                        recebido.write(pedaco);
                    }
                    cliente.getOutputStream().write("stream: OK\0".getBytes(StandardCharsets.US_ASCII));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            clamd.start();

            byte[] conteudo = new byte[150_000];   // mais de dois pedaços de 64 KB
            for (int i = 0; i < conteudo.length; i++) {
                conteudo[i] = (byte) i;
            }
            ResultadoAnalise r = new ClamAvAnalisador("localhost", servidor.getLocalPort(), 5).analisar(conteudo);
            clamd.join(5000);

            assertThat(r).isEqualTo(ResultadoAnalise.LIMPO);
            byte[] bytes = recebido.toByteArray();
            assertThat(new String(bytes, 0, 10, StandardCharsets.US_ASCII)).isEqualTo("zINSTREAM\0");
            byte[] arquivoRecebido = java.util.Arrays.copyOfRange(bytes, 10, bytes.length);
            assertThat(arquivoRecebido).isEqualTo(conteudo);
        }
    }
}
