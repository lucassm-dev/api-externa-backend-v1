package com.apiexternabackend.services;

import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.dtos.InvestidorResponseDTO;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.mappers.InvestidorMapper;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvestidorService {

    private final InvestidorRepository repository;
    private final InvestidorMapper mapper;

    public Page<InvestidorResponseDTO> listar(Long investidorId, Pageable pageable) {
        return repository.findByIdAndAtivoTrue(investidorId, pageable).map(mapper::toResponse);
    }

    @Transactional
    public void excluir(Long id, Long investidorId) {
        Investidor investidor = buscarDoProprio(id, investidorId);
        investidor.setAtivo(false);
        // AC-555: encerra o acesso e libera o e-mail para reuso (SPEC-10)
        investidor.getUsuario().setSituacao(SituacaoUsuario.EXCLUIDO);
        repository.save(investidor);
    }

    public InvestidorResponseDTO buscarPorId(Long id, Long investidorId) {
        return mapper.toResponse(buscarDoProprio(id, investidorId));
    }

    // AC-504: excluído não vaza; AC-543/AC-544: conta de outro responde igual a inexistente (ASM-458)
    private Investidor buscarDoProprio(Long id, Long investidorId) {
        return repository.findByIdAndAtivoTrue(id)
                .filter(investidor -> investidor.getId().equals(investidorId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("AUT-003", "Investidor não encontrado: " + id));
    }
}