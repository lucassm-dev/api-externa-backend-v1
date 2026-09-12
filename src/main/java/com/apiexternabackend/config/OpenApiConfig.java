package com.apiexternabackend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_BEARER = "bearerAuth";

    /**
     * Sem este esquema declarado o Swagger UI não mostra o botão "Authorize", e quem abre a
     * documentação só consegue exercitar /auth/** — todo o resto responde 401 porque a UI não
     * tem onde guardar o token para mandar no cabeçalho.
     */
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API — Simulação de Carteira de Ações")
                        .version("v1")
                        .description("""
                                Carteiras hipotéticas de ações: cadastro de corretoras validadas na CVM, \
                                catálogo de ações com cotação de fonte externa, compra e venda a preço de \
                                mercado e consolidação em BRL.

                                Faça login em POST /auth/login, copie o campo "token" da resposta e cole em \
                                Authorize — sem ele, todo endpoint fora de /auth/** responde 401 (AUT-005).""")
                )
                .components(new Components().addSecuritySchemes(ESQUEMA_BEARER,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token devolvido por POST /auth/login. Cole só o token, sem o prefixo Bearer.")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
    }
}
