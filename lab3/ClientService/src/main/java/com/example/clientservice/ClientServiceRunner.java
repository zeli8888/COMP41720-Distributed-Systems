package com.example.clientservice;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ClientServiceRunner implements CommandLineRunner {
    @Value("${server.uri}")
    private String SERVER_URL;
    private final RestClient restClient;
    public ClientServiceRunner() {
        this.restClient = RestClient.create();
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("ClientService started. Type 'help' for available commands, 'exit' to stop.");
        java.util.Scanner scanner = new java.util.Scanner(System.in);

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            switch (input.toLowerCase()) {
                case "exit":
                    scanner.close();
                    System.out.println("ClientService stopped.");
                    return;

                case "hello":
                    callHelloEndpoint();
                    break;

                case "help":
                    showHelp();
                    break;

                default:
                    System.out.println("Unknown command. Type 'help' for available commands.");
                    break;
            }
        }
    }

    private void callHelloEndpoint() {
        try {
            String response = restClient.get()
                    .uri(SERVER_URL + "/hello")
                    .retrieve()
                    .body(String.class);
            System.out.println("Response: " + response);
        } catch (RestClientException e) {
            System.err.println("Error calling server: " + e.getMessage());
            System.err.println("Please make sure the server is running at: " + SERVER_URL);
        }
    }

    private void showHelp() {
        System.out.println("Available commands:");
        System.out.println("  hello  - Call the hello endpoint on the server");
        System.out.println("  help   - Show this help message");
        System.out.println("  exit   - Stop the client service");
    }
}