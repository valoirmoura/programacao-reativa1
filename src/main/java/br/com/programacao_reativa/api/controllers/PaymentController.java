package br.com.programacao_reativa.api.controllers;

import br.com.programacao_reativa.domain.model.Payment;
import br.com.programacao_reativa.domain.publishers.PaymentPublisher;
import br.com.programacao_reativa.domain.repository.PaymentRepository;
import br.com.programacao_reativa.infra.database.InMemoryDatabase;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final PaymentPublisher paymentPublisher;

    @PostMapping
    public Mono<Payment> createPayment(@RequestBody final NewPaymentInput input) {
        final String userId = input.getUserId();
        log.info("Payment to be processed {}", userId);

        return this.paymentRepository
                .createPayment(userId)
                .flatMap(payment -> this.paymentPublisher.onPaymentCreate(payment))
                .flatMap(payment ->
                        Flux.interval(Duration.ofSeconds(1))
                                .doOnNext(v -> log.info("Next Tick {}", v))
                                .flatMap(tick -> this.paymentRepository.getPayment(userId))
                                .filter(it -> Payment.PaymentStatus.APPROVED == it.getStatus())
                                .next())
                .doOnNext(next -> log.info("Payment Processed {}", userId))
                .timeout(Duration.ofSeconds(5))
                .retry(3)
                .retryWhen(
                        Retry.backoff(2, Duration.ofSeconds(1))
                                .doAfterRetry(signal -> log.info("Execution Failed... retrying... {}", signal)));

    }


    @GetMapping(value = "users")
    public Flux<Payment> findAllById(@RequestParam String ids) {
        var _ids = Arrays.asList(ids.split(","));
        log.info("Collection {} payments", _ids.size());

        return Flux.fromIterable(_ids)
                .flatMap(this.paymentRepository::getPayment);
    }


    @GetMapping(value = "ids")
    public Mono<String> getIds() {

        return Mono.fromCallable(() -> {
                    return String.join(",", InMemoryDatabase.DATABASE.keySet());
                })
                .subscribeOn(Schedulers.parallel());

    }


    @Data
    public static class NewPaymentInput {
        private String userId;
    }
}
