package com.apiexternabackend.mappers;

import com.apiexternabackend.domains.Corretora;
import com.apiexternabackend.domains.dtos.CorretoraResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CorretoraMapper {

    CorretoraResponseDTO toResponse(Corretora corretora);
}
