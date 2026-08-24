package com.apiexternabackend.infra.client.brapi.dtos;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BrapiResponseDTO {

    private List<BrapiResultDTO> results;
}
