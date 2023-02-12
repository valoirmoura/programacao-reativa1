package br.com.programacao_reativa.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PubSubMessage {

    String key;
    String value;

}
