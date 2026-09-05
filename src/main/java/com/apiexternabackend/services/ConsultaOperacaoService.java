package com.apiexternabackend.services;

import com.apiexternabackend.domains.CarteiraAcao;
import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.dtos.CarteiraAcaoResponseDTO;
import com.apiexternabackend.domains.dtos.CarteiraConsolidadaResponseDTO;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final CambioCacheService cambioCacheService;

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

        // AC-496 (Q-MAP-09+Q-MAP-10): soma em BRL — a carteira pode ter vendas em BRL e USD juntas,
        // somar o valor bruto misturaria moedas
        BigDecimal total = vendas.stream()
                .map(Operacao::getLucroRealizadoBrl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> porTicker = vendas.stream()
                .collect(Collectors.groupingBy(
                        op -> op.getAcao().getTicker(),
                        Collectors.reducing(BigDecimal.ZERO, Operacao::getLucroRealizadoBrl, BigDecimal::add)));

        return new LucroRealizadoResponseDTO(total, porTicker);
    }

    public CarteiraConsolidadaResponseDTO consolidado(Long carteiraId, Long investidorId) {
        carteiraService.buscarPorId(carteiraId, investidorId); // valida que a carteira é do investidor logado

        List<CarteiraAcao> posicoes = carteiraAcaoRepository.findByCarteiraId(carteiraId);

        BigDecimal valorInvestido = posicoes.stream() // AC-493: câmbio histórico, já acumulado por PosicaoService
                .map(CarteiraAcao::getCustoTotalBrl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean temPosicaoUsd = posicoes.stream().anyMatch(p -> "USD".equals(p.getAcao().getMoeda()));
        CambioCacheService.CambioObtido cambioObtido = temPosicaoUsd ? cambioCacheService.obterTaxaAtual() : null;
        BigDecimal taxaAtual = cambioObtido != null ? cambioObtido.resultado().taxa() : BigDecimal.ONE; // AC-495
        LocalDateTime dataHoraTaxa = cambioObtido != null ? cambioObtido.resultado().dataHora() : LocalDateTime.now();

        BigDecimal valorDeMercado = posicoes.stream() // AC-493: câmbio atual
                .map(p -> {
                    BigDecimal cotacao = p.getAcao().getCotacaoAtual() != null ? p.getAcao().getCotacaoAtual() : BigDecimal.ZERO;
                    BigDecimal taxa = "USD".equals(p.getAcao().getMoeda()) ? taxaAtual : BigDecimal.ONE; // AC-495
                    return cotacao.multiply(BigDecimal.valueOf(p.getQuantidade())).multiply(taxa);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CarteiraConsolidadaResponseDTO dto = new CarteiraConsolidadaResponseDTO(
                valorInvestido, valorDeMercado, valorDeMercado.subtract(valorInvestido),
                taxaAtual, dataHoraTaxa, new ArrayList<>()); // AC-494

        if (cambioObtido != null && cambioObtido.desatualizado()) {
            dto.getAvisos().add("Taxa de câmbio USD-BRL pode estar desatualizada — fontes externas indisponíveis; usando última taxa conhecida de "
                    + cambioObtido.resultado().dataHora() + ".");
        }
        return dto;
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
