package com.apiexternabackend.services;

import com.apiexternabackend.domains.Corretora;
import com.apiexternabackend.domains.dtos.CorretoraRequestDTO;
import com.apiexternabackend.domains.dtos.CorretoraResponseDTO;
import com.apiexternabackend.infra.client.cnpj.dto.CnpjResponseDTO;
import com.apiexternabackend.infra.client.cep.dto.CepResponseDTO;
import com.apiexternabackend.infra.facade.CnpjFacade;
import com.apiexternabackend.infra.facade.CepFacade;
import com.apiexternabackend.infra.facade.CvmFacade;
import com.apiexternabackend.infra.facade.CvmFacade.ResultadoVerificacaoCvm;
import com.apiexternabackend.mappers.CorretoraMapper;
import com.apiexternabackend.repositories.CorretoraRepository;
import com.apiexternabackend.resources.exceptions.IntegracaoExternaException;
import com.apiexternabackend.resources.exceptions.RecursoDuplicadoException;
import com.apiexternabackend.resources.exceptions.RecursoNaoEncontradoException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CorretoraServiceTest {

    // CNPJ válido: XP Investimentos — dígitos verificadores corretos
    private static final String CNPJ_VALIDO = "02332886000104";
    private static final String CNPJ_INVALIDO_FORMATO = "1234";
    private static final String CNPJ_INVALIDO_DIGITO = "12345678000100";

    @Mock private CorretoraRepository repository;
    @Mock private com.apiexternabackend.repositories.CarteiraRepository carteiraRepository;
    @Mock private CorretoraMapper mapper;
    @Mock private CnpjFacade cnpjFacade;
    @Mock private CepFacade cepFacade;
    @Mock private CvmFacade cvmFacade;

    @InjectMocks
    private CorretoraService service;

    private CnpjResponseDTO cnpjResponse;
    private CepResponseDTO cepResponse;
    private Corretora corretora;
    private CorretoraResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        lenient().when(cnpjFacade.normalizar(anyString())).thenAnswer(inv -> inv.getArgument(0));

        cnpjResponse = new CnpjResponseDTO();
        cnpjResponse.setRazaoSocial("XP INVESTIMENTOS");
        cnpjResponse.setSituacaoCadastral("ATIVA");

        cepResponse = new CepResponseDTO();
        cepResponse.setCep("04538-133");
        cepResponse.setLocalidade("São Paulo");
        cepResponse.setUf("SP");

        corretora = new Corretora();
        corretora.setId(1L);
        corretora.setCnpj(CNPJ_VALIDO);

        responseDTO = new CorretoraResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setCnpj(CNPJ_VALIDO);
        responseDTO.setDataBaseCvm(LocalDate.now());
    }

    @Test
    @DisplayName("@spec:AC-101 CNPJ mal formatado é rejeitado antes de qualquer chamada externa")
    void deveRejeitarCnpjMalFormatadoSemChamarFonteExterna() {
        doThrow(new RegraVioladaException("COR-003", "CNPJ inválido")).when(cnpjFacade).validar(any());

        assertThatThrownBy(() -> service.cadastrar(new CorretoraRequestDTO(CNPJ_INVALIDO_FORMATO)))
                .isInstanceOf(RegraVioladaException.class);

        verify(cnpjFacade, never()).buscar(anyString());
        verify(cvmFacade, never()).verificar(anyString());
        verify(cepFacade, never()).buscar(anyString());
    }

    @Test
    @DisplayName("@spec:AC-101 CNPJ com dígitos verificadores inválidos é rejeitado antes de chamadas externas")
    void deveRejeitarCnpjComDigitosInvalidos() {
        doThrow(new RegraVioladaException("COR-003", "CNPJ com dígitos verificadores inválidos")).when(cnpjFacade).validar(any());

        assertThatThrownBy(() -> service.cadastrar(new CorretoraRequestDTO(CNPJ_INVALIDO_DIGITO)))
                .isInstanceOf(RegraVioladaException.class);

        verify(cnpjFacade, never()).buscar(anyString());
    }

    @Test
    @DisplayName("@spec:AC-419 CorretoraService delega validação ao CnpjFacade sem reimplementar")
    void deveDelegarValidacaoAoCnpjFacade() {
        when(repository.existsByCnpjAndAtivoTrue(any())).thenReturn(false);
        when(cnpjFacade.buscar(any())).thenReturn(cnpjResponse);
        when(cvmFacade.verificar(any())).thenReturn(ResultadoVerificacaoCvm.autorizada(LocalDate.now()));
        when(repository.save(any())).thenReturn(corretora);
        when(mapper.toResponse(corretora)).thenReturn(responseDTO);

        service.cadastrar(new CorretoraRequestDTO(CNPJ_VALIDO));

        verify(cnpjFacade).validar(any());
        verify(cnpjFacade).normalizar(CNPJ_VALIDO);
    }

    @Test
    @DisplayName("@spec:AC-420 Corretora ativa é desativada ao excluir")
    void deveDesativarCorretoraAoExcluir() {
        Corretora ativa = new Corretora();
        ativa.setId(1L);
        ativa.setAtivo(true);
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(java.util.Optional.of(ativa));
        when(carteiraRepository.countByCorretoraIdAndAtivaTrue(1L)).thenReturn(0L);

        service.excluir(1L);

        verify(repository).save(ativa);
        assertThat(ativa.getAtivo()).isFalse();
    }

    @Test
    @DisplayName("@spec:AC-421 Excluir corretora inexistente ou inativa lança RecursoNaoEncontradoException")
    void deveLancarNotFoundAoExcluirCorretoraInexistente() {
        when(repository.findByIdAndAtivoTrue(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.excluir(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("@spec:AC-454 Excluir corretora com carteira ativa vinculada é bloqueado")
    void deveBloquearExclusaoComCarteiraAtivaVinculada() {
        Corretora ativa = new Corretora();
        ativa.setId(1L);
        ativa.setAtivo(true);
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(java.util.Optional.of(ativa));
        when(carteiraRepository.countByCorretoraIdAndAtivaTrue(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.excluir(1L))
                .isInstanceOf(com.apiexternabackend.resources.exceptions.RegraVioladaException.class);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("@spec:AC-455 Excluir corretora sem carteira ativa vinculada é permitido")
    void devePermitirExclusaoSemCarteiraVinculada() {
        Corretora ativa = new Corretora();
        ativa.setId(1L);
        ativa.setAtivo(true);
        when(repository.findByIdAndAtivoTrue(1L)).thenReturn(java.util.Optional.of(ativa));
        when(carteiraRepository.countByCorretoraIdAndAtivaTrue(1L)).thenReturn(0L);

        service.excluir(1L);

        verify(repository).save(ativa);
    }

    @Test
    @DisplayName("@spec:AC-445 Recadastrar CNPJ de corretora excluída funciona (verificação de duplicidade só considera ativas)")
    void deveRecadastrarCnpjDeCorretoraExcluida() {
        when(repository.existsByCnpjAndAtivoTrue(CNPJ_VALIDO)).thenReturn(false);
        when(cnpjFacade.buscar(any())).thenReturn(cnpjResponse);
        when(cvmFacade.verificar(any())).thenReturn(ResultadoVerificacaoCvm.autorizada(LocalDate.now()));
        when(repository.save(any())).thenReturn(corretora);
        when(mapper.toResponse(corretora)).thenReturn(responseDTO);

        service.cadastrar(new CorretoraRequestDTO(CNPJ_VALIDO));

        verify(repository).existsByCnpjAndAtivoTrue(CNPJ_VALIDO);
    }

    @Test
    @DisplayName("@spec:AC-433 Corretora não encontrada traz o código COR-001 do catálogo")
    void deveTrazerCodigoCor001AoNaoEncontrarCorretora() {
        when(repository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .extracting(e -> ((RecursoNaoEncontradoException) e).getCodigo())
                .isEqualTo("COR-001");
    }

    @Test
    @DisplayName("@spec:AC-102 CNPJ válido busca dados cadastrais e endereço e persiste a corretora")
    void deveBuscarDadosEPersistirCorretoraValida() {
        when(repository.existsByCnpjAndAtivoTrue(CNPJ_VALIDO)).thenReturn(false);
        when(cnpjFacade.buscar(CNPJ_VALIDO)).thenReturn(cnpjResponse);
        when(cvmFacade.verificar(CNPJ_VALIDO)).thenReturn(ResultadoVerificacaoCvm.autorizada(LocalDate.now()));
        when(repository.save(any())).thenReturn(corretora);
        when(mapper.toResponse(corretora)).thenReturn(responseDTO);

        CorretoraResponseDTO result = service.cadastrar(new CorretoraRequestDTO(CNPJ_VALIDO));

        assertThat(result.getCnpj()).isEqualTo(CNPJ_VALIDO);
        assertThat(result.getDataBaseCvm()).isNotNull();
    }

    @Test
    @DisplayName("@spec:AC-103 CNPJ não encontrado na Receita impede o cadastro")
    void deveRejeitarCnpjNaoEncontradoNaReceita() {
        when(repository.existsByCnpjAndAtivoTrue(CNPJ_VALIDO)).thenReturn(false);
        when(cnpjFacade.buscar(CNPJ_VALIDO))
                .thenThrow(new RegraVioladaException("COR-003", "CNPJ não encontrado na base da Receita"));

        assertThatThrownBy(() -> service.cadastrar(new CorretoraRequestDTO(CNPJ_VALIDO)))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessageContaining("Receita");
    }

    @Test
    @DisplayName("@spec:AC-104 @spec:AC-446 CNPJ duplicado entre ativos é impedido")
    void deveRejeitarCnpjDuplicado() {
        when(repository.existsByCnpjAndAtivoTrue(CNPJ_VALIDO)).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(new CorretoraRequestDTO(CNPJ_VALIDO)))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    @DisplayName("@spec:AC-433 CNPJ duplicado traz o código COR-002 do catálogo")
    void deveTrazerCodigoCor002AoDuplicarCnpj() {
        when(repository.existsByCnpjAndAtivoTrue(CNPJ_VALIDO)).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(new CorretoraRequestDTO(CNPJ_VALIDO)))
                .isInstanceOf(RecursoDuplicadoException.class)
                .extracting(e -> ((RecursoDuplicadoException) e).getCodigo())
                .isEqualTo("COR-002");
    }

    @Test
    @DisplayName("@spec:AC-105 Corretora não autorizada na CVM não é cadastrada")
    void deveRejeitarCorretoraInautorizada() {
        when(repository.existsByCnpjAndAtivoTrue(CNPJ_VALIDO)).thenReturn(false);
        when(cnpjFacade.buscar(CNPJ_VALIDO)).thenReturn(cnpjResponse);
        when(cvmFacade.verificar(CNPJ_VALIDO))
                .thenReturn(ResultadoVerificacaoCvm.naoAutorizada(LocalDate.now(), "Corretora não autorizada na CVM"));

        assertThatThrownBy(() -> service.cadastrar(new CorretoraRequestDTO(CNPJ_VALIDO)))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessageContaining("não autorizada");
    }

    @Test
    @DisplayName("@spec:AC-433 Corretora não autorizada na CVM traz o código COR-003 do catálogo")
    void deveTrazerCodigoCor003AoNaoAutorizar() {
        when(repository.existsByCnpjAndAtivoTrue(CNPJ_VALIDO)).thenReturn(false);
        when(cnpjFacade.buscar(CNPJ_VALIDO)).thenReturn(cnpjResponse);
        when(cvmFacade.verificar(CNPJ_VALIDO))
                .thenReturn(ResultadoVerificacaoCvm.naoAutorizada(LocalDate.now(), "Corretora não autorizada na CVM"));

        assertThatThrownBy(() -> service.cadastrar(new CorretoraRequestDTO(CNPJ_VALIDO)))
                .isInstanceOf(RegraVioladaException.class)
                .extracting(e -> ((RegraVioladaException) e).getCodigo())
                .isEqualTo("COR-003");
    }

    @Test
    @DisplayName("@spec:AC-106 Falha ao verificar CVM retorna mensagem de falha, não de reprovada")
    void deveMensagemDeFalhaQuandoCvmIndisponivel() {
        when(repository.existsByCnpjAndAtivoTrue(CNPJ_VALIDO)).thenReturn(false);
        when(cnpjFacade.buscar(CNPJ_VALIDO)).thenReturn(cnpjResponse);
        when(cvmFacade.verificar(CNPJ_VALIDO))
                .thenReturn(ResultadoVerificacaoCvm.falhaVerificacao(null,
                        "Não foi possível verificar a autorização na CVM: base de dados indisponível"));

        assertThatThrownBy(() -> service.cadastrar(new CorretoraRequestDTO(CNPJ_VALIDO)))
                .isInstanceOf(IntegracaoExternaException.class)
                .hasMessageContaining("verificar")
                .hasMessageNotContaining("não autorizada");
    }

    @Test
    @DisplayName("@spec:AC-434 Falha de infraestrutura ao verificar CVM retorna EXT-007 (503), distinto de COR-003 (422)")
    void deveRetornarExt007QuandoCvmIndisponivel() {
        when(repository.existsByCnpjAndAtivoTrue(CNPJ_VALIDO)).thenReturn(false);
        when(cnpjFacade.buscar(CNPJ_VALIDO)).thenReturn(cnpjResponse);
        when(cvmFacade.verificar(CNPJ_VALIDO))
                .thenReturn(ResultadoVerificacaoCvm.falhaVerificacao(null,
                        "Não foi possível verificar a autorização na CVM: base de dados indisponível"));

        assertThatThrownBy(() -> service.cadastrar(new CorretoraRequestDTO(CNPJ_VALIDO)))
                .isInstanceOf(IntegracaoExternaException.class)
                .extracting(e -> ((IntegracaoExternaException) e).getCodigo())
                .isEqualTo("EXT-007");
    }

    @Test
    @DisplayName("@spec:AC-107 Resposta informa a data da base CVM usada na verificação")
    void deveRetornarDataDaBaseCvmNaResposta() {
        LocalDate dataBase = LocalDate.now();
        when(repository.existsByCnpjAndAtivoTrue(CNPJ_VALIDO)).thenReturn(false);
        when(cnpjFacade.buscar(CNPJ_VALIDO)).thenReturn(cnpjResponse);
        when(cvmFacade.verificar(CNPJ_VALIDO)).thenReturn(ResultadoVerificacaoCvm.autorizada(dataBase));
        when(repository.save(any())).thenReturn(corretora);
        when(mapper.toResponse(corretora)).thenReturn(responseDTO);

        CorretoraResponseDTO result = service.cadastrar(new CorretoraRequestDTO(CNPJ_VALIDO));

        assertThat(result.getDataBaseCvm()).isEqualTo(dataBase);
    }
}
