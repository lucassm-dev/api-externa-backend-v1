package com.apiexternabackend.services;

import com.apiexternabackend.domains.CarteiraAcao;
import com.apiexternabackend.domains.dtos.CarteiraAcaoResponseDTO;
import com.apiexternabackend.domains.dtos.OperacaoResponseDTO;
import com.apiexternabackend.mappers.CarteiraAcaoMapper;
import com.apiexternabackend.mappers.OperacaoMapper;
import com.apiexternabackend.repositories.CarteiraAcaoRepository;
import com.apiexternabackend.repositories.OperacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsultaOperacaoService {

    private final OperacaoRepository operacaoRepository;
    private final CarteiraAcaoRepository carteiraAcaoRepository;
    private final OperacaoMapper operacaoMapper;
    private final CarteiraAcaoMapper carteiraAcaoMapper;
    private final CarteiraService carteiraService;

    public Page<OperacaoResponseDTO> historico(Long investidorId, Pageable pageable) {
        return operacaoRepository.findByInvestidorId(investidorId, pageable)
                .map(operacaoMapper::toResponse);
    }

    public List<CarteiraAcaoResponseDTO> posicoes(Long carteiraId, Long investidorId) {
        carteiraService.buscarPorId(carteiraId, investidorId); // valida que a carteira é do investidor logado
        return carteiraAcaoRepository.findByCarteiraId(carteiraId).stream()
                .map(pos -> {
                    CarteiraAcaoResponseDTO dto = carteiraAcaoMapper.toResponse(pos);
                    dto.setRentabilidadeNaoRealizada(calcularRentabilidade(pos));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calcularRentabilidade(CarteiraAcao pos) {
        if (pos.getAcao().getCotacaoAtual() == null || pos.getPrecoMedio() == null) {
            return BigDecimal.ZERO;
        }
        // RN-P05: (cotação atual − preço médio) × quantidade
        return pos.getAcao().getCotacaoAtual()
                .subtract(pos.getPrecoMedio())
                .multiply(BigDecimal.valueOf(pos.getQuantidade()));
    }
}
