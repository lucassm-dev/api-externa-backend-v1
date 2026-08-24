package com.apiexternabackend.infra.converters;

import com.apiexternabackend.domains.enums.TipoOperacao;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TipoOperacaoConverter implements AttributeConverter<TipoOperacao, String> {

    @Override
    public String convertToDatabaseColumn(TipoOperacao tipo) {
        return tipo == null ? null : tipo.name();
    }

    @Override
    public TipoOperacao convertToEntityAttribute(String value) {
        return value == null ? null : TipoOperacao.valueOf(value);
    }
}
