package com.apiexternabackend.services;

import com.apiexternabackend.domains.Carteira;
import com.apiexternabackend.domains.Corretora;
import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.dtos.CarteiraRequestDTO;
import com.apiexternabackend.domains.dtos.CarteiraResponseDTO;
import com.apiexternabackend.mappers.CarteiraMapper;
import com.apiexternabackend.repositories.CarteiraRepository;
import com.apiexternabackend.repositories.CorretoraRepository;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarteiraService {

    private final CarteiraRepository carteiraRepository;
    private final InvestidorRepository investidorRepository;
    private final CorretoraRepository corretoraRepository;
    private final CarteiraMapper mapper;

    public CarteiraResponseDTO criar(CarteiraRequestDTO dto, Long investidorId) {
        Investidor investidor = investidorRepository.findById(investidorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("AUT-003", "Investidor não encontrado: " + investidorId));

        Corretora corretora = corretoraRepository.findById(dto.getCorretoraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("COR-001", "Corretora não encontrada: " + dto.getCorretoraId()));

        Carteira carteira = new Carteira();
        carteira.setInvestidor(investidor);
        carteira.setCorretora(corretora);
        carteira.setMercado(dto.getMercado());
        carteira.setNome(dto.getNome());
        carteira.setAtiva(true);

        return mapper.toResponse(carteiraRepository.save(carteira));
    }

    public Page<CarteiraResponseDTO> listarPorInvestidor(Long investidorId, Pageable pageable) {
        return carteiraRepository.findByInvestidorIdAndAtivaTrue(investidorId, pageable)
                .map(mapper::toResponse);
    }

    public CarteiraResponseDTO renomear(Long id, String novoNome, Long investidorId) {
        Carteira carteira = buscarAtiva(id, investidorId);
        carteira.setNome(novoNome);
        return mapper.toResponse(carteiraRepository.save(carteira));
    }

    public void excluir(Long id, Long investidorId) {
        Carteira carteira = buscarAtiva(id, investidorId);
        carteira.setAtiva(false);
        carteiraRepository.save(carteira);
    }

    public Carteira buscarAtiva(Long id, Long investidorId) {
        Carteira carteira = carteiraRepository.findById(id)
                .filter(Carteira::getAtiva)
                .orElseThrow(() -> new RecursoNaoEncontradoException("CAR-001", "Carteira não encontrada ou inativa: " + id));
        return validarDono(carteira, investidorId);
    }

    public Carteira buscarPorId(Long id, Long investidorId) {
        Carteira carteira = carteiraRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("CAR-001", "Carteira não encontrada: " + id));
        return validarDono(carteira, investidorId);
    }

    /**
     * Carteira de outro investidor responde como "não encontrada" (mesmo código/mensagem),
     * não "acesso negado" — evita que B descubra que a carteira de A existe (ASM-411).
     */
    private Carteira validarDono(Carteira carteira, Long investidorId) {
        if (!carteira.getInvestidor().getId().equals(investidorId)) {
            throw new RecursoNaoEncontradoException("CAR-001", "Carteira não encontrada ou inativa: " + carteira.getId());
        }
        return carteira;
    }
}