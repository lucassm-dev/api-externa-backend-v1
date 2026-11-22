package com.apiexternabackend.services;

import com.apiexternabackend.domains.dtos.RelatorioUsoResponseDTO;
import com.apiexternabackend.repositories.RegistroUsoRepository;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RelatorioUsoService {

    private final RegistroUsoRepository repository;

    public RelatorioUsoResponseDTO relatorio(LocalDate dataInicio, LocalDate dataFim) {
        // ASM-512: sem datas, hoje e os 6 dias anteriores
        LocalDate ultimoDia = dataFim != null ? dataFim : LocalDate.now();
        LocalDate primeiroDia = dataInicio != null ? dataInicio : ultimoDia.minusDays(6);
        if (ultimoDia.isBefore(primeiroDia)) {
            throw new RegraVioladaException("AUT-018", "Data final anterior à data inicial");
        }
        LocalDateTime inicio = primeiroDia.atStartOfDay();
        LocalDateTime fim = ultimoDia.plusDays(1).atStartOfDay();
        List<RelatorioUsoResponseDTO.Pico> picos = repository.picosPorHora(inicio, fim).stream()
                .map(linha -> new RelatorioUsoResponseDTO.Pico(dataHora(linha[0]), ((Number) linha[1]).longValue()))
                .toList();
        List<RelatorioUsoResponseDTO.UsuarioUso> usuarios = repository.usuariosQueMaisChamaram(inicio, fim).stream()
                .map(linha -> new RelatorioUsoResponseDTO.UsuarioUso(
                        ((Number) linha[0]).longValue(), (String) linha[1], ((Number) linha[2]).longValue()))
                .toList();
        return new RelatorioUsoResponseDTO(repository.contarNoPeriodo(inicio, fim), picos, usuarios);
    }

    // consulta nativa: o driver pode devolver Timestamp ou LocalDateTime
    private static LocalDateTime dataHora(Object valor) {
        return valor instanceof Timestamp timestamp ? timestamp.toLocalDateTime() : (LocalDateTime) valor;
    }
}
