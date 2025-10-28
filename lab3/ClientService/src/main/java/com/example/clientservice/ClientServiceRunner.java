package com.example.clientservice;

import com.example.clientservice.client.ClientWithoutResilience;
import com.example.clientservice.client.ClientResilience;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Scanner;

@Component
public class ClientServiceRunner implements CommandLineRunner {
    @Value("${timeout.connection.second}")
    private int connectionTimeout;
    @Value("${timeout.read.second}")
    private int readTimeout;
    private final Scanner scanner;
    private final ClientWithoutResilience clientWithoutResilience;
    private final ClientResilience clientResilience;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public ClientServiceRunner(ClientWithoutResilience clientWithoutResilience, ClientResilience clientResilience,
                               CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        this.scanner = new Scanner(System.in);
        this.clientWithoutResilience = clientWithoutResilience;
        this.clientResilience = clientResilience;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("ClientService started. Type 'help' for available commands, 'exit' to stop.");

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim().toLowerCase();

            switch (input) {
                case "exit":
                    scanner.close();
                    System.out.println("ClientService stopped.");
                    return;

                case "hello":
                    callHelloEndpoint();
                    break;

                case "hello-fail":
                case "hello-delay":
                case "hello-chaos":
                    callHelloSpecialEndpoint(input);
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
            String response = clientWithoutResilience.callHello();
            System.out.println("Response: " + response);
        } catch (Exception e) {
            System.err.println("Error calling server: " + e.getMessage());
        }
    }

    private void callHelloSpecialEndpoint(String mode) {
        while (true) {
            System.out.print("Use resilience mechanisms? (yes/no): ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("yes")) {
                switch (mode) {
                    case "hello-fail":
                        callHelloFailEndpointWithResilience();
                        break;
                    case "hello-delay":
                        callHelloDelayEndpointWithResilience();
                        break;
                    case "hello-chaos":
                        callHelloChaosEndpointWithResilience();
                        break;
                    case "default":
                        System.out.println("Unknown mode.");
                        break;
                }
                break;
            } else if (input.equalsIgnoreCase("no")) {
                switch (mode) {
                    case "hello-fail":
                        callHelloFailEndpointWithoutResilience();
                        break;
                    case "hello-delay":
                        callHelloDelayEndpointWithoutResilience();
                        break;
                    case "hello-chaos":
                        callHelloChaosEndpointWithoutResilience();
                        break;
                    case "default":
                        System.out.println("Unknown mode.");
                        break;
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
            String response = clientWithoutResilience.callHelloFail(shouldFail);
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
            String response = clientWithoutResilience.callHelloDelay(delayMs);
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

    private void callHelloChaosEndpointWithoutResilience() {
        try {
            int chaosPercent = getChaosPercentFromUser();
            String response = clientWithoutResilience.callHelloChaos(chaosPercent);
            System.out.println("Response: " + response);
        } catch (Exception e) {
            System.err.println("Error calling server: " + e.getMessage());
        }
    }

    private void callHelloChaosEndpointWithResilience() {
        try {
            int chaosPercent = getChaosPercentFromUser();
            String response = clientResilience.callHelloChaos(chaosPercent);
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

    private int getChaosPercentFromUser() {
        while (true) {
            System.out.print("Enter chaos percent (0-100): ");
            String input = scanner.nextLine().trim();
            try {
                int chaosPercent = Integer.parseInt(input);
                if (chaosPercent < 0 || chaosPercent > 100) {
                    System.out.println("Please enter a number between 0 and 100.");
                } else {
                    return chaosPercent;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
    }

    public void printDetailedResilienceStatus() {
        try {
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

            System.out.println("Timeouts:");
            System.out.println("  Connection Timeout: " + connectionTimeout + " seconds");
            System.out.println("  Read Timeout: " + readTimeout + " seconds");
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
        System.out.println("  hello-chaos - Call the hello-chaos endpoint on the server");
        System.out.println("  status - Show detailed resilience status");
        System.out.println("  help   - Show this help message");
        System.out.println("  exit   - Stop the client service");
    }
}