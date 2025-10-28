package com.example.serverservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        System.out.println("Received request for /hello at " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
        return "Hello from Server";
    }

    @GetMapping("/hello-fail")
    public String hello(@RequestParam("shouldFail") boolean shouldFail) {
        System.out.println("Received request for /hello-fail with shouldFail: " + shouldFail + ", at " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
        if (shouldFail) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated failure");
        }
        return "Hello from Server";
    }

    @GetMapping("/hello-delay")
    public String helloDelay(@RequestParam("delayMs") long delayMs) throws InterruptedException {
        System.out.println("Received request for /hello-delay with delayMs: " + delayMs + ", at " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
        Thread.sleep(delayMs);
        return "Hello from Server after delay of " + delayMs + " ms";
    }

    @GetMapping("/hello-chaos")
    public String helloChaos(@RequestParam("chaosPercent") int chaosPercent) {
        System.out.println("Received request for /hello-chaos with chaosPercent: " + chaosPercent + ", at " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
        int randomValue = (int) (Math.random() * 100);
        if (randomValue < chaosPercent) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated chaos failure");
        }
        return "Hello from Server with chaos percent of " + chaosPercent + "%";
    }
}
