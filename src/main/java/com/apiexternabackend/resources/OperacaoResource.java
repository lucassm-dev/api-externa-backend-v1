package com.apiexternabackend.resources;

import com.apiexternabackend.domains.dtos.CarteiraAcaoResponseDTO;
import com.apiexternabackend.domains.dtos.OperacaoEditarDTO;
import com.apiexternabackend.domains.dtos.OperacaoRequestDTO;
import com.apiexternabackend.domains.dtos.OperacaoResponseDTO;
import com.apiexternabackend.services.ConsultaOperacaoService;
import com.apiexternabackend.services.OperacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class OperacaoResource {

    private final OperacaoService operacaoService;
    private final ConsultaOperacaoService consultaService;

    @PostMapping("/operacoes/compra")
    public ResponseEntity<OperacaoResponseDTO> comprar(@RequestBody @Valid OperacaoRequestDTO dto) {
        OperacaoResponseDTO response = operacaoService.comprar(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/operacoes/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/operacoes/venda")
    public ResponseEntity<OperacaoResponseDTO> vender(@RequestBody @Valid OperacaoRequestDTO dto) {
        OperacaoResponseDTO response = operacaoService.vender(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/operacoes/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/operacoes")
    public ResponseEntity<Page<OperacaoResponseDTO>> historico(
            @RequestParam Long investidorId, Pageable pageable) {
        return ResponseEntity.ok(consultaService.historico(investidorId, pageable));
    }

    @PutMapping("/operacoes/{id}")
    public ResponseEntity<OperacaoResponseDTO> editar(
            @PathVariable Long id, @RequestBody @Valid OperacaoEditarDTO dto) {
        return ResponseEntity.ok(operacaoService.editar(id, dto.getQuantidade(), dto.getPrecoUnitario()));
    }

    @DeleteMapping("/operacoes/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        operacaoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/carteiras/{id}/posicoes")
    public ResponseEntity<List<CarteiraAcaoResponseDTO>> posicoes(@PathVariable Long id) {
        return ResponseEntity.ok(consultaService.posicoes(id));
    }
}
