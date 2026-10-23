package com.bloodlink.api.doador.adapter.in.web;

import com.bloodlink.api.doador.adapter.in.web.dto.AtualizarDoadorRequest;
import com.bloodlink.api.doador.adapter.in.web.dto.DoadorResponse;
import com.bloodlink.api.doador.adapter.in.web.dto.RegistrarDoadorRequest;
import com.bloodlink.api.doador.application.port.in.*;
import com.bloodlink.api.doador.application.port.out.*;
import com.bloodlink.api.doador.domain.Doador;
import com.bloodlink.api.doador.domain.TipoSanguineo;
import com.bloodlink.api.shared.Pagina.Pagina;
import com.bloodlink.auth.shared.exceptions.RegraDeNegocioException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doadores")
public class DoadorController {

    private final RegistrarDoadorUseCase registrarUseCase;
    private final ConsultarDoadorUseCase consultarUseCase;
    private final ListarDoadoresUseCase listarUseCase;
    private final AtualizarDoadorUseCase atualizarUseCase;
    private final AlterarSituacaoDoadorUseCase situacaoUseCase;
    private final ConsultarElegibilidadeUseCase elegibilidadeUseCase;
    private final DoacaoUseCase doacaoUseCase;
    private final ImpedimentoUseCase impedimentoUseCase;
    private final ConfirmarTipoSanguineoUseCase tipoSanguineoUseCase;

    public DoadorController(RegistrarDoadorUseCase registrarUseCase,
                            ConsultarDoadorUseCase consultarUseCase,
                            ListarDoadoresUseCase listarUseCase,
                            AtualizarDoadorUseCase atualizarUseCase,
                            AlterarSituacaoDoadorUseCase situacaoUseCase,
                            ConsultarElegibilidadeUseCase elegibilidadeUseCase,
                            DoacaoUseCase doacaoUseCase,
                            ImpedimentoUseCase impedimentoUseCase,
                            ConfirmarTipoSanguineoUseCase tipoSanguineoUseCase) {
        this.registrarUseCase = registrarUseCase;
        this.consultarUseCase = consultarUseCase;
        this.listarUseCase = listarUseCase;
        this.atualizarUseCase = atualizarUseCase;
        this.situacaoUseCase = situacaoUseCase;
        this.elegibilidadeUseCase = elegibilidadeUseCase;
        this.doacaoUseCase = doacaoUseCase;
        this.impedimentoUseCase = impedimentoUseCase;
        this.tipoSanguineoUseCase = tipoSanguineoUseCase;
    }

    @PostMapping
    public ResponseEntity<DoadorResponse> registrar(@RequestBody RegistrarDoadorRequest request) {
        Doador doador = registrarUseCase.registrar(request.paraComando());
        return ResponseEntity.created(URI.create("/api/doadores/" + doador.id()))
                .body(DoadorResponse.de(doador));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoadorResponse> consultar(@PathVariable UUID id) {
        return ResponseEntity.ok(DoadorResponse.de(consultarUseCase.porId(id)));
    }

    @PostMapping("/buscar")
    public ResponseEntity<DoadorResponse> buscar(@RequestBody BuscarDoadorRequest request) {
        boolean cpf = StringUtils.hasText(request.cpf());
        boolean email = StringUtils.hasText(request.email());
        boolean nome = StringUtils.hasText(request.nome());
        if ((cpf ? 1 : 0) + (email ? 1 : 0) + (nome ? 1 : 0) != 1) {
            throw new RegraDeNegocioException("Informe exatamente um critério de busca: cpf, email ou nome");
        }
        Doador doador = cpf ? consultarUseCase.porCpf(request.cpf())
                : email ? consultarUseCase.porEmail(request.email())
                : consultarUseCase.porNome(request.nome());
        return ResponseEntity.ok(DoadorResponse.de(doador));
    }

    @GetMapping
    public ResponseEntity<Pagina<DoadorResumoResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String tipoSanguineo,
            @RequestParam(required = false) Boolean ativo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        TipoSanguineo tipo = null;
        if (StringUtils.hasText(tipoSanguineo)) {
            tipo = TipoSanguineo.de(tipoSanguineo)
                    .orElseThrow(() -> new RegraDeNegocioException("Tipo sanguíneo inválido"));
        }
        FiltroDoador filtro = new FiltroDoador(StringUtils.hasText(nome) ? nome : null, tipo, ativo);
        return ResponseEntity.ok(listarUseCase.listar(filtro, page, size).mapear(DoadorResumoResponse::de));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DoadorResponse> atualizar(@PathVariable UUID id,
                                                    @RequestBody AtualizarDoadorRequest request) {
        return ResponseEntity.ok(DoadorResponse.de(atualizarUseCase.atualizar(id, request.paraComando())));
    }

    @PatchMapping("/{id}/inativar")
    public ResponseEntity<DoadorResponse> inativar(@PathVariable UUID id) {
        return ResponseEntity.ok(DoadorResponse.de(situacaoUseCase.inativar(id)));
    }

    @PatchMapping("/{id}/reativar")
    public ResponseEntity<DoadorResponse> reativar(@PathVariable UUID id) {
        return ResponseEntity.ok(DoadorResponse.de(situacaoUseCase.reativar(id)));
    }


    @GetMapping("/{id}/elegibilidade")
    public ResponseEntity<ElegibilidadeResponse> elegibilidade(@PathVariable UUID id) {
        return ResponseEntity.ok(ElegibilidadeResponse.de(elegibilidadeUseCase.consultar(id)));
    }

    @GetMapping("/{id}/doacoes")
    public ResponseEntity<List<DoacaoResponse>> historico(@PathVariable UUID id) {
        return ResponseEntity.ok(doacaoUseCase.historico(id)
                .stream()
                .map(DoacaoResponse::de)
                .toList());
    }

    @PostMapping("/{id}/doacoes")
    public ResponseEntity<DoacaoResponse> registrarDoacao(@PathVariable UUID id,
                                                          @RequestBody RegistrarDoacaoRequest request,
                                                          Authentication autenticacao) {
        var doacao = doacaoUseCase.registrar(request.paraComando(id, usuarioId(autenticacao)));
        return ResponseEntity.created(URI.create("/api/doadores/" + id + "/doacoes"))
                .body(DoacaoResponse.de(doacao));
    }

    @GetMapping("/{id}/impedimentos")
    public ResponseEntity<List<ImpedimentoResponse>> impedimentos(@PathVariable UUID id) {
        return ResponseEntity.ok(
                impedimentoUseCase.listar(id).stream().map(ImpedimentoResponse::de).toList());
    }

    @PostMapping("/{id}/impedimentos")
    public ResponseEntity<ImpedimentoResponse> registrarImpedimento(@PathVariable UUID id,
                                                                    @RequestBody RegistrarImpedimentoRequest request,
                                                                    Authentication autenticacao) {
        var impedimento = impedimentoUseCase.registrar(request.paraComando(id, usuarioId(autenticacao)));
        return ResponseEntity.created(URI.create("/api/doadores/" + id + "/impedimentos"))
                .body(ImpedimentoResponse.de(impedimento));
    }

    @PatchMapping("/{id}/impedimentos/{impedimentoId}/encerrar")
    public ResponseEntity<ImpedimentoResponse> encerrarImpedimento(@PathVariable UUID id,
                                                                   @PathVariable UUID impedimentoId) {
        return ResponseEntity.ok(ImpedimentoResponse.de(impedimentoUseCase.encerrar(id, impedimentoId)));
    }

    @PatchMapping("/{id}/tipo-sanguineo")
    public ResponseEntity<DoadorResponse> confirmarTipoSanguineo(@PathVariable UUID id,
                                                                 @RequestBody ConfirmarTipoSanguineoRequest request) {
        return ResponseEntity.ok(DoadorResponse.de(tipoSanguineoUseCase.confirmar(id, request.paraTipo())));
    }

    /** O nome da autenticação é o subject do token, que é o id do usuário. */
    private static UUID usuarioId(Authentication autenticacao) {
        return UUID.fromString(autenticacao.getName());
    }
}