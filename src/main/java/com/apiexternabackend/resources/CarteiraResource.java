package com.apiexternabackend.resources;

import com.apiexternabackend.domains.dtos.CarteiraRenomearDTO;
import com.apiexternabackend.domains.dtos.CarteiraRequestDTO;
import com.apiexternabackend.domains.dtos.CarteiraResponseDTO;
import com.apiexternabackend.services.CarteiraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/carteiras")
@RequiredArgsConstructor
public class CarteiraResource {

    private final CarteiraService service;

    @PostMapping
    public ResponseEntity<CarteiraResponseDTO> criar(@RequestBody @Valid CarteiraRequestDTO dto) {
        CarteiraResponseDTO response = service.criar(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<CarteiraResponseDTO>> listar(
            @RequestParam Long investidorId, Pageable pageable) {
        return ResponseEntity.ok(service.listarPorInvestidor(investidorId, pageable));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CarteiraResponseDTO> renomear(
            @PathVariable Long id, @RequestBody @Valid CarteiraRenomearDTO dto) {
        return ResponseEntity.ok(service.renomear(id, dto.getNome()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
