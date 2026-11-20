package com.apiexternabackend.mappers;

import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.dtos.InvestidorResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface InvestidorMapper {

    @Mapping(target = "email", source = "usuario.email")
    InvestidorResponseDTO toResponse(Investidor investidor);
}
