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

    public CarteiraResponseDTO criar(CarteiraRequestDTO dto) {
        Investidor investidor = investidorRepository.findById(dto.getInvestidorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("AUT-003", "Investidor não encontrado: " + dto.getInvestidorId()));

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

    public CarteiraResponseDTO renomear(Long id, String novoNome) {
        Carteira carteira = buscarAtiva(id);
        carteira.setNome(novoNome);
        return mapper.toResponse(carteiraRepository.save(carteira));
    }

    public void excluir(Long id) {
        Carteira carteira = buscarAtiva(id);
        carteira.setAtiva(false);
        carteiraRepository.save(carteira);
    }

    public Carteira buscarAtiva(Long id) {
        return carteiraRepository.findById(id)
                .filter(Carteira::getAtiva)
                .orElseThrow(() -> new RecursoNaoEncontradoException("CAR-001", "Carteira não encontrada ou inativa: " + id));
    }

    public Carteira buscarPorId(Long id) {
        return carteiraRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("CAR-001", "Carteira não encontrada: " + id));
    }
}
