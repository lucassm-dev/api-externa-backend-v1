package com.apiexternabackend.resources;

import com.apiexternabackend.config.InvestidorPrincipal;
import com.apiexternabackend.domains.dtos.AuthCadastroRequestDTO;
import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.domains.dtos.AuthTokenResponseDTO;
import com.apiexternabackend.domains.dtos.InvestidorResponseDTO;
import com.apiexternabackend.domains.dtos.UsuarioLogadoResponseDTO;
import com.apiexternabackend.services.AutenticacaoService;
import com.apiexternabackend.services.RecuperacaoSenhaService;
import com.apiexternabackend.domains.dtos.EsqueciSenhaRequestDTO;
import com.apiexternabackend.domains.dtos.RedefinirSenhaRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthResource {

    private final AutenticacaoService service;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

    @PostMapping("/cadastro")
    public ResponseEntity<InvestidorResponseDTO> cadastrar(@RequestBody @Valid AuthCadastroRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenResponseDTO> login(@RequestBody @Valid AuthLoginRequestDTO dto) {
        return ResponseEntity.ok(service.login(dto));
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> esqueciSenha(@RequestBody @Valid EsqueciSenhaRequestDTO dto) {
        recuperacaoSenhaService.solicitar(dto.getEmail());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinirSenha(@RequestBody @Valid RedefinirSenhaRequestDTO dto) {
        recuperacaoSenhaService.redefinir(dto.getEmail(), dto.getCodigo(), dto.getNovaSenha());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioLogadoResponseDTO> me(@AuthenticationPrincipal InvestidorPrincipal principal) {
        return ResponseEntity.ok(service.usuarioLogado(principal.id()));
    }
}
