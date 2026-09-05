package com.apiexternabackend.services;

import com.apiexternabackend.domains.CarteiraAcao;
import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.dtos.CarteiraAcaoResponseDTO;
import com.apiexternabackend.domains.dtos.LucroRealizadoResponseDTO;
import com.apiexternabackend.domains.dtos.OperacaoResponseDTO;
import com.apiexternabackend.domains.enums.TipoOperacao;
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
import java.util.Map;
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

    public LucroRealizadoResponseDTO lucroRealizado(Long carteiraId, Long investidorId) {
        carteiraService.buscarPorId(carteiraId, investidorId); // valida que a carteira é do investidor logado

        List<Operacao> vendas = operacaoRepository.findByCarteiraIdAndTipoAndAtivoTrue(carteiraId, TipoOperacao.VENDA);

        BigDecimal total = vendas.stream()
                .map(Operacao::getLucroRealizado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> porTicker = vendas.stream()
                .collect(Collectors.groupingBy(
                        op -> op.getAcao().getTicker(),
                        Collectors.reducing(BigDecimal.ZERO, Operacao::getLucroRealizado, BigDecimal::add)));

        return new LucroRealizadoResponseDTO(total, porTicker);
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
