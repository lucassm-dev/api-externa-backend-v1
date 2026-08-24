package com.apiexternabackend.mappers;

import com.apiexternabackend.domains.CarteiraAcao;
import com.apiexternabackend.domains.dtos.CarteiraAcaoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CarteiraAcaoMapper {

    @Mapping(target = "ticker", source = "acao.ticker")
    @Mapping(target = "nomeEmpresa", source = "acao.nomeEmpresa")
    @Mapping(target = "cotacaoAtual", source = "acao.cotacaoAtual")
    @Mapping(target = "dataHoraCotacao", source = "acao.dataHoraCotacao")
    @Mapping(target = "rentabilidadeNaoRealizada", ignore = true)
    CarteiraAcaoResponseDTO toResponse(CarteiraAcao carteiraAcao);
}
