package com.apiexternabackend.services;

import com.apiexternabackend.domains.CvmParticipante;
import com.apiexternabackend.infra.client.cvm.CvmCorretoraClient;
import com.apiexternabackend.repositories.CvmParticipanteRepository;
import com.apiexternabackend.resources.exceptions.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CvmIngestaoService {

    private static final int BATCH_SIZE = 500;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CvmCorretoraClient client;
    private final CvmParticipanteRepository repository;

    public boolean precisaAtualizar() {
        Optional<LocalDate> dataBase = repository.findDataBaseMaisRecente();
        if (dataBase.isEmpty()) return true;
        // Recarrega se a base mais recente não é de hoje
        return !dataBase.get().equals(LocalDate.now());
    }

    @Transactional
    public void ingerir() {
        try (BufferedReader reader = client.downloadCsv()) {
            String header = reader.readLine();
            if (header == null) {
                throw new ExternalServiceException("Dataset da CVM vazio ou corrompido");
            }

            // Detecta índices das colunas pelo cabeçalho
            String[] cols = header.split(";");
            int idxCnpj = indexOf(cols, "CNPJ_CIA", "CNPJ");
            int idxNome = indexOf(cols, "NOME_EMPRESA", "DENOM_SOCIAL");
            int idxTipo = indexOf(cols, "TIPO_PARTICIPANTE", "CATEG_REG");
            int idxSituacao = indexOf(cols, "SITUACAO", "SIT");
            int idxData = indexOf(cols, "DT_REG", "DT_CONST", "DT_REFER");

            LocalDate hoje = LocalDate.now();
            List<CvmParticipante> batch = new ArrayList<>(BATCH_SIZE);
            String line;
            int total = 0;

            repository.deleteAll();

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(";", -1);
                if (fields.length <= Math.max(idxCnpj, Math.max(idxNome, idxSituacao))) continue;

                String cnpj = clean(fields, idxCnpj).replaceAll("[^0-9]", "");
                if (cnpj.length() != 14) continue;

                String nome = clean(fields, idxNome);
                String tipo = idxTipo >= 0 ? clean(fields, idxTipo) : "";
                String situacao = clean(fields, idxSituacao);
                LocalDate dataBase = parseDate(idxData >= 0 ? clean(fields, idxData) : null, hoje);

                batch.add(new CvmParticipante(cnpj, nome, tipo, situacao, dataBase));
                if (batch.size() >= BATCH_SIZE) {
                    repository.saveAll(batch);
                    total += batch.size();
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                repository.saveAll(batch);
                total += batch.size();
            }

            log.info("Ingestão CVM concluída: {} participantes carregados", total);
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("Erro ao processar dataset da CVM: " + e.getMessage(), e);
        }
    }

    private int indexOf(String[] headers, String... candidates) {
        for (String c : candidates) {
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase(c)) return i;
            }
        }
        return -1;
    }

    private String clean(String[] fields, int idx) {
        if (idx < 0 || idx >= fields.length) return "";
        return fields[idx].trim().replaceAll("^\"|\"$", "");
    }

    private LocalDate parseDate(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return LocalDate.parse(raw, DATE_FMT);
        } catch (Exception e) {
            return fallback;
        }
    }
}
