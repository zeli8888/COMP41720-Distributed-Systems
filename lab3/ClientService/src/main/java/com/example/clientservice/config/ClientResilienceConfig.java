package com.example.clientservice.config;

import com.example.clientservice.client.ClientResilience;
import com.example.clientservice.client.ClientWithoutResilience;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import java.time.Duration;

@Configuration
public class ClientResilienceConfig {
    @Value("${server.uri}")
    private String serverUrl;
    @Value("${timeout.connection.second}")
    private int connectionTimeout;
    @Value("${timeout.read.second}")
    private int readTimeout;

    @Bean
    public ClientResilience clientResilience() {
        // Create and return the proxy instance for ClientResilience
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectionTimeout));  // connection timeout
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeout));     // read timeout
        RestClient restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .requestFactory(requestFactory)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(ClientResilience.class);
    }

    @Bean
    public ClientWithoutResilience clientWithoutResilience() {
        // Create and return the proxy instance for ClientWithoutResilience
        RestClient restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(ClientWithoutResilience.class);
    }
}
