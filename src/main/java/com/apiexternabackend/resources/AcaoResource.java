package com.apiexternabackend.resources;

import com.apiexternabackend.domains.dtos.AcaoRequestDTO;
import com.apiexternabackend.domains.dtos.AcaoResponseDTO;
import com.apiexternabackend.services.AcaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/acoes")
@RequiredArgsConstructor
public class AcaoResource {

    private final AcaoService service;

    @PostMapping
    public ResponseEntity<AcaoResponseDTO> cadastrar(@RequestBody @Valid AcaoRequestDTO dto) {
        AcaoResponseDTO response = service.cadastrar(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<AcaoResponseDTO>> listar(Pageable pageable) {
        return ResponseEntity.ok(service.listar(pageable));
    }

    @GetMapping("/ticker/{ticker}")
    public ResponseEntity<AcaoResponseDTO> buscarPorTicker(@PathVariable String ticker) {
        return ResponseEntity.ok(service.buscarPorTicker(ticker));
    }

    @PutMapping("/{id}/atualizar-cotacao")
    public ResponseEntity<AcaoResponseDTO> atualizarCotacao(@PathVariable Long id) {
        return ResponseEntity.ok(service.atualizarCotacao(id));
    }
}
