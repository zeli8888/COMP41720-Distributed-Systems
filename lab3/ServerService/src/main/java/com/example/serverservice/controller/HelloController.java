package com.example.serverservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Server";
    }

    @GetMapping("/hello-fail")
    public String hello(@RequestParam("shouldFail") boolean shouldFail) {
        if (shouldFail) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated failure");
        }
        return "Hello from Server";
    }

    @GetMapping("/hello-delay")
    public String helloDelay(@RequestParam("delayMs") long delayMs) throws InterruptedException {
        Thread.sleep(delayMs);
        return "Hello from Server after delay of " + delayMs + " ms";
    }
}
