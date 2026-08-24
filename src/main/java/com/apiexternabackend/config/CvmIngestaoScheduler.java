package com.apiexternabackend.config;

import com.apiexternabackend.services.CvmIngestaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CvmIngestaoScheduler {

    private final CvmIngestaoService service;

    @EventListener(ApplicationReadyEvent.class)
    public void ingestirNaSubida() {
        if (service.precisaAtualizar()) {
            log.info("Base CVM ausente ou desatualizada — iniciando ingestão");
            try {
                service.ingerir();
            } catch (Exception e) {
                log.error("Falha na ingestão CVM na subida: {}", e.getMessage());
            }
        } else {
            log.info("Base CVM já atualizada — pulando ingestão na subida");
        }
    }

    // Todo dia às 06:00 (dataset CVM atualizado no início do dia útil)
    @Scheduled(cron = "0 0 6 * * MON-FRI")
    public void ingestirDiariamente() {
        log.info("Job diário CVM iniciado");
        try {
            service.ingerir();
        } catch (Exception e) {
            log.error("Falha no job diário de ingestão CVM: {}", e.getMessage());
        }
    }
}
