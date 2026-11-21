package com.apiexternabackend.resources;

import com.apiexternabackend.config.InvestidorPrincipal;
import com.apiexternabackend.domains.dtos.BloqueioRequestDTO;
import com.apiexternabackend.domains.dtos.CriarAdminRequestDTO;
import com.apiexternabackend.domains.dtos.UsuarioAdminResponseDTO;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.services.UsuarioAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UsuarioAdminResource {

    private final UsuarioAdminService service;

    @GetMapping
    public ResponseEntity<Page<UsuarioAdminResponseDTO>> listar(@RequestParam(required = false) SituacaoUsuario situacao,
                                                                @PageableDefault(sort = "id") Pageable pageable) {
        return ResponseEntity.ok(service.listar(situacao, pageable));
    }

    @PostMapping
    public ResponseEntity<UsuarioAdminResponseDTO> criarAdmin(@RequestBody @Valid CriarAdminRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarAdmin(dto));
    }

    @PatchMapping("/{id}/bloquear")
    public ResponseEntity<UsuarioAdminResponseDTO> bloquear(@PathVariable Long id, @RequestBody @Valid BloqueioRequestDTO dto,
                                                            @AuthenticationPrincipal InvestidorPrincipal admin) {
        return ResponseEntity.ok(service.bloquear(id, dto.motivo(), admin.id()));
    }

    @PatchMapping("/{id}/desbloquear")
    public ResponseEntity<UsuarioAdminResponseDTO> desbloquear(@PathVariable Long id) {
        return ResponseEntity.ok(service.desbloquear(id));
    }
}
