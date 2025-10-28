package com.example.clientservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

public interface ClientResilience {
    // Methods with Resilience4j annotations
    @CircuitBreaker(name = "serverService")
    @Retry(name = "serverService")
    @GetExchange("/hello")
    String callHello();

    @CircuitBreaker(name = "serverService")
    @Retry(name = "serverService")
    @GetExchange("/hello-fail")
    String callHelloFail(@RequestParam boolean shouldFail);

    @CircuitBreaker(name = "serverService")
    @Retry(name = "serverService")
    @GetExchange("/hello-delay")
    String callHelloDelay(@RequestParam long delayMs);
}
