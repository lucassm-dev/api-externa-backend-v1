package com.apiexternabackend.mappers;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.dtos.AcaoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AcaoMapper {

    AcaoResponseDTO toResponse(Acao acao);
}
