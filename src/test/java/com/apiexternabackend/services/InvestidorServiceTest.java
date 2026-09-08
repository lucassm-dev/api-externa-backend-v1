package com.apiexternabackend.services;

import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.dtos.InvestidorResponseDTO;
import com.apiexternabackend.mappers.InvestidorMapper;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestidorServiceTest {

    @Mock
    private InvestidorRepository repository;

    @Mock
    private InvestidorMapper mapper;

    @InjectMocks
    private InvestidorService service;

    private Investidor investidor;
    private InvestidorResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        investidor = new Investidor(1L, "João Silva", "joao@email.com", "12345678901", "hash", LocalDateTime.now(), true);
        responseDTO = new InvestidorResponseDTO(1L, "João Silva", "joao@email.com");
    }

    @Test
    @DisplayName("@spec:AC-054 Listar investidores retorna página com os cadastrados")
    void deveListarInvestidoresPaginados() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Investidor> page = new PageImpl<>(List.of(investidor));
        when(repository.findAllByAtivoTrue(pageable)).thenReturn(page);
        when(mapper.toResponse(investidor)).thenReturn(responseDTO);

        Page<InvestidorResponseDTO> result = service.listar(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("joao@email.com");
    }

    @Test
    @DisplayName("@spec:AC-055 Buscar investidor por id existente retorna seus dados")
    void deveBuscarInvestidorPorId() {
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(investidor));
        when(mapper.toResponse(investidor)).thenReturn(responseDTO);

        InvestidorResponseDTO result = service.buscarPorId(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("@spec:AC-055 Buscar investidor com id inexistente lança não encontrado")
    void deveLancarNotFoundParaIdInexistente() {
        when(repository.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("@spec:AC-504 Buscar investidor excluído por id retorna não encontrado (AUT-003)")
    void deveRetornarNaoEncontradoParaInvestidorExcluido() {
        // investidor existe na tabela, mas com ativo=false — a consulta filtrada não o enxerga
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(1L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .extracting(e -> ((RecursoNaoEncontradoException) e).getCodigo())
                .isEqualTo("AUT-003");
    }

    @Test
    @DisplayName("@spec:AC-422 Investidor ativo é desativado ao excluir")
    void deveDesativarInvestidorAoExcluir() {
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(investidor));

        service.excluir(1L);

        verify(repository).save(investidor);
        assertThat(investidor.getAtivo()).isFalse();
    }

    @Test
    @DisplayName("@spec:AC-423 Excluir investidor inexistente ou inativo lança RecursoNaoEncontradoException")
    void deveLancarNotFoundAoExcluirInvestidorInexistente() {
        when(repository.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
