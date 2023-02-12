package br.com.programacao_reativa.domain.publishers;

import br.com.programacao_reativa.domain.model.Payment;
import br.com.programacao_reativa.domain.model.PubSubMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
public class PaymentPublisher {

    private final Sinks.Many<PubSubMessage> sink;
    private final ObjectMapper mapper;


    public Mono<Payment> onPaymentCreate(final Payment payment) {

       return Mono.fromCallable(() -> {
                    var userId = payment.getUserId();
                    var data = mapper.writeValueAsString(payment);
                    return new PubSubMessage(userId, data);
                })
                .subscribeOn(Schedulers.parallel())
                .doOnNext(next -> this.sink.tryEmitNext(next))
                .thenReturn(payment);
    }
}
