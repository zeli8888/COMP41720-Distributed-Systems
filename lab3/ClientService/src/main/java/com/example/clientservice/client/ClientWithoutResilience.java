package com.example.clientservice.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

public interface ClientWithoutResilience {
    @GetExchange("/hello")
    String callHello();

    @GetExchange("/hello-fail")
    String callHelloFail(@RequestParam boolean shouldFail);

    @GetExchange("/hello-delay")
    String callHelloDelay(@RequestParam long delayMs);

    @GetExchange("/hello-chaos")
    String callHelloChaos(@RequestParam int chaosPercent);
}