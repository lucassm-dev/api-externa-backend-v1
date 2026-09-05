package com.apiexternabackend.services;

import com.apiexternabackend.domains.Carteira;
import com.apiexternabackend.domains.Corretora;
import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.dtos.CarteiraRequestDTO;
import com.apiexternabackend.domains.dtos.CarteiraResponseDTO;
import com.apiexternabackend.domains.enums.Mercado;
import com.apiexternabackend.mappers.CarteiraMapper;
import com.apiexternabackend.repositories.CarteiraRepository;
import com.apiexternabackend.repositories.CorretoraRepository;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarteiraServiceTest {

    @Mock private CarteiraRepository carteiraRepository;
    @Mock private InvestidorRepository investidorRepository;
    @Mock private CorretoraRepository corretoraRepository;
    @Mock private com.apiexternabackend.repositories.CarteiraAcaoRepository carteiraAcaoRepository;
    @Mock private CarteiraMapper mapper;

    @InjectMocks
    private CarteiraService service;

    private static final Long INVESTIDOR_ID = 1L;
    private static final Long OUTRO_INVESTIDOR_ID = 2L;

    private Investidor investidor;
    private Corretora corretora;
    private Carteira carteiraBR;
    private Carteira carteiraUS;
    private CarteiraResponseDTO responseBR;

    @BeforeEach
    void setUp() {
        investidor = new Investidor(INVESTIDOR_ID, "João", "joao@email.com", "12345678901", "hash", LocalDateTime.now(), true);
        corretora = new Corretora();
        corretora.setId(1L);
        corretora.setRazaoSocial("XP");

        carteiraBR = new Carteira(1L, investidor, corretora, Mercado.BR, "Carteira BR", true);
        carteiraUS = new Carteira(2L, investidor, corretora, Mercado.US, "Carteira US", true);

        responseBR = new CarteiraResponseDTO(1L, 1L, 1L, "XP", Mercado.BR, "BRL", "Carteira BR", true);
    }

    @Test
    @DisplayName("@spec:AC-301 Criar carteira vincula ao investidor do token, mercado e corretora")
    void deveCriarCarteira() {
        when(investidorRepository.findById(INVESTIDOR_ID)).thenReturn(Optional.of(investidor));
        when(corretoraRepository.findById(1L)).thenReturn(Optional.of(corretora));
        when(carteiraRepository.save(any())).thenReturn(carteiraBR);
        when(mapper.toResponse(carteiraBR)).thenReturn(responseBR);

        CarteiraResponseDTO result = service.criar(new CarteiraRequestDTO(1L, Mercado.BR, "Carteira BR"), INVESTIDOR_ID);

        assertThat(result.getMercado()).isEqualTo(Mercado.BR);
        assertThat(result.getInvestidorId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("@spec:AC-302 Investidor pode ter múltiplas carteiras")
    void devePermitirMultiplasCarteiras() {
        when(investidorRepository.findById(INVESTIDOR_ID)).thenReturn(Optional.of(investidor));
        when(corretoraRepository.findById(1L)).thenReturn(Optional.of(corretora));
        when(carteiraRepository.save(any())).thenReturn(carteiraBR).thenReturn(carteiraUS);
        when(mapper.toResponse(any())).thenReturn(responseBR);

        service.criar(new CarteiraRequestDTO(1L, Mercado.BR, "Carteira BR"), INVESTIDOR_ID);
        service.criar(new CarteiraRequestDTO(1L, Mercado.US, "Carteira US"), INVESTIDOR_ID);

        verify(carteiraRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    @DisplayName("@spec:AC-303 Criar carteira com corretora inexistente é recusado")
    void deveRejeitarCorretoraNaoEncontrada() {
        when(investidorRepository.findById(INVESTIDOR_ID)).thenReturn(Optional.of(investidor));
        when(corretoraRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(new CarteiraRequestDTO(99L, Mercado.BR, "X"), INVESTIDOR_ID))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Corretora");
    }

    @Test
    @DisplayName("@spec:AC-304 BR totaliza em BRL, US em USD — moedas nunca somadas")
    void deveMercadoDerivaMoeda() {
        when(investidorRepository.findById(INVESTIDOR_ID)).thenReturn(Optional.of(investidor));
        when(corretoraRepository.findById(1L)).thenReturn(Optional.of(corretora));
        when(carteiraRepository.save(any())).thenReturn(carteiraBR);
        CarteiraResponseDTO respBR = new CarteiraResponseDTO(1L, 1L, 1L, "XP", Mercado.BR, "BRL", "BR", true);
        CarteiraResponseDTO respUS = new CarteiraResponseDTO(2L, 1L, 1L, "XP", Mercado.US, "USD", "US", true);
        when(mapper.toResponse(any())).thenReturn(respBR).thenReturn(respUS);

        CarteiraResponseDTO br = service.criar(new CarteiraRequestDTO(1L, Mercado.BR, "BR"), INVESTIDOR_ID);
        when(carteiraRepository.save(any())).thenReturn(carteiraUS);
        CarteiraResponseDTO us = service.criar(new CarteiraRequestDTO(1L, Mercado.US, "US"), INVESTIDOR_ID);

        assertThat(br.getMoeda()).isEqualTo("BRL");
        assertThat(us.getMoeda()).isEqualTo("USD");
    }

    @Test
    @DisplayName("@spec:AC-306 Listagem filtra carteiras pelo id do investidor")
    void deveListarApenasCarteirasDoInvestidor() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(carteiraRepository.findByInvestidorIdAndAtivaTrue(INVESTIDOR_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(carteiraBR)));
        when(mapper.toResponse(carteiraBR)).thenReturn(responseBR);

        Page<CarteiraResponseDTO> result = service.listarPorInvestidor(INVESTIDOR_ID, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getInvestidorId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("@spec:AC-307 Renomear carteira grava novo nome sem afetar outras propriedades")
    void deveRenomearCarteira() {
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteiraBR));
        when(carteiraRepository.save(any())).thenReturn(carteiraBR);
        CarteiraResponseDTO novoNome = new CarteiraResponseDTO(1L, 1L, 1L, "XP", Mercado.BR, "BRL", "Novo Nome", true);
        when(mapper.toResponse(carteiraBR)).thenReturn(novoNome);

        CarteiraResponseDTO result = service.renomear(1L, "Novo Nome", INVESTIDOR_ID);

        assertThat(result.getNome()).isEqualTo("Novo Nome");
        assertThat(carteiraBR.getNome()).isEqualTo("Novo Nome");
    }

    @Test
    @DisplayName("@spec:AC-308 @spec:AC-453 Excluir carteira sem posição ativa marca como inativa — exclusão lógica")
    void deveExcluirLogicamenteCarteira() {
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteiraBR));
        when(carteiraAcaoRepository.countByCarteiraIdAndQuantidadeGreaterThan(1L, 0)).thenReturn(0L);

        service.excluir(1L, INVESTIDOR_ID);

        assertThat(carteiraBR.getAtiva()).isFalse();
        verify(carteiraRepository).save(carteiraBR);
    }

    @Test
    @DisplayName("@spec:AC-452 Excluir carteira com posição ativa é bloqueado")
    void deveBloquearExclusaoDeCarteiraComPosicaoAtiva() {
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteiraBR));
        when(carteiraAcaoRepository.countByCarteiraIdAndQuantidadeGreaterThan(1L, 0)).thenReturn(3L);

        assertThatThrownBy(() -> service.excluir(1L, INVESTIDOR_ID))
                .isInstanceOf(com.apiexternabackend.resources.exceptions.RegraVioladaException.class);

        verify(carteiraRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("@spec:AC-310 Nome de carteira inativa pode ser reutilizado em nova carteira")
    void devePermitirNomeDeInativaEmNovaCarteira() {
        when(investidorRepository.findById(INVESTIDOR_ID)).thenReturn(Optional.of(investidor));
        when(corretoraRepository.findById(1L)).thenReturn(Optional.of(corretora));
        Carteira novaCarteira = new Carteira(4L, investidor, corretora, Mercado.BR, "Teste", true);
        when(carteiraRepository.save(any())).thenReturn(novaCarteira);
        CarteiraResponseDTO resp = new CarteiraResponseDTO(4L, 1L, 1L, "XP", Mercado.BR, "BRL", "Teste", true);
        when(mapper.toResponse(novaCarteira)).thenReturn(resp);

        CarteiraResponseDTO result = service.criar(new CarteiraRequestDTO(1L, Mercado.BR, "Teste"), INVESTIDOR_ID);

        assertThat(result.getNome()).isEqualTo("Teste");
        assertThat(result.getAtiva()).isTrue();
    }

    @Test
    @DisplayName("@spec:AC-006 Carteira de outro investidor não é encontrada ao buscar")
    void deveTratarCarteiraDeOutroInvestidorComoNaoEncontrada() {
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteiraBR));

        assertThatThrownBy(() -> service.buscarAtiva(1L, OUTRO_INVESTIDOR_ID))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("@spec:AC-443 Renomear carteira de outro investidor é recusado como não encontrada")
    void deveRecusarRenomearCarteiraDeOutroInvestidor() {
        when(carteiraRepository.findById(1L)).thenReturn(Optional.of(carteiraBR));

        assertThatThrownBy(() -> service.renomear(1L, "Hack", OUTRO_INVESTIDOR_ID))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}