package com.apiexternabackend.services;

import com.apiexternabackend.config.JwtService;
import com.apiexternabackend.domains.Investidor;
import com.apiexternabackend.domains.dtos.AuthCadastroRequestDTO;
import com.apiexternabackend.domains.dtos.AuthLoginRequestDTO;
import com.apiexternabackend.domains.dtos.AuthTokenResponseDTO;
import com.apiexternabackend.domains.dtos.InvestidorResponseDTO;
import com.apiexternabackend.mappers.InvestidorMapper;
import com.apiexternabackend.repositories.InvestidorRepository;
import com.apiexternabackend.resources.exceptions.CredenciaisInvalidasException;
import com.apiexternabackend.resources.exceptions.RecursoDuplicadoException;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock private InvestidorRepository repository;
    @Mock private InvestidorMapper mapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AutenticacaoService service;

    private Investidor investidor;

    @BeforeEach
    void setUp() {
        investidor = new Investidor(1L, "João Silva", "joao@email.com", "12345678901", "hash-bcrypt", LocalDateTime.now(), true);
    }

    @Test
    @DisplayName("@spec:AC-001 Cadastro com credenciais válidas cria a conta")
    void deveCadastrarComCredenciaisValidas() {
        AuthCadastroRequestDTO dto = new AuthCadastroRequestDTO("João Silva", "joao@email.com", "12345678901", "senha123");
        when(repository.existsByEmailAndAtivoTrue(dto.getEmail())).thenReturn(false);
        when(repository.existsByCpfAndAtivoTrue(dto.getCpf())).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-bcrypt");
        when(repository.save(any())).thenReturn(investidor);
        when(mapper.toResponse(investidor)).thenReturn(new InvestidorResponseDTO(1L, "João Silva", "joao@email.com"));

        InvestidorResponseDTO result = service.cadastrar(dto);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("joao@email.com");
    }

    @Test
    @DisplayName("@spec:AC-002 Identificador de login (e-mail) é único")
    void deveRejeitarLoginDuplicado() {
        AuthCadastroRequestDTO dto = new AuthCadastroRequestDTO("João", "joao@email.com", "12345678901", "senha123");
        when(repository.existsByEmailAndAtivoTrue(dto.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(dto))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("E-mail");
    }

    @Test
    @DisplayName("@spec:AC-003 Senha nunca é guardada em texto puro — é o hash do encoder que é persistido")
    void deveSalvarSenhaComoHash() {
        AuthCadastroRequestDTO dto = new AuthCadastroRequestDTO("João", "joao@email.com", "12345678901", "senha123");
        when(repository.existsByEmailAndAtivoTrue(dto.getEmail())).thenReturn(false);
        when(repository.existsByCpfAndAtivoTrue(dto.getCpf())).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$10$hashFicticio");
        when(repository.save(any())).thenReturn(investidor);
        when(mapper.toResponse(investidor)).thenReturn(new InvestidorResponseDTO(1L, "João", "joao@email.com"));

        service.cadastrar(dto);

        ArgumentCaptor<Investidor> captor = ArgumentCaptor.forClass(Investidor.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSenha()).isEqualTo("$2a$10$hashFicticio");
        assertThat(captor.getValue().getSenha()).isNotEqualTo("senha123");
    }

    @Test
    @DisplayName("@spec:AC-435 CPF duplicado é recusado")
    void deveRejeitarCpfDuplicado() {
        AuthCadastroRequestDTO dto = new AuthCadastroRequestDTO("João", "joao@email.com", "12345678901", "senha123");
        when(repository.existsByEmailAndAtivoTrue(dto.getEmail())).thenReturn(false);
        when(repository.existsByCpfAndAtivoTrue(dto.getCpf())).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(dto))
                .isInstanceOf(RecursoDuplicadoException.class)
                .hasMessageContaining("CPF");
    }

    @Test
    @DisplayName("@spec:AC-436 Senha fora da política mínima (8+, letra e número) é recusada")
    void deveRejeitarSenhaForaDaPolitica() {
        when(repository.existsByEmailAndAtivoTrue(anyString())).thenReturn(false);
        when(repository.existsByCpfAndAtivoTrue(anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.cadastrar(new AuthCadastroRequestDTO("João", "joao@email.com", "12345678901", "curta1")))
                .isInstanceOf(RegraVioladaException.class);
        assertThatThrownBy(() -> service.cadastrar(new AuthCadastroRequestDTO("João", "joao@email.com", "12345678901", "somenteletras")))
                .isInstanceOf(RegraVioladaException.class);
        assertThatThrownBy(() -> service.cadastrar(new AuthCadastroRequestDTO("João", "joao@email.com", "12345678901", "12345678")))
                .isInstanceOf(RegraVioladaException.class);
    }

    @Test
    @DisplayName("@spec:AC-004 Login com credenciais corretas autentica")
    void deveAutenticarComCredenciaisCorretas() {
        when(repository.findByEmailAndAtivoTrue("joao@email.com")).thenReturn(Optional.of(investidor));
        when(passwordEncoder.matches("senha123", "hash-bcrypt")).thenReturn(true);
        when(jwtService.gerar(1L, "joao@email.com")).thenReturn("token-jwt-ficticio");
        Instant expiracao = Instant.now().plusSeconds(86400);
        when(jwtService.expiracaoDe("token-jwt-ficticio")).thenReturn(expiracao);

        AuthTokenResponseDTO result = service.login(new AuthLoginRequestDTO("joao@email.com", "senha123"));

        assertThat(result.getToken()).isEqualTo("token-jwt-ficticio");
        assertThat(result.getTipo()).isEqualTo("Bearer");
        assertThat(result.getExpiraEm()).isEqualTo(expiracao);
    }

    @Test
    @DisplayName("@spec:AC-005 Login com senha incorreta é recusado com mensagem genérica")
    void deveRecusarSenhaIncorreta() {
        when(repository.findByEmailAndAtivoTrue("joao@email.com")).thenReturn(Optional.of(investidor));
        when(passwordEncoder.matches("senha-errada", "hash-bcrypt")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new AuthLoginRequestDTO("joao@email.com", "senha-errada")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha inválidos.");
    }

    @Test
    @DisplayName("@spec:AC-005 Login com e-mail inexistente é recusado com a MESMA mensagem genérica")
    void deveRecusarEmailInexistenteComMensagemIdentica() {
        when(repository.findByEmailAndAtivoTrue("naoexiste@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new AuthLoginRequestDTO("naoexiste@email.com", "qualquer123")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha inválidos.");
    }

    @Test
    @DisplayName("@spec:AC-503 Login de investidor excluído é recusado com a mesma mensagem genérica")
    void deveRecusarLoginDeInvestidorExcluido() {
        // investidor inativo não aparece em findByEmailAndAtivoTrue — mesmo caminho de "não existe"
        when(repository.findByEmailAndAtivoTrue("joao@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new AuthLoginRequestDTO("joao@email.com", "senha123")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha inválidos.");
    }

    @Test
    @DisplayName("@spec:AC-505 E-mail de investidor excluído pode ser reaproveitado no cadastro")
    void devePermitirCadastroComEmailDeInvestidorExcluido() {
        AuthCadastroRequestDTO dto = new AuthCadastroRequestDTO("Outro Nome", "joao@email.com", "99988877766", "senha123");
        when(repository.existsByEmailAndAtivoTrue(dto.getEmail())).thenReturn(false); // só o excluído tinha esse e-mail
        when(repository.existsByCpfAndAtivoTrue(dto.getCpf())).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash-bcrypt");
        when(repository.save(any())).thenReturn(investidor);
        when(mapper.toResponse(investidor)).thenReturn(new InvestidorResponseDTO(2L, "Outro Nome", "joao@email.com"));

        InvestidorResponseDTO result = service.cadastrar(dto);

        assertThat(result.getEmail()).isEqualTo("joao@email.com");
        verify(repository).save(any());
    }

    @Test
    @DisplayName("@spec:AC-506 CPF de investidor excluído pode ser reaproveitado no cadastro")
    void devePermitirCadastroComCpfDeInvestidorExcluido() {
        AuthCadastroRequestDTO dto = new AuthCadastroRequestDTO("Outro Nome", "novo@email.com", "12345678901", "senha123");
        when(repository.existsByEmailAndAtivoTrue(dto.getEmail())).thenReturn(false);
        when(repository.existsByCpfAndAtivoTrue(dto.getCpf())).thenReturn(false); // só o excluído tinha esse CPF
        when(passwordEncoder.encode("senha123")).thenReturn("hash-bcrypt");
        when(repository.save(any())).thenReturn(investidor);
        when(mapper.toResponse(investidor)).thenReturn(new InvestidorResponseDTO(2L, "Outro Nome", "novo@email.com"));

        assertThat(service.cadastrar(dto)).isNotNull();
        verify(repository).save(any());
    }

    @Test
    @DisplayName("@spec:AC-507 Duplicidade entre investidores ATIVOS continua bloqueada")
    void deveBloquearDuplicidadeEntreAtivos() {
        AuthCadastroRequestDTO dto = new AuthCadastroRequestDTO("Outro", "joao@email.com", "99988877766", "senha123");
        when(repository.existsByEmailAndAtivoTrue(dto.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(dto))
                .isInstanceOf(RecursoDuplicadoException.class)
                .extracting(e -> ((RecursoDuplicadoException) e).getCodigo())
                .isEqualTo("AUT-001");
    }

    @Test
    @DisplayName("@spec:AC-508 Login com e-mail reaproveitado autentica o investidor ATIVO")
    void deveAutenticarInvestidorAtivoComEmailReaproveitado() {
        // o e-mail existe em duas linhas (uma inativa, uma ativa) — a consulta filtrada
        // devolve só a ativa, sem quebrar por resultado múltiplo
        Investidor ativo = new Investidor(2L, "Novo Dono", "joao@email.com", "99988877766", "hash-novo", LocalDateTime.now(), true);
        when(repository.findByEmailAndAtivoTrue("joao@email.com")).thenReturn(Optional.of(ativo));
        when(passwordEncoder.matches("senha123", "hash-novo")).thenReturn(true);
        when(jwtService.gerar(2L, "joao@email.com")).thenReturn("token-do-ativo");
        when(jwtService.expiracaoDe("token-do-ativo")).thenReturn(Instant.now().plusSeconds(86400));

        AuthTokenResponseDTO result = service.login(new AuthLoginRequestDTO("joao@email.com", "senha123"));

        assertThat(result.getToken()).isEqualTo("token-do-ativo");
        verify(jwtService).gerar(2L, "joao@email.com");
    }
}
