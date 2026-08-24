package com.apiexternabackend.mappers;

import com.apiexternabackend.domains.Carteira;
import com.apiexternabackend.domains.dtos.CarteiraResponseDTO;
import com.apiexternabackend.domains.enums.Mercado;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CarteiraMapper {

    @Mapping(target = "investidorId", source = "investidor.id")
    @Mapping(target = "corretoraId", source = "corretora.id")
    @Mapping(target = "nomeCorretora", source = "corretora.razaoSocial")
    @Mapping(target = "moeda", source = "mercado", qualifiedByName = "mercadoParaMoeda")
    CarteiraResponseDTO toResponse(Carteira carteira);

    @Named("mercadoParaMoeda")
    default String mercadoParaMoeda(Mercado mercado) {
        return mercado == Mercado.BR ? "BRL" : "USD";
    }
}