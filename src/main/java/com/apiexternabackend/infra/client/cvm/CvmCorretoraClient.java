package com.apiexternabackend.infra.client.cvm;

import com.apiexternabackend.resources.exceptions.ExternalServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.zip.ZipInputStream;

/**
 * Baixa e expõe o stream do CSV de participantes intermediários da CVM.
 * Dataset: dados.cvm.gov.br/dados/INTERMED/CAD/DADOS/cad_intermed.zip
 * Encoding: Latin-1, separador: ';'
 */
@Slf4j
@Component
public class CvmCorretoraClient {

    private static final String CSV_CHARSET = "ISO-8859-1";

    @Value("${cvm.dataset.url:https://dados.cvm.gov.br/dados/INTERMED/CAD/DADOS/cad_intermed.zip}")
    private String datasetUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public BufferedReader downloadCsv() {
        try {
            log.info("Iniciando download do dataset CVM: {}", datasetUrl);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(datasetUrl))
                    .timeout(Duration.ofMinutes(5))
                    .GET()
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new ExternalServiceException(
                        "Falha ao baixar dataset CVM: HTTP " + response.statusCode());
            }

            ZipInputStream zipStream = new ZipInputStream(response.body());
            zipStream.getNextEntry();

            return new BufferedReader(new InputStreamReader(zipStream, Charset.forName(CSV_CHARSET)));
        } catch (ExternalServiceException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Não foi possível acessar o dataset da CVM: " + e.getMessage(), e);
        }
    }
}
