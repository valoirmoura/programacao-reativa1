package br.com.programacao_reativa.domain.listeners;

import br.com.programacao_reativa.domain.model.Payment;
import br.com.programacao_reativa.domain.model.PubSubMessage;
import br.com.programacao_reativa.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentListner implements InitializingBean {

    private final Sinks.Many<PubSubMessage> sink;
    private final PaymentRepository paymentRepository;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.sink.asFlux()
                .delayElements(Duration.ofSeconds(2))
                .subscribe(
                        next -> {
                            log.info("On next message - {}", next.getKey());
                            this.paymentRepository.processPayment(next.getKey(), Payment.PaymentStatus.APPROVED)
                                    .doOnNext(it -> log.info("Payment processed on listener {}", it))
                                    .subscribe();
                        },
                        error -> {
                            log.error("On pub-sub lister observe error", error);
                        },
                        () -> {
                            log.info("On Pub-Sub listerner Complete");
                        }
                );
    }
}
