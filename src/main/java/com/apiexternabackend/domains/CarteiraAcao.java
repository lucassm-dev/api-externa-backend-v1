package com.apiexternabackend.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
    name = "carteira_acao",
    uniqueConstraints = @UniqueConstraint(columnNames = {"carteira_id", "acao_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CarteiraAcao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carteira_id", nullable = false)
    private Carteira carteira;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal precoMedio;

    @Column(name = "custo_total_brl", nullable = false, precision = 18, scale = 4)
    private BigDecimal custoTotalBrl = BigDecimal.ZERO;
}
