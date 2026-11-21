package com.apiexternabackend.resources;

import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.domains.dtos.AcaoResponseDTO;
import com.apiexternabackend.services.AcaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/acoes")
@RequiredArgsConstructor
public class AcaoResource {

    private final AcaoService service;

    @GetMapping
    public ResponseEntity<Page<AcaoResponseDTO>> listar(Pageable pageable) {
        return ResponseEntity.ok(service.listar(pageable));
    }

    @GetMapping("/ticker/{ticker}")
    public ResponseEntity<AcaoResponseDTO> buscarPorTicker(@PathVariable String ticker,
                                                           @RequestParam(required = false) Mercado mercado) {
        return ResponseEntity.ok(service.buscarPorTicker(ticker, mercado));
    }

    @PutMapping("/{id}/atualizar-cotacao")
    public ResponseEntity<AcaoResponseDTO> atualizarCotacao(
            @PathVariable Long id, @RequestParam(defaultValue = "false") boolean forcar) {
        return ResponseEntity.ok(service.atualizarCotacao(id, forcar));
    }
}
