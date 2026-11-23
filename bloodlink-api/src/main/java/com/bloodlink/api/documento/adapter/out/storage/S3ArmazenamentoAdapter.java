package com.bloodlink.api.documento.adapter.out.storage;

import com.bloodlink.api.documento.application.port.out.ArmazenamentoArquivoPort;
import com.bloodlink.api.documento.exception.OperacaoDocumentoException;
import com.bloodlink.common.log.MascaraLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.net.URI;
import java.time.Duration;

/**
 * Armazenamento compatível com S3: AWS S3 em produção, MinIO em desenvolvimento.
 * Criar o cliente não abre conexão: a API sobe mesmo com o armazenamento fora do ar.
 */
@Component
public class S3ArmazenamentoAdapter implements ArmazenamentoArquivoPort {

    private static final Logger log = LoggerFactory.getLogger(S3ArmazenamentoAdapter.class);

    private final S3Client s3;
    private final String bucket;
    private final boolean criptografiaNoServidor;

    public S3ArmazenamentoAdapter(
            @Value("${bloodlink.armazenamento.endpoint:}") String endpoint,
            @Value("${bloodlink.armazenamento.regiao:us-east-1}") String regiao,
            @Value("${bloodlink.armazenamento.bucket:bloodlink-documentos}") String bucket,
            @Value("${bloodlink.armazenamento.chave-acesso:}") String chaveAcesso,
            @Value("${bloodlink.armazenamento.chave-secreta:}") String chaveSecreta,
            @Value("${bloodlink.armazenamento.path-style:false}") boolean pathStyle,
            @Value("${bloodlink.armazenamento.criptografia-no-servidor:false}") boolean criptografiaNoServidor) {
        // sem chave configurada: usa a identidade do ambiente (por exemplo, o papel IAM em produção)
        AwsCredentialsProvider credenciais = chaveAcesso.isBlank()
                ? DefaultCredentialsProvider.create()
                : StaticCredentialsProvider.create(AwsBasicCredentials.create(chaveAcesso, chaveSecreta));

        S3ClientBuilder construtor = S3Client.builder()
                .region(Region.of(regiao))
                .credentialsProvider(credenciais)
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build())
                .overrideConfiguration(c -> c
                        .apiCallTimeout(Duration.ofSeconds(20))
                        .apiCallAttemptTimeout(Duration.ofSeconds(10)));
        if (!endpoint.isBlank()) {
            construtor.endpointOverride(URI.create(endpoint));
        }
        this.s3 = construtor.build();
        this.bucket = bucket;
        this.criptografiaNoServidor = criptografiaNoServidor;
    }

    @Override
    public void gravar(String chave, byte[] conteudo, String tipoConteudo) {
        try {
            PutObjectRequest.Builder pedido = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(chave)
                    .contentType(tipoConteudo)
                    .contentLength((long) conteudo.length);
            if (criptografiaNoServidor) {
                pedido.serverSideEncryption(ServerSideEncryption.AES256);
            }
            s3.putObject(pedido.build(), RequestBody.fromBytes(conteudo));
        } catch (SdkException e) {
            throw falha("gravar o arquivo", chave, e);
        }
    }

    @Override
    public byte[] ler(String chave) {
        try {
            return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(chave).build())
                    .asByteArray();
        } catch (SdkException e) {
            throw falha("ler o arquivo", chave, e);
        }
    }

    @Override
    public void remover(String chave) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(chave).build());
        } catch (SdkException e) {
            throw falha("remover o arquivo", chave, e);
        }
    }

    private OperacaoDocumentoException falha(String operacao, String chave, SdkException e) {
        log.error("Armazenamento: falha ao {} ({}) - causa: {}", operacao, chave, MascaraLog.causa(e));
        log.debug("Detalhe da falha no armazenamento", e);
        return new OperacaoDocumentoException(operacao, e);
    }
}
