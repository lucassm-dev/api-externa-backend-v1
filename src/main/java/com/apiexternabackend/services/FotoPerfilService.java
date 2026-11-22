package com.apiexternabackend.services;

import com.apiexternabackend.resources.exceptions.RegraVioladaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FotoPerfilService {

    static final long TAMANHO_MAXIMO = 2 * 1024 * 1024;

    public void enviar(Long usuarioId, MultipartFile arquivo) {
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new RegraVioladaException("AUT-016", "A foto deve ter no máximo 2 MB.");
        }
    }
}
