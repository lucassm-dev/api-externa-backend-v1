package com.apiexternabackend.services;

import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Service
public class FotoPerfilService {

    static final long TAMANHO_MAXIMO = 2 * 1024 * 1024;
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    public void enviar(Long usuarioId, MultipartFile arquivo) throws IOException {
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new RegraVioladaException("AUT-016", "A foto deve ter no máximo 2 MB.");
        }
        // ADR-017: o formato vem da assinatura do conteúdo, nunca da extensão nem do Content-Type do cliente
        if (tipo(arquivo.getBytes()) == null) {
            throw new RegraVioladaException("AUT-017", "A foto deve estar em JPEG, PNG ou WebP.");
        }
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
