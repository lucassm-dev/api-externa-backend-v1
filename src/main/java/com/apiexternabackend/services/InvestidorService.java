package com.apiexternabackend.services;

import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.dtos.InvestidorResponseDTO;
import com.apiexternabackend.mappers.InvestidorMapper;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvestidorService {

    private final InvestidorRepository repository;
    private final InvestidorMapper mapper;

    public Page<InvestidorResponseDTO> listar(Pageable pageable) {
        return repository.findAllByAtivoTrue(pageable).map(mapper::toResponse);
    }

    public void excluir(Long id) {
        Investidor investidor = repository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("AUT-003", "Investidor não encontrado: " + id));
        investidor.setAtivo(false);
        repository.save(investidor);
    }

    public InvestidorResponseDTO buscarPorId(Long id) {
        Investidor investidor = repository.findByIdAndAtivoTrue(id) // AC-504: excluído não vaza na busca individual
                .orElseThrow(() -> new RecursoNaoEncontradoException("AUT-003", "Investidor não encontrado: " + id));
        return mapper.toResponse(investidor);
    }
}