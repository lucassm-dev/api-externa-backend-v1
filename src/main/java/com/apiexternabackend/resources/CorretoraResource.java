package com.apiexternabackend.resources;

import org.springframework.web.bind.annotation.PatchMapping;
import com.apiexternabackend.domains.dtos.DescontinuacaoRequestDTO;
import com.apiexternabackend.domains.dtos.CarteirasAfetadasResponseDTO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.apiexternabackend.config.InvestidorPrincipal;
import com.apiexternabackend.domains.dtos.CorretoraRequestDTO;
import com.apiexternabackend.domains.dtos.CorretoraResponseDTO;
import com.apiexternabackend.services.CorretoraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/corretoras")
@RequiredArgsConstructor
public class CorretoraResource {

    private final CorretoraService service;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CorretoraResponseDTO> cadastrar(@RequestBody @Valid CorretoraRequestDTO dto) {
        CorretoraResponseDTO response = service.cadastrar(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<CorretoraResponseDTO>> listar(Pageable pageable) {
        return ResponseEntity.ok(service.listar(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CorretoraResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/cnpj/{cnpj}")
    public ResponseEntity<CorretoraResponseDTO> buscarPorCnpj(@PathVariable String cnpj) {
        return ResponseEntity.ok(service.buscarPorCnpj(cnpj));
    }

    @GetMapping("/{id}/carteiras-afetadas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CarteirasAfetadasResponseDTO> carteirasAfetadas(@PathVariable Long id) {
        return ResponseEntity.ok(service.carteirasAfetadas(id));
    }

    @PatchMapping("/{id}/descontinuar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CorretoraResponseDTO> descontinuar(@PathVariable Long id, @RequestBody @Valid DescontinuacaoRequestDTO dto,
                                                             @AuthenticationPrincipal InvestidorPrincipal admin) {
        return ResponseEntity.ok(service.descontinuar(id, dto.motivo(), admin.id()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id, @AuthenticationPrincipal InvestidorPrincipal admin) {
        service.excluir(id, admin.id());
        return ResponseEntity.noContent().build();
    }
}