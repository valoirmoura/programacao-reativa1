package br.com.programacao_reativa;

import br.com.programacao_reativa.domain.model.PubSubMessage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Sinks;

@SpringBootApplication
public class ProgramacaoReativaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProgramacaoReativaApplication.class, args);
    }

    @Bean
    public Sinks.Many<PubSubMessage> sink() {
        return Sinks.many().multicast().onBackpressureBuffer(1000);
    }

}
