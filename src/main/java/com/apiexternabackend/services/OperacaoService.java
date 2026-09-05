package com.apiexternabackend.services;

import com.apiexternabackend.domains.Acao;
import com.apiexternabackend.domains.Carteira;
import com.apiexternabackend.domains.CarteiraAcao;
import com.apiexternabackend.domains.Operacao;
import com.apiexternabackend.domains.dtos.OperacaoRequestDTO;
import com.apiexternabackend.domains.dtos.OperacaoResponseDTO;
import com.apiexternabackend.domains.enums.TipoOperacao;
import com.apiexternabackend.infra.adapter.CotacaoResultado;
import com.apiexternabackend.mappers.OperacaoMapper;
import com.apiexternabackend.repositories.AcaoRepository;
import com.apiexternabackend.repositories.CarteiraAcaoRepository;
import com.apiexternabackend.repositories.OperacaoRepository;
import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperacaoService {

    private final OperacaoRepository operacaoRepository;
    private final AcaoRepository acaoRepository;
    private final CarteiraAcaoRepository carteiraAcaoRepository;
    private final CarteiraService carteiraService;
    private final PosicaoService posicaoService;
    private final OperacaoMapper mapper;
    private final CotacaoCacheService cotacaoCacheService;
    private final CambioCacheService cambioCacheService;

    @Transactional
    public OperacaoResponseDTO comprar(OperacaoRequestDTO dto, Long investidorId) {
        Carteira carteira = carteiraService.buscarAtiva(dto.getCarteiraId(), investidorId); // AC-309
        Acao acao = buscarAcao(dto.getTicker());

        // AC-497 (Q-MAP-09): carteira aceita ações BR e US juntas — sem checagem de mercado

        CotacaoObtida obtida = obterCotacaoComFallback(acao); // AC-477/AC-478/AC-482/AC-483/AC-484
        CotacaoResultado cotacao = obtida.resultado();
        boolean precoManual = dto.getPrecoUnitario() != null; // AC-457
        BigDecimal precoEfetivo = precoManual ? dto.getPrecoUnitario() : cotacao.preco(); // AC-456
        validarEscalaDecimal(precoEfetivo); // AC-459

        Operacao operacao = new Operacao();
        operacao.setCarteira(carteira);
        operacao.setAcao(acao);
        operacao.setTipo(TipoOperacao.COMPRA);
        operacao.setQuantidade(dto.getQuantidade());
        operacao.setPrecoUnitario(precoEfetivo);
        operacao.setPrecoManual(precoManual);
        operacao.setCotacaoNoMomento(cotacao.preco()); // AC-461
        operacao.setDataHora(LocalDateTime.now());
        CambioCacheService.CambioObtido cambioObtido = aplicarTaxaCambio(operacao, acao); // AC-486/AC-487

        operacao = operacaoRepository.save(operacao); // AC-407
        posicaoService.recalcular(carteira, acao);    // AC-402

        OperacaoResponseDTO response = comAvisoDeCotacaoDesatualizada(mapper.toResponse(operacao), obtida);
        return comAvisoDeCambioDesatualizado(response, cambioObtido);
    }

    @Transactional
    public OperacaoResponseDTO vender(OperacaoRequestDTO dto, Long investidorId) {
        Carteira carteira = carteiraService.buscarAtiva(dto.getCarteiraId(), investidorId); // AC-309
        Acao acao = buscarAcao(dto.getTicker());

        // AC-497 (Q-MAP-09): carteira aceita ações BR e US juntas — sem checagem de mercado

        // AC-404: não vende mais que a posição atual
        CarteiraAcao posicao = carteiraAcaoRepository
                .findByCarteiraIdAndAcaoId(carteira.getId(), acao.getId())
                .orElseThrow(() -> new RegraVioladaException("OPE-003",
                        "Sem posição em " + dto.getTicker() + " nesta carteira"));

        if (dto.getQuantidade() > posicao.getQuantidade()) {
            throw new RegraVioladaException("OPE-004",
                    "Quantidade excede a posição atual (" + posicao.getQuantidade() + " unidades)");
        }

        CotacaoObtida obtida = obterCotacaoComFallback(acao); // AC-477/AC-478/AC-482/AC-483/AC-484
        CotacaoResultado cotacao = obtida.resultado();
        boolean precoManual = dto.getPrecoUnitario() != null; // AC-457
        BigDecimal precoEfetivo = precoManual ? dto.getPrecoUnitario() : cotacao.preco(); // AC-456
        validarEscalaDecimal(precoEfetivo); // AC-459

        Operacao operacao = new Operacao();
        operacao.setCarteira(carteira);
        operacao.setAcao(acao);
        operacao.setTipo(TipoOperacao.VENDA);
        operacao.setQuantidade(dto.getQuantidade());
        operacao.setPrecoUnitario(precoEfetivo);
        operacao.setPrecoManual(precoManual);
        operacao.setCotacaoNoMomento(cotacao.preco()); // AC-461
        operacao.setDataHora(LocalDateTime.now());
        CambioCacheService.CambioObtido cambioObtido = aplicarTaxaCambio(operacao, acao); // AC-486/AC-487

        operacao = operacaoRepository.save(operacao); // AC-407
        posicaoService.recalcular(carteira, acao);    // AC-405 (zera posição se necessário)

        OperacaoResponseDTO response = comAvisoDeCotacaoDesatualizada(mapper.toResponse(operacao), obtida);
        return comAvisoDeCambioDesatualizado(response, cambioObtido);
    }

    @Transactional
    public OperacaoResponseDTO editar(Long id, Integer novaQuantidade, java.math.BigDecimal novoPreco, Long investidorId) {
        Operacao operacao = buscarOperacao(id, investidorId);

        if (novaQuantidade != null) operacao.setQuantidade(novaQuantidade);
        if (novoPreco != null) {
            validarEscalaDecimal(novoPreco); // AC-464
            operacao.setPrecoUnitario(novoPreco);
            operacao.setPrecoManual(true);
            operacao.setCotacaoNoMomento(operacao.getAcao().getCotacaoAtual()); // AC-465, sem nova chamada externa (ASM-419)
        }

        operacao = operacaoRepository.save(operacao);
        posicaoService.recalcular(operacao.getCarteira(), operacao.getAcao()); // AC-412

        return mapper.toResponse(operacao);
    }

    @Transactional
    public void excluir(Long id, Long investidorId) {
        Operacao operacao = buscarOperacao(id, investidorId);
        Carteira carteira = operacao.getCarteira();
        Acao acao = operacao.getAcao();

        operacao.setAtivo(false); // AC-472: soft delete — mantém rastreabilidade
        operacaoRepository.save(operacao);
        posicaoService.recalcular(carteira, acao); // AC-413/AC-474
    }

    private Operacao buscarOperacao(Long id, Long investidorId) {
        Operacao operacao = operacaoRepository.findByIdAndAtivoTrue(id) // AC-473: excluída não é reoperável
                .orElseThrow(() -> new RecursoNaoEncontradoException("OPE-001", "Operação não encontrada: " + id));
        if (!operacao.getCarteira().getInvestidor().getId().equals(investidorId)) {
            // não revela que a operação existe e é de outro investidor (mesma semântica de CAR-001/ASM-411)
            throw new RecursoNaoEncontradoException("OPE-001", "Operação não encontrada: " + id);
        }
        return operacao;
    }

    private Acao buscarAcao(String ticker) {
        return acaoRepository.findByTickerAndAtivoTrue(ticker.toUpperCase().trim())
                .orElseThrow(() -> new RecursoNaoEncontradoException("ACA-001", "Ação não encontrada: " + ticker));
    }

    private void validarEscalaDecimal(BigDecimal preco) {
        if (preco.stripTrailingZeros().scale() > 2) {
            throw new RegraVioladaException("OPE-005",
                    "Preço unitário deve ter no máximo 2 casas decimais: " + preco);
        }
    }

    /**
     * Q-MAP-06: cota estourada ou fonte indisponível em compra/venda não bloqueia —
     * prossegue com a última cotação conhecida (AC-482/AC-483). Sem cotação salva
     * alguma vez, não há fallback possível (AC-484).
     */
    private CotacaoObtida obterCotacaoComFallback(Acao acao) {
        try {
            return new CotacaoObtida(cotacaoCacheService.obter(acao, false), false);
        } catch (IntegracaoExternaException e) {
            if (acao.getCotacaoAtual() == null || acao.getDataHoraCotacao() == null) {
                throw e; // AC-484
            }
            log.warn("Cotação de {} indisponível/cota excedida ({}). Prosseguindo com última cotação conhecida.",
                    acao.getTicker(), e.getMessage());
            return new CotacaoObtida(new CotacaoResultado(acao.getCotacaoAtual(), acao.getDataHoraCotacao()), true);
        }
    }

    private OperacaoResponseDTO comAvisoDeCotacaoDesatualizada(OperacaoResponseDTO response, CotacaoObtida obtida) {
        if (!obtida.desatualizada()) {
            return response;
        }
        List<String> avisos = new ArrayList<>(response.getAvisos());
        avisos.add("Cotação pode estar desatualizada — fonte externa indisponível ou com cota excedida no momento da operação; usando último valor conhecido de "
                + obtida.resultado().dataHora() + ".");
        response.setAvisos(avisos);
        return response;
    }

    private record CotacaoObtida(CotacaoResultado resultado, boolean desatualizada) {
    }

    /**
     * Q-MAP-10: grava a taxa de câmbio do momento em operações de ativo USD;
     * ativos BRL usam taxa 1 — cálculo uniforme, sem caminho especial (AC-487).
     * Retorna null quando não houve busca de câmbio (ativo BRL, sem aviso possível).
     */
    private CambioCacheService.CambioObtido aplicarTaxaCambio(Operacao operacao, Acao acao) {
        if (!"USD".equals(acao.getMoeda())) {
            operacao.setTaxaCambioNaOperacao(BigDecimal.ONE);
            operacao.setDataHoraTaxaCambio(operacao.getDataHora());
            return null;
        }
        CambioCacheService.CambioObtido obtido = cambioCacheService.obterTaxaAtual(); // AC-486/AC-488/AC-489/AC-490
        operacao.setTaxaCambioNaOperacao(obtido.resultado().taxa());
        operacao.setDataHoraTaxaCambio(obtido.resultado().dataHora());
        return obtido;
    }

    private OperacaoResponseDTO comAvisoDeCambioDesatualizado(OperacaoResponseDTO response, CambioCacheService.CambioObtido obtido) {
        if (obtido == null || !obtido.desatualizado()) {
            return response;
        }
        List<String> avisos = new ArrayList<>(response.getAvisos());
        avisos.add("Taxa de câmbio USD-BRL pode estar desatualizada — fontes externas indisponíveis no momento da operação; usando última taxa conhecida de "
                + obtido.resultado().dataHora() + ".");
        response.setAvisos(avisos);
        return response;
    }
}
