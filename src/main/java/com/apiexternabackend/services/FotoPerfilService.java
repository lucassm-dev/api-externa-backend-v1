package com.apiexternabackend.services;

import com.apiexternabackend.domains.FotoPerfil;
import com.apiexternabackend.repositories.FotoPerfilRepository;
import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FotoPerfilService {

    static final long TAMANHO_MAXIMO = 10 * 1024 * 1024;
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private final FotoPerfilRepository repository;

    public void enviar(Long usuarioId, MultipartFile arquivo) throws IOException {
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new RegraVioladaException("AUT-016", "A foto deve ter no máximo 10 MB.");
        }
        byte[] conteudo = arquivo.getBytes();
        // ADR-017: o formato vem da assinatura do conteúdo, nunca da extensão nem do Content-Type do cliente
        String tipo = tipo(conteudo);
        if (tipo == null) {
            throw new RegraVioladaException("AUT-017", "A foto deve estar em JPEG, PNG ou WebP.");
        }
        repository.save(new FotoPerfil(usuarioId, conteudo, tipo, Instant.now()));
    }

    public void remover(Long usuarioId) {
        repository.deleteById(usuarioId);
    }

    public Optional<FotoPerfil> buscar(Long usuarioId) {
        return repository.findById(usuarioId);
    }

    static String tipo(byte[] conteudo) {
        if (comecaCom(conteudo, JPEG)) {
            return "image/jpeg";
        }
        if (comecaCom(conteudo, PNG)) {
            return "image/png";
        }
        if (conteudo.length >= 12
                && new String(conteudo, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(conteudo, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            return "image/webp";
        }
        return null;
    }

    private static boolean comecaCom(byte[] conteudo, byte[] assinatura) {
        return conteudo.length >= assinatura.length
                && Arrays.equals(conteudo, 0, assinatura.length, assinatura, 0, assinatura.length);
    }
}
