package com.apiexternabackend.services;

import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.dtos.InvestidorRequestDTO;
import com.apiexternabackend.domains.dtos.InvestidorResponseDTO;
import com.apiexternabackend.mappers.InvestidorMapper;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.resources.exceptions.DuplicateResourceException;
import com.apiexternabackend.resources.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvestidorService {

    private final InvestidorRepository repository;
    private final InvestidorMapper mapper;

    public InvestidorResponseDTO cadastrar(InvestidorRequestDTO dto) {
        if (repository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("E-mail já está em uso: " + dto.getEmail());
        }
        if (repository.existsByCpf(dto.getCpf())) {
            throw new DuplicateResourceException("CPF já está em uso: " + dto.getCpf());
        }
        Investidor salvo = repository.save(mapper.toEntity(dto));
        return mapper.toResponse(salvo);
    }

    public Page<InvestidorResponseDTO> listar(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toResponse);
    }

    public InvestidorResponseDTO buscarPorId(Long id) {
        Investidor investidor = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investidor não encontrado: " + id));
        return mapper.toResponse(investidor);
    }
}
