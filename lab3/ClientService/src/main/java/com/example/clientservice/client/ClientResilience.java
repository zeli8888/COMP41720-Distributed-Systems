package com.example.clientservice.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

import java.util.concurrent.CompletableFuture;

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
    @TimeLimiter(name = "serverService")
    @GetExchange("/hello-delay")
    CompletableFuture<String> callHelloDelay(@RequestParam long delayMs);

    @CircuitBreaker(name = "serverService")
    @Retry(name = "serverService")
    @GetExchange("/hello-chaos")
    String callHelloChaos(@RequestParam int chaosPercent);
}
