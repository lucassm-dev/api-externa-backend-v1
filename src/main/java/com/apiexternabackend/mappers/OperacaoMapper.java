package com.apiexternabackend.mappers;

import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.dtos.OperacaoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OperacaoMapper {

    @Mapping(target = "carteiraId", source = "carteira.id")
    @Mapping(target = "ticker", source = "acao.ticker")
    @Mapping(target = "moeda", source = "acao.moeda")
    @Mapping(target = "valorTotal", expression = "java(operacao.getPrecoUnitario().multiply(java.math.BigDecimal.valueOf(operacao.getQuantidade())))")
    @Mapping(target = "avisos", expression = "java(calcularAvisos(operacao))")
    OperacaoResponseDTO toResponse(Operacao operacao);

    /**
     * Aviso de desvio (RN-Q-MAP-04): só se preço foi informado manualmente e destoa
     * 10x ou mais da cotação de mercado do momento — nunca bloqueia, só avisa.
     */
    default List<String> calcularAvisos(Operacao operacao) {
        if (!Boolean.TRUE.equals(operacao.getPrecoManual())
                || operacao.getCotacaoNoMomento() == null
                || operacao.getCotacaoNoMomento().signum() == 0) {
            return List.of();
        }

        BigDecimal razao = operacao.getPrecoUnitario()
                .divide(operacao.getCotacaoNoMomento(), 4, RoundingMode.HALF_UP);

        boolean acima = razao.compareTo(BigDecimal.TEN) >= 0;
        boolean abaixo = razao.compareTo(new BigDecimal("0.1")) <= 0;
        if (!acima && !abaixo) {
            return List.of();
        }

        BigDecimal multiplo = acima ? razao : BigDecimal.ONE.divide(razao, 0, RoundingMode.HALF_UP);
        String direcao = acima ? "acima" : "abaixo";

        return List.of("O preço informado (" + operacao.getPrecoUnitario() + ") está " + multiplo
                + "x " + direcao + " da cotação atual (" + operacao.getCotacaoNoMomento()
                + "). Confirme se está correto.");
    }
}
