package com.example.clientservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class Client {

    private final RestClient restClient;
    private final String baseUrl;

    public Client(@Value("${server.uri}") String serverUrl) {
        this.baseUrl = serverUrl;
        this.restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .build();
    }

    public String callHello() {
        return restClient.get()
                .uri("/hello")
                .retrieve()
                .body(String.class);
    }

    public String callHelloFail(boolean shouldFail) {
        return restClient.get()
                .uri("/hello-fail?shouldFail=" + shouldFail)
                .retrieve()
                .body(String.class);
    }

    public String callHelloDelay(long delayMs) {
        return restClient.get()
                .uri("/hello-delay?delayMs=" + delayMs)
                .retrieve()
                .body(String.class);
    }
}