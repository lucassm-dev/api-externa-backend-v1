package com.apiexternabackend.resources;

import com.apiexternabackend.domains.dtos.RelatorioUsoResponseDTO;
import com.apiexternabackend.services.RelatorioUsoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/uso")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class RelatorioUsoResource {

    private final RelatorioUsoService service;

    @GetMapping
    public ResponseEntity<RelatorioUsoResponseDTO> relatorio(@RequestParam(required = false) LocalDate dataInicio,
                                                             @RequestParam(required = false) LocalDate dataFim) {
        return ResponseEntity.ok(service.relatorio(dataInicio, dataFim));
    }
}
