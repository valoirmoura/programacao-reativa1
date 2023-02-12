package br.com.programacao_reativa.domain.repository;

import br.com.programacao_reativa.domain.model.Payment;
import br.com.programacao_reativa.infra.database.Database;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PaymentRepository {


    private static final ThreadFactory THREAD_FACTORY = new CustomizableThreadFactory("database-");
    private static final Scheduler DB_SCHEDULER = Schedulers.fromExecutor(
            Executors.newFixedThreadPool(8, THREAD_FACTORY));
    private final Database database;

    //Scheduler é uma abstração de um pool de request
    static {
        Schedulers.parallel(); //Especifico para Tarefas que trabalhem com CPU, abre Threads conforme numero de CPUs
        Schedulers.boundedElastic(); //Específico para IO, tudo que irá fazer chamadas Bloqueantes, ou seja, chamadas para Banco de Dados, Disco....
//        Schedulers.immediate();
//        Schedulers.single();
    }


    public Mono<Payment> createPayment(String userId) {
        var payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .status(Payment.PaymentStatus.PENDING)
                .build();

        return Mono.fromCallable(() -> {
                    log.info("Saving payment transaction for user {}", userId);
                    return this.database.save(userId, payment);
                })
                .delayElement(Duration.ofMillis(20))
                .subscribeOn(DB_SCHEDULER) //boundedElastic somente pq simulamos uma chamada para um banco real
                .doOnNext(next -> log.info("Payment received {}", next.getUserId()));
    }


    public Mono<Payment> getPayment(final String userId) {
        return Mono.defer(() -> {
                    log.info("Getting payment from database {}", userId);
                    final Optional<Payment> payment = this.database.get(userId, Payment.class);
                    return Mono.justOrEmpty(payment);
                })
                .delayElement(Duration.ofMillis(20))
                .subscribeOn(DB_SCHEDULER)
                .doOnNext(v -> log.info("Payment Received - {}", userId));
    }

    public Mono<Object> processPayment(final String key, final Payment.PaymentStatus status) {
        log.info("On payment {} receved to status {}", key, status);
        return getPayment(key)
                .flatMap(payment -> Mono.fromCallable(() -> {
                            log.info("Processing payment {} to status {}", key, status);
                            try {
                                return this.database.save(key, payment.withStatus(status));
                            } catch (JsonProcessingException e) {
                                return new RuntimeException(e);
                            }
                        })
                        .delayElement(Duration.ofMillis(30))
                        .subscribeOn(DB_SCHEDULER));
    }
}
