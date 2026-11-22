package com.apiexternabackend.config;

import com.apiexternabackend.domains.Usuario;
import com.apiexternabackend.domains.enums.SituacaoUsuario;
import com.apiexternabackend.repositories.UsuarioRepository;
import com.apiexternabackend.services.RegistroUsoService;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final UsuarioRepository usuarioRepository;
    private final RegistroUsoService registroUsoService;

    /**
     * Origens autorizadas a chamar a API de outro endereço. Vazio por padrão: ambiente que
     * não configurar a propriedade não libera origem nenhuma, em vez de liberar todas.
     */
    @Value("${cors.allowed-origins:}")
    private List<String> origensPermitidas = List.of();

    private static final String[] ROTAS_PUBLICAS = {
            // spec-17: /auth/me exige token, então só cadastro e login ficam abertos
            "/auth/cadastro", "/auth/login", "/auth/esqueci-senha", "/auth/redefinir-senha",
            "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"
    };

    @Value("${jwt.secret}")
    private String segredo;

    @Value("${jwt.issuer}")
    private String emissor;

    private SecretKey chave() {
        return new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chave()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chave()).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(emissor));
        return decoder;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origensPermitidas);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        // sem credenciais: o token viaja no cabeçalho Authorization, não em cookie de sessão
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(this::autenticacaoDe))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterAfter(new RegistroUsoFilter(registroUsoService), AuthorizationFilter.class);

        return http.build();
    }

    // ASM-460: os resources continuam recebendo InvestidorPrincipal via @AuthenticationPrincipal
    // AC-585/ASM-484: a situação é lida do banco a cada requisição para o bloqueio valer na hora
    private AbstractAuthenticationToken autenticacaoDe(Jwt jwt) {
        Long id = Long.valueOf(jwt.getSubject());
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        SituacaoUsuario situacao = usuario == null ? SituacaoUsuario.EXCLUIDO : usuario.getSituacao();
        if (situacao == SituacaoUsuario.BLOQUEADO) {
            throw new DisabledException("Conta bloqueada.");
        }
        if (situacao == SituacaoUsuario.EXCLUIDO) {
            throw new InvalidBearerTokenException("Conta excluída.");
        }
        // #26: troca de senha derruba os tokens emitidos antes dela (iat tem precisão de segundos)
        if (usuario.getSenhaAlteradaEm() != null && jwt.getIssuedAt().isBefore(usuario.getSenhaAlteradaEm())) {
            throw new InvalidBearerTokenException("Token anterior à troca de senha.");
        }
        var authorities = jwt.getClaimAsStringList("authorities").stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(new InvestidorPrincipal(id), null, authorities);
    }
}
