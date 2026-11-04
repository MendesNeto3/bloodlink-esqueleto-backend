package com.bloodlink.centro.adapter.out.persistence;

import com.bloodlink.centro.domain.TipoCentro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "centro")
@Getter
@Setter
@NoArgsConstructor
public class CentroJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String schema;

    @Column(nullable = false)
    private boolean ativo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoCentro tipo = TipoCentro.FIXO;

    private String endereco;

    private String cidade;

    private String uf;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Column(name = "dias_funcionamento")
    private String diasFuncionamento;

    private LocalTime abertura;

    private LocalTime fechamento;
}