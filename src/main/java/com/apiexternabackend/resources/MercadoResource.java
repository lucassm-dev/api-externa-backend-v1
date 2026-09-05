package com.apiexternabackend.resources;

import com.apiexternabackend.domains.dtos.BarraCotacoesResponseDTO;
import com.apiexternabackend.services.BarraCotacoesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mercado")
@RequiredArgsConstructor
public class MercadoResource {

    private final BarraCotacoesService service;

    @GetMapping("/barra-cotacoes")
    public ResponseEntity<BarraCotacoesResponseDTO> barraCotacoes() {
        return ResponseEntity.ok(service.obter());
    }
}