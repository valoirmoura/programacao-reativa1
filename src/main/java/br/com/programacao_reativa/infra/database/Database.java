package br.com.programacao_reativa.infra.database;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Optional;

public interface Database {

    <T> T save(final String key, final T value) throws JsonProcessingException;
    <T>Optional<T> get(final String key, final Class<T> clazz);
}
