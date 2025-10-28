package com.example.clientservice.config;

import com.example.clientservice.client.ClientResilience;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class ClientResilienceConfig {
    @Value("${server.uri}")
    private String serverUrl;

    @Bean
    public ClientResilience serverClient() {
        // Create and return the proxy instance for ClientResilienceBase to enable Resilience4j annotations
        RestClient restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .build();

        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(ClientResilience.class);
    }
}
