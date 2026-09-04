package com.apiexternabackend.services;

import com.apiexternabackend.config.JwtService;
import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.dtos.AuthCadastroRequestDTO;
import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.domains.dtos.AuthTokenResponseDTO;
import com.apiexternabackend.domains.dtos.InvestidorResponseDTO;
import com.apiexternabackend.mappers.InvestidorMapper;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.resources.exceptions.CredenciaisInvalidasException;
import com.apiexternabackend.resources.exceptions.RecursoDuplicadoException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    private static final Pattern POLITICA_SENHA = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    private static final String MENSAGEM_LOGIN_INVALIDO = "E-mail ou senha inválidos.";

    private final InvestidorRepository repository;
    private final InvestidorMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public InvestidorResponseDTO cadastrar(AuthCadastroRequestDTO dto) {
        if (repository.existsByEmail(dto.getEmail())) {
            throw new RecursoDuplicadoException("AUT-001", "E-mail já está em uso: " + dto.getEmail());
        }
        if (repository.existsByCpf(dto.getCpf())) {
            throw new RecursoDuplicadoException("AUT-002", "CPF já está em uso: " + dto.getCpf());
        }
        if (!POLITICA_SENHA.matcher(dto.getSenha()).matches()) {
            throw new RegraVioladaException("AUT-008",
                    "Senha deve ter no mínimo 8 caracteres, com pelo menos uma letra e um número.");
        }

        Investidor investidor = new Investidor();
        investidor.setNome(dto.getNome());
        investidor.setEmail(dto.getEmail());
        investidor.setCpf(dto.getCpf());
        investidor.setSenha(passwordEncoder.encode(dto.getSenha()));
        investidor.setCriadoEm(LocalDateTime.now());

        return mapper.toResponse(repository.save(investidor));
    }

    public AuthTokenResponseDTO login(AuthLoginRequestDTO dto) {
        Investidor investidor = repository.findByEmail(dto.getEmail())
                .filter(i -> passwordEncoder.matches(dto.getSenha(), i.getSenha()))
                .orElseThrow(() -> new CredenciaisInvalidasException("AUT-004", MENSAGEM_LOGIN_INVALIDO));

        String token = jwtService.gerar(investidor.getId(), investidor.getEmail());
        return new AuthTokenResponseDTO(token, "Bearer", jwtService.expiracaoDe(token));
    }
}