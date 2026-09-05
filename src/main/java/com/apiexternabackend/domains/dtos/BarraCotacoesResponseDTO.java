package com.apiexternabackend.domains.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BarraCotacoesResponseDTO {

    private List<ItemBarraCotacoesDTO> itens = new ArrayList<>();
    private LocalDateTime atualizadoEm;
    private List<String> avisos = new ArrayList<>();
}
