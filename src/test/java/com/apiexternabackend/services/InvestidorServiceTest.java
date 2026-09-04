package com.apiexternabackend.services;

import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.dtos.InvestidorRequestDTO;
import com.apiexternabackend.domains.dtos.InvestidorResponseDTO;
import com.apiexternabackend.mappers.InvestidorMapper;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.resources.exceptions.RecursoDuplicadoException;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    private InvestidorRequestDTO requestDTO;
    private Investidor investidor;
    private InvestidorResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new InvestidorRequestDTO("João Silva", "joao@email.com", "12345678901");
        investidor = new Investidor(1L, "João Silva", "joao@email.com", "12345678901", true);
        responseDTO = new InvestidorResponseDTO(1L, "João Silva", "joao@email.com");
    }

    @Test
    @DisplayName("@spec:AC-051 Cadastro com nome, e-mail e CPF válidos cria o investidor")
    void deveCadastrarInvestidorComDadosValidos() {
        when(repository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(repository.existsByCpf(requestDTO.getCpf())).thenReturn(false);
        when(mapper.toEntity(requestDTO)).thenReturn(investidor);
        when(repository.save(investidor)).thenReturn(investidor);
        when(mapper.toResponse(investidor)).thenReturn(responseDTO);

        InvestidorResponseDTO result = service.cadastrar(requestDTO);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("joao@email.com");
    }

    @Test
    @DisplayName("@spec:AC-052 E-mail duplicado é rejeitado com mensagem indicando o campo")
    void deveRejeitarEmailDuplicado() {
        when(repository.existsByEmail(requestDTO.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(requestDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("E-mail");
    }

    @Test
    @DisplayName("@spec:AC-052 CPF duplicado é rejeitado com mensagem indicando o campo")
    void deveRejeitarCpfDuplicado() {
        when(repository.existsByEmail(requestDTO.getEmail())).thenReturn(false);
        when(repository.existsByCpf(requestDTO.getCpf())).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(requestDTO))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("CPF");
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
        when(repository.findById(1L)).thenReturn(Optional.of(investidor));
        when(mapper.toResponse(investidor)).thenReturn(responseDTO);

        InvestidorResponseDTO result = service.buscarPorId(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("@spec:AC-055 Buscar investidor com id inexistente lança não encontrado")
    void deveLancarNotFoundParaIdInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("@spec:AC-422 Investidor ativo é desativado ao excluir")
    void deveDesativarInvestidorAoExcluir() {
        Investidor ativo = new Investidor(1L, "João", "joao@email.com", "12345678901", true);
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(ativo));

        service.excluir(1L);

        verify(repository).save(ativo);
        assertThat(ativo.getAtivo()).isFalse();
    }

    @Test
    @DisplayName("@spec:AC-423 Excluir investidor inexistente ou inativo lança RecursoNaoEncontradoException")
    void deveLancarNotFoundAoExcluirInvestidorInexistente() {
        when(repository.findByIdAndAtivoTrue(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
