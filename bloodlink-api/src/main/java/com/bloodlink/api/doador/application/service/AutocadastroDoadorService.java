package com.bloodlink.api.doador.application.service;

import com.bloodlink.api.agendamento.application.port.out.CentroConsultaPort;
import com.bloodlink.api.doador.application.port.in.AutocadastroDoadorUseCase;
import com.bloodlink.api.doador.application.port.in.ContaDoadorPort;
import com.bloodlink.api.doador.application.port.out.AutocadastroComando;
import com.bloodlink.api.doador.doadorException.RegistroDoadorException;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.doador.domain.OrigemCadastro;
import com.bloodlink.api.doador.domain.SexoBiologico;
import com.bloodlink.api.doador.domain.TipoSanguineo;
import com.bloodlink.api.shared.tenant.application.port.out.TenantProvisionamentoRepositoryPort;
import com.bloodlink.api.shared.tenant.domain.StatusProvisionamento;
import com.bloodlink.auth.shared.ConversorEnum.ConversorEnum;
import com.bloodlink.auth.shared.exceptions.ConflitoException;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;
import com.bloodlink.common.log.MascaraLog;
import com.bloodlink.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AutocadastroDoadorService implements AutocadastroDoadorUseCase {

    private static final Logger log = LoggerFactory.getLogger(AutocadastroDoadorService.class);

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final String PREFIXO_SCHEMA = "centro_";
    private static final Pattern CENTRO_ID = Pattern.compile("[a-f0-9]{12}");

    private final TenantProvisionamentoRepositoryPort provisionamentos;
    private final AutocadastroPersistenciaService persistencia;
    private final ContaDoadorPort contas;
    private final CentroConsultaPort centros;
    private final Clock clock;

    public AutocadastroDoadorService(TenantProvisionamentoRepositoryPort provisionamentos,
                                     AutocadastroPersistenciaService persistencia,
                                     ContaDoadorPort contas,
                                     CentroConsultaPort centros,
                                     Clock clock) {
        this.provisionamentos = provisionamentos;
        this.persistencia = persistencia;
        this.contas = contas;
        this.centros = centros;
        this.clock = clock;
    }

    @Override
    public void cadastrar(AutocadastroComando comando) {
        if (comando == null) {
            throw new IllegalArgumentException("Os dados do cadastro são obrigatórios");
        }
        // ---- tudo é validado antes de tocar em banco ou em outro serviço ----
        String centroId = validarCentroId(comando.centroId());
        String nome = ValidacoesDoador.validarNome(comando.nome());
        String cpf = ValidacoesDoador.validarCpf(comando.cpf());
        LocalDate nascimento = ValidacoesDoador.validarNascimento(comando.dataNascimento());
        SexoBiologico sexo = ConversorEnum.obrigatorio(SexoBiologico.class, "sexo", comando.sexo());
        TipoSanguineo tipo = TipoSanguineo.de(comando.tipoSanguineo())
                .orElseThrow(() -> new IllegalArgumentException("Tipo sanguíneo inválido: use A+, A-, B+, B-, AB+, AB-, O+ ou O-"));
        BigDecimal peso = ValidacoesDoador.validarPeso(comando.pesoKg());
        String telefone = ValidacoesDoador.validarTelefone(comando.telefone());
        String email = ValidacoesDoador.validarEmail(comando.email());
        if (email == null) {
            throw new IllegalArgumentException("O e-mail é obrigatório para criar a conta");
        }
        String cidade = ValidacoesDoador.validarCidade(comando.cidade());
        String uf = ValidacoesDoador.validarUf(comando.uf());
        LocalDate ultimaDoacao = validarUltimaDoacao(comando.ultimaDoacao());
        boolean consentimentoResponsavel = Boolean.TRUE.equals(comando.consentimentoResponsavel());
        ValidacoesDoador.exigirConsentimentoSeAdolescente(nascimento, consentimentoResponsavel);
        if (!Boolean.TRUE.equals(comando.aceiteArmazenamento())) {
            throw new IllegalArgumentException("É necessário autorizar o armazenamento da ficha para continuar");
        }
        if (comando.ficha() == null) {
            throw new IllegalArgumentException("Responda todas as perguntas da ficha");
        }

        Instant agora = clock.instant();
        UUID doadorId = UUID.randomUUID();
        Doador novo = new Doador(doadorId, nome, cpf, nascimento, sexo, tipo, peso, telefone, email,
                consentimentoResponsavel, true, agora, agora,
                cidade, uf, ultimaDoacao, OrigemCadastro.AUTOCADASTRO, agora);
        AutocadastroComando.RespostasFicha r = comando.ficha();
        FichaPreDoacao ficha = new FichaPreDoacao(doadorId, r.bemDeSaude(), r.dormiu6h(), r.alimentadoHidratado(),
                r.alcool12h(), r.procedimentoRecente(), r.gestanteAmamentando(), r.doencaRelevante(),
                r.medicamentoOuVacina(), r.observacoes(), agora);

        String schema = resolverSchema(centroId);
        if (!centros.aceitaNovosCadastros(centroId)) {
            throw new RegraDeNegocioException("Este hemocentro não está disponível para novos cadastros");
        }
        AutocadastroPersistenciaService.Resultado resultado = persistir(schema, novo, ficha);
        if (resultado.enviarConvite()) {
            enviarConvite(resultado, centroId);
        }
    }

    private String validarCentroId(String centroId) {
        String limpo = centroId == null ? "" : centroId.trim().toLowerCase();
        if (!CENTRO_ID.matcher(limpo).matches()) {
            throw new IllegalArgumentException("Escolha um hemocentro válido");
        }
        return limpo;
    }

    private LocalDate validarUltimaDoacao(LocalDate data) {
        if (data != null && data.isAfter(LocalDate.now(clock.withZone(FUSO)))) {
            throw new IllegalArgumentException("A data da última doação não pode estar no futuro");
        }
        return data;
    }

    /** O hemocentro precisa existir e ter o schema pronto; a consulta roda no schema public. */
    private String resolverSchema(String centroId) {
        String schema = PREFIXO_SCHEMA + centroId;
        try {
            boolean pronto = provisionamentos.buscarPorSchema(schema)
                    .filter(p -> p.status() == StatusProvisionamento.CONCLUIDO)
                    .isPresent();
            if (!pronto) {
                throw new RegraDeNegocioException("Este hemocentro não está disponível para novos cadastros");
            }
            return schema;
        } catch (RegraDeNegocioException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Falha ao consultar o hemocentro {} no autocadastro - causa: {}", centroId, MascaraLog.causa(e));
            log.debug("Detalhe da falha", e);
            throw new RegistroDoadorException(e);
        }
    }

    private AutocadastroPersistenciaService.Resultado persistir(String schema, Doador novo, FichaPreDoacao ficha) {
        try {
            TenantContext.set(schema);
            return persistencia.registrarOuLocalizar(novo, ficha);
        } catch (DataIntegrityViolationException e) {
            // dois cadastros do mesmo CPF ao mesmo tempo: o outro venceu; resposta igual à de quem já tem cadastro
            log.warn("Autocadastro do doador {} barrado por restrição do banco - causa: {}",
                    novo.id(), MascaraLog.causa(e));
            return AutocadastroPersistenciaService.Resultado.nenhum();
        } catch (RuntimeException e) {
            log.error("Falha no autocadastro do doador {} - causa: {}", novo.id(), MascaraLog.causa(e));
            log.debug("Detalhe da falha no autocadastro", e);
            throw new RegistroDoadorException(e);
        } finally {
            TenantContext.clear();
        }
    }

    private void enviarConvite(AutocadastroPersistenciaService.Resultado r, String centroId) {
        try {
            contas.solicitarConta(new ContaDoadorPort.SolicitacaoContaDoador(
                    r.nome(), r.email(), centroId, r.doadorId()));
            log.info("Autocadastro do doador {}: link de ativação solicitado", r.doadorId());
        } catch (ConflitoException e) {
            // e-mail de outra conta ou conta já ativa: não pode aparecer para quem está anônimo
            log.info("Autocadastro do doador {}: conta já existente, nenhum link novo", r.doadorId());
        }
        // outras falhas do Auth sobem: o cadastro já existe e uma nova tentativa reenvia o link
    }
}
