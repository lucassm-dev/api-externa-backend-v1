package com.apiexternabackend.mappers;

import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.dtos.OperacaoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OperacaoMapper {

    @Mapping(target = "carteiraId", source = "carteira.id")
    @Mapping(target = "ticker", source = "acao.ticker")
    @Mapping(target = "valorTotal", expression = "java(operacao.getPrecoUnitario().multiply(java.math.BigDecimal.valueOf(operacao.getQuantidade())))")
    OperacaoResponseDTO toResponse(Operacao operacao);
}
