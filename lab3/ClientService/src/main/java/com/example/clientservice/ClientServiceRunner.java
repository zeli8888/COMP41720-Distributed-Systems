package com.example.clientservice;

import com.example.clientservice.client.Client;
import com.example.clientservice.client.ClientResilience;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class ClientServiceRunner implements CommandLineRunner {

    private final Scanner scanner = new Scanner(System.in);
    private final Client client;
    private final ClientResilience clientResilience;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public ClientServiceRunner(Client client, ClientResilience clientResilience,
                               CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        this.clientResilience = clientResilience;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
        this.client = client;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("ClientService started. Type 'help' for available commands, 'exit' to stop.");

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

                case "hello-fail":
                    callHelloFailDelayEndpoint("hello-fail");
                    break;

                case "hello-delay":
                    callHelloFailDelayEndpoint("hello-delay");
                    break;

                case "status":
                    printDetailedResilienceStatus();
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
            String response = client.callHello();
            System.out.println("Response: " + response);
        } catch (Exception e) {
            System.err.println("Error calling server: " + e.getMessage());
        }
    }

    private void callHelloFailDelayEndpoint(String mode) {
        while (true) {
            System.out.print("Use resilience mechanisms? (yes/no): ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("yes")) {
                if (mode.equals("hello-fail")) {
                    callHelloFailEndpointWithResilience();
                } else {
                    callHelloDelayEndpointWithResilience();
                }
                break;
            } else if (input.equalsIgnoreCase("no")) {
                if (mode.equals("hello-fail")) {
                    callHelloFailEndpointWithoutResilience();
                } else {
                    callHelloDelayEndpointWithoutResilience();
                }
                break;
            } else {
                System.out.println("Invalid input. Please enter 'yes' or 'no'.");
            }
        }
    }

    private void callHelloFailEndpointWithoutResilience() {
        try {
            boolean shouldFail = getShouldFailFromUser();
            String response = client.callHelloFail(shouldFail);
            System.out.println("Response: " + response);
        } catch (Exception e) {
            System.err.println("Error calling server: " + e.getMessage());
        }
    }

    private void callHelloFailEndpointWithResilience() {
        try {
            boolean shouldFail = getShouldFailFromUser();
            String response = clientResilience.callHelloFail(shouldFail);
            System.out.println("Response: " + response);
        } catch (Exception e) {
            System.err.println("Error with resilience: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    private void callHelloDelayEndpointWithoutResilience() {
        try {
            long delayMs = getDelayFromUser();
            String response = client.callHelloDelay(delayMs);
            System.out.println("Response: " + response);
        } catch (Exception e) {
            System.err.println("Error calling server: " + e.getMessage());
        }
    }

    private void callHelloDelayEndpointWithResilience() {
        try {
            long delayMs = getDelayFromUser();
            String response = clientResilience.callHelloDelay(delayMs);
            System.out.println("Response: " + response);
        } catch (Exception e) {
            System.err.println("Error with resilience: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    private boolean getShouldFailFromUser() {
        while (true) {
            System.out.print("Should the call fail? (yes/no): ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("no")) {
                return input.equalsIgnoreCase("yes");
            } else {
                System.out.println("Invalid input. Please enter 'yes' or 'no'.");
            }
        }
    }

    private long getDelayFromUser() {
        while (true) {
            System.out.print("Enter delay in milliseconds: ");
            String input = scanner.nextLine().trim();
            try {
                long delayMs = Long.parseLong(input);
                if (delayMs < 0) {
                    System.out.println("Please enter a non-negative number.");
                } else {
                    return delayMs;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
    }

    public void printDetailedResilienceStatus() {
        try {
            System.out.println("\n🔧 Resilience4j Detailed Status");

            io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker =
                    circuitBreakerRegistry.circuitBreaker("serverService");
            io.github.resilience4j.retry.Retry retry =
                    retryRegistry.retry("serverService");
            System.out.println("\n🔧 Resilience4j Detailed Status");
            System.out.println("Circuit Breaker:");
            System.out.println("  State: " + circuitBreaker.getState());
            System.out.println("  Failure Rate Threshold: " + circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold() + "%");
            System.out.println("  Sliding Window Size: " + circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize());
            System.out.println("  Permitted Calls in Half-Open: " + circuitBreaker.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState());

            io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();
            System.out.println("  Current Metrics:");
            System.out.println("    • Total Calls: " + metrics.getNumberOfBufferedCalls());
            System.out.println("    • Successful: " + metrics.getNumberOfSuccessfulCalls());
            System.out.println("    • Failed: " + metrics.getNumberOfFailedCalls());
            System.out.println("    • Current Failure Rate: " + String.format("%.1f", metrics.getFailureRate()) + "%");

            System.out.println("Retry:");
            System.out.println("  Max Attempts: " + retry.getRetryConfig().getMaxAttempts());
            io.github.resilience4j.retry.Retry.Metrics retryMetrics = retry.getMetrics();
            System.out.println("  Current Metrics:");
            System.out.println("    • Successful (no retry): " + retryMetrics.getNumberOfSuccessfulCallsWithoutRetryAttempt());
            System.out.println("    • Successful (with retry): " + retryMetrics.getNumberOfSuccessfulCallsWithRetryAttempt());
            System.out.println("    • Failed (after retry): " + retryMetrics.getNumberOfFailedCallsWithRetryAttempt());
            System.out.println();
        } catch (Exception e) {
            System.out.println("❌ Error getting resilience status: " + e.getMessage());
        }
    }

    private void showHelp() {
        System.out.println("Available commands:");
        System.out.println("  hello  - Call the hello endpoint on the server");
        System.out.println("  hello-fail - Call the hello-fail endpoint on the server");
        System.out.println("  hello-delay - Call the hello-delay endpoint on the server");
        System.out.println("  status - Show detailed resilience status");
        System.out.println("  help   - Show this help message");
        System.out.println("  exit   - Stop the client service");
    }
}