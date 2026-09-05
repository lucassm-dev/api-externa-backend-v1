package com.apiexternabackend.domains;

import com.apiexternabackend.domains.enums.TipoOperacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "operacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Operacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carteira_id", nullable = false)
    private Carteira carteira;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private TipoOperacao tipo;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "preco_manual", nullable = false)
    private Boolean precoManual = false;

    @Column(name = "cotacao_no_momento", precision = 18, scale = 4)
    private BigDecimal cotacaoNoMomento;
}
