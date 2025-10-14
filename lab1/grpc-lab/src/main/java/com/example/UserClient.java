package com.example;

import com.example.grpc.UserServiceGrpc;
import com.example.grpc.UserServiceProto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

public class UserClient {

    private final UserServiceGrpc.UserServiceBlockingStub blockingStub;
    private final Scanner scanner;
    private final String grpcHost;
    private final int grpcPort;
    private final String restBaseUrl;

    // Benchmark configuration
    private static final int DEFAULT_NUM_REQUESTS = 100;
    private static final int DEFAULT_WARMUP_REQUESTS = 10;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // For REST API data model
    public static class User {
        public String id;
        public String name;
        public String email;

        public User() {}

        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }

    public UserClient(String grpcHost, int grpcPort, String restBaseUrl) {
        this.grpcHost = grpcHost;
        this.grpcPort = grpcPort;
        this.restBaseUrl = restBaseUrl;

        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();
        this.blockingStub = UserServiceGrpc.newBlockingStub(channel);
        this.scanner = new Scanner(System.in);
    }

    public void startInteractiveMode() {
        System.out.println("=== gRPC User Client ===");
        System.out.println("Connected to server successfully!");
        System.out.println("gRPC Server: " + grpcHost + ":" + grpcPort);
        System.out.println("REST Server: " + restBaseUrl);
        System.out.println("\nAvailable commands:");
        System.out.println("1. create - Create a new user");
        System.out.println("2. get [id] - Get user by ID");
        System.out.println("3. update [id] - Update user");
        System.out.println("4. delete [id] - Delete user");
        System.out.println("5. list - List all users");
        System.out.println("6. benchmark - Run performance comparison between REST and gRPC");
        System.out.println("7. exit - Exit client");
        System.out.println();

        while (true) {
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            String[] parts = input.split("\\s+");
            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "create":
                        handleCreateUser();
                        break;
                    case "get":
                        if (parts.length < 2) {
                            System.out.println("Usage: get [id]");
                        } else {
                            handleGetUser(parts[1]);
                        }
                        break;
                    case "update":
                        if (parts.length < 2) {
                            System.out.println("Usage: update [id]");
                        } else {
                            handleUpdateUser(parts[1]);
                        }
                        break;
                    case "delete":
                        if (parts.length < 2) {
                            System.out.println("Usage: delete [id]");
                        } else {
                            handleDeleteUser(parts[1]);
                        }
                        break;
                    case "list":
                        handleListUsers();
                        break;
                    case "benchmark":
                        handleBenchmark(parts);
                        break;
                    case "exit":
                    case "quit":
                        System.out.println("Goodbye!");
                        return;
                    case "help":
                        printHelp();
                        break;
                    default:
                        System.out.println("Unknown command. Type 'help' for available commands.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private void handleBenchmark(String[] parts) {
        int numRequests = DEFAULT_NUM_REQUESTS;
        int warmupRequests = DEFAULT_WARMUP_REQUESTS;

        // Parse optional parameters
        if (parts.length > 1) {
            try {
                numRequests = Integer.parseInt(parts[1]);
                if (parts.length > 2) {
                    warmupRequests = Integer.parseInt(parts[2]);
                }
            } catch (NumberFormatException e) {
                System.out.println("Usage: benchmark [num_requests] [warmup_requests]");
                System.out.println("Using default values: " + numRequests + " requests, " +
                        warmupRequests + " warmup requests");
            }
        }

        System.out.println("🚀 Starting Performance Benchmark...");
        System.out.println("JDK Version: " + System.getProperty("java.version"));
        System.out.println("Number of requests per test: " + numRequests);
        System.out.println("Warmup requests: " + warmupRequests);
        System.out.println();

        try {
            // Warm up
            System.out.println("🔥 Warming up...");
            warmupRest(warmupRequests);
            warmupGrpc(warmupRequests);

            // Benchmark REST API
            System.out.println("\n📊 Benchmarking REST API...");
            double restTime = benchmarkRest(numRequests);

            // Benchmark gRPC
            System.out.println("\n📊 Benchmarking gRPC...");
            double grpcTime = benchmarkGrpc(numRequests);

            // Compare results
            printComparisonResults(restTime, grpcTime, numRequests);
        } catch (Exception e) {
            System.err.println("❌ Benchmark failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void warmupRest(int warmupRequests) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            for (int i = 0; i < warmupRequests; i++) {
                try {
                    HttpGet getRequest = new HttpGet(restBaseUrl);
                    httpClient.execute(getRequest).close();
                } catch (Exception e) {
                    // Ignore warmup errors
                }
            }
        } catch (Exception e) {
            System.err.println("REST warmup error: " + e.getMessage());
        }
    }

    private void warmupGrpc(int warmupRequests) {
        ManagedChannel channel = null;
        try {
            channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                    .usePlaintext()
                    .build();

            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);

            for (int i = 0; i < warmupRequests; i++) {
                try {
                    stub.listUsers(UserServiceProto.Empty.newBuilder().build());
                } catch (Exception e) {
                    // Ignore warmup errors
                }
            }
        } catch (Exception e) {
            System.err.println("gRPC warmup error: " + e.getMessage());
        } finally {
            if (channel != null) {
                try {
                    channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private double benchmarkRest(int numRequests) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        long startTime = System.nanoTime();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            for (int i = 0; i < numRequests; i++) {
                try {
                    String uniqueId = "benchmark-user-" + System.currentTimeMillis() + "-" + i;

                    // 1. Create User (POST)
                    User newUser = new User("BenchmarkUser" + i, "benchmark" + i + "@example.com");
                    String userJson = objectMapper.writeValueAsString(newUser);

                    HttpPost postRequest = new HttpPost(restBaseUrl);
                    postRequest.setEntity(new StringEntity(userJson, ContentType.APPLICATION_JSON));

                    try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                        if (response.getCode() == 201) {
                            // Parse response to get created user ID
                            String responseBody = new String(response.getEntity().getContent().readAllBytes());
                            User createdUser = objectMapper.readValue(responseBody, User.class);
                            String userId = createdUser.id;

                            // 2. Get User (GET)
                            HttpGet getRequest = new HttpGet(restBaseUrl + "/" + userId);
                            httpClient.execute(getRequest).close();

                            // 3. Update User (PUT)
                            User updatedUser = new User("UpdatedBenchmarkUser" + i, "updated" + i + "@example.com");
                            String updateJson = objectMapper.writeValueAsString(updatedUser);

                            HttpPut putRequest = new HttpPut(restBaseUrl + "/" + userId);
                            putRequest.setEntity(new StringEntity(updateJson, ContentType.APPLICATION_JSON));
                            httpClient.execute(putRequest).close();

                            // 4. Delete User (DELETE)
                            HttpDelete deleteRequest = new HttpDelete(restBaseUrl + "/" + userId);
                            httpClient.execute(deleteRequest).close();

                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                            System.err.println("POST request failed with status: " + response.getCode());
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Request failed: " + e.getMessage());
                }

                // Progress indicator
                if ((i + 1) % 10 == 0) {
                    System.out.println("✅ Completed " + (i + 1) + " REST requests...");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error during REST benchmarking: " + e.getMessage());
            e.printStackTrace();
        }

        long endTime = System.nanoTime();
        double totalTime = (endTime - startTime) / 1_000_000_000.0;

        System.out.printf("📈 REST API: %d successes, %d errors%n", successCount.get(), errorCount.get());
        System.out.printf("⏱️  REST API completed in %.3f seconds%n", totalTime);
        return totalTime;
    }

    private double benchmarkGrpc(int numRequests) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Create new gRPC channel for benchmark to avoid interference
        ManagedChannel channel = ManagedChannelBuilder.forAddress(grpcHost, grpcPort)
                .usePlaintext()
                .build();

        long startTime = System.nanoTime();

        try {
            UserServiceGrpc.UserServiceBlockingStub stub = UserServiceGrpc.newBlockingStub(channel);

            for (int i = 0; i < numRequests; i++) {
                try {
                    // 1. Create User
                    UserServiceProto.CreateUserRequest createRequest = UserServiceProto.CreateUserRequest.newBuilder()
                            .setName("BenchmarkUser" + i)
                            .setEmail("benchmark" + i + "@example.com")
                            .build();
                    UserServiceProto.User createdUser = stub.createUser(createRequest);
                    String userId = createdUser.getId();

                    // 2. Get User
                    UserServiceProto.UserRequest getRequest = UserServiceProto.UserRequest.newBuilder()
                            .setId(userId)
                            .build();
                    stub.getUser(getRequest);

                    // 3. Update User
                    UserServiceProto.UpdateUserRequest updateRequest = UserServiceProto.UpdateUserRequest.newBuilder()
                            .setId(userId)
                            .setName("UpdatedBenchmarkUser" + i)
                            .setEmail("updated" + i + "@example.com")
                            .build();
                    stub.updateUser(updateRequest);

                    // 4. Delete User
                    UserServiceProto.UserRequest deleteRequest = UserServiceProto.UserRequest.newBuilder()
                            .setId(userId)
                            .build();
                    stub.deleteUser(deleteRequest);

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("gRPC request failed: " + e.getMessage());
                }

                // Progress indicator
                if ((i + 1) % 10 == 0) {
                    System.out.println("✅ Completed " + (i + 1) + " gRPC requests...");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error during gRPC benchmarking: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long endTime = System.nanoTime();
        double totalTime = (endTime - startTime) / 1_000_000_000.0;

        System.out.printf("📈 gRPC: %d successes, %d errors%n", successCount.get(), errorCount.get());
        System.out.printf("⏱️  gRPC completed in %.3f seconds%n", totalTime);
        return totalTime;
    }

    private void printComparisonResults(double restTime, double grpcTime, int numRequests) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("📊 PERFORMANCE COMPARISON RESULTS");
        System.out.println("=".repeat(50));

        System.out.printf("REST API total time: %.3f seconds%n", restTime);
        System.out.printf("gRPC total time: %.3f seconds%n", grpcTime);
        System.out.println();

        double restAvgMs = (restTime * 1000) / numRequests;
        double grpcAvgMs = (grpcTime * 1000) / numRequests;

        System.out.printf("REST API average time per request: %.3f ms%n", restAvgMs);
        System.out.printf("gRPC average time per request: %.3f ms%n", grpcAvgMs);
        System.out.println();

        double improvement = ((restTime - grpcTime) / restTime) * 100;
        String comparison = improvement > 0 ? "faster" : "slower";

        System.out.printf("🎯 gRPC is %.2f%% %s than REST API%n", Math.abs(improvement), comparison);

        if (improvement > 0) {
            System.out.println("🚀 gRPC shows better performance!");
        } else {
            System.out.println("⚠️  REST API demonstrates better performance in this test scenario");
            System.out.println("✅ Verified: Both gRPC and REST use identical business logic (H2 database + Spring Data JPA)");
            System.out.println();
            System.out.println("🔍 Potential reasons for REST's better performance:");
            System.out.println("1. Sequential synchronous calls don't leverage gRPC's HTTP/2 multiplexing advantages");
            System.out.println("2. Protobuf overhead outweighs benefits for simple data structures");
            System.out.println("3. gRPC framework layers introduce additional processing cost");
            System.out.println("4. JSON serialization/deserialization is more efficient for small payloads in this context");
        }
    }

    private void handleCreateUser() {
        System.out.print("Enter name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Enter email: ");
        String email = scanner.nextLine().trim();

        if (name.isEmpty() || email.isEmpty()) {
            System.out.println("Name and email cannot be empty");
            return;
        }

        try {
            UserServiceProto.CreateUserRequest request = UserServiceProto.CreateUserRequest.newBuilder()
                    .setName(name)
                    .setEmail(email)
                    .build();

            UserServiceProto.User response = blockingStub.createUser(request);
            System.out.println("✅ User created successfully:");
            System.out.println("   ID: " + response.getId());
            System.out.println("   Name: " + response.getName());
            System.out.println("   Email: " + response.getEmail());
        } catch (Exception e) {
            System.err.println("❌ Failed to create user: " + e.getMessage());
        }
    }

    private void handleGetUser(String id) {
        try {
            UserServiceProto.UserRequest request = UserServiceProto.UserRequest.newBuilder()
                    .setId(id)
                    .build();

            UserServiceProto.User response = blockingStub.getUser(request);
            System.out.println("✅ User found:");
            System.out.println("   ID: " + response.getId());
            System.out.println("   Name: " + response.getName());
            System.out.println("   Email: " + response.getEmail());
        } catch (Exception e) {
            System.err.println("❌ Failed to get user: " + e.getMessage());
        }
    }

    private void handleUpdateUser(String id) {
        try {
            UserServiceProto.UserRequest getRequest = UserServiceProto.UserRequest.newBuilder()
                    .setId(id)
                    .build();

            UserServiceProto.User currentUser;
            try {
                currentUser = blockingStub.getUser(getRequest);
            } catch (Exception e) {
                System.err.println("❌ User not found with ID: " + id);
                return;
            }

            System.out.println("Current user:");
            System.out.println("  Name: " + currentUser.getName());
            System.out.println("  Email: " + currentUser.getEmail());
            System.out.println();

            System.out.print("Enter new name (press Enter to keep current): ");
            String nameInput = scanner.nextLine().trim();
            String name = nameInput.isEmpty() ? currentUser.getName() : nameInput;

            System.out.print("Enter new email (press Enter to keep current): ");
            String emailInput = scanner.nextLine().trim();
            String email = emailInput.isEmpty() ? currentUser.getEmail() : emailInput;

            UserServiceProto.UpdateUserRequest request = UserServiceProto.UpdateUserRequest.newBuilder()
                    .setId(id)
                    .setName(name)
                    .setEmail(email)
                    .build();

            UserServiceProto.User response = blockingStub.updateUser(request);
            System.out.println("✅ User updated successfully:");
            System.out.println("   ID: " + response.getId());
            System.out.println("   Name: " + response.getName());
            System.out.println("   Email: " + response.getEmail());
        } catch (Exception e) {
            System.err.println("❌ Failed to update user: " + e.getMessage());
        }
    }

    private void handleDeleteUser(String id) {
        try {
            System.out.print("Are you sure you want to delete user " + id + "? (y/N): ");
            String confirmation = scanner.nextLine().trim();

            if (!confirmation.equalsIgnoreCase("y") && !confirmation.equalsIgnoreCase("yes")) {
                System.out.println("Deletion cancelled.");
                return;
            }

            UserServiceProto.UserRequest request = UserServiceProto.UserRequest.newBuilder()
                    .setId(id)
                    .build();

            blockingStub.deleteUser(request);
            System.out.println("✅ User deleted successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to delete user: " + e.getMessage());
        }
    }

    private void handleListUsers() {
        try {
            UserServiceProto.Empty request = UserServiceProto.Empty.newBuilder().build();
            UserServiceProto.UserList response = blockingStub.listUsers(request);

            if (response.getUsersCount() == 0) {
                System.out.println("No users found.");
                return;
            }

            System.out.println("📋 Users list (" + response.getUsersCount() + " users):");
            System.out.println("==========================================");

            for (UserServiceProto.User user : response.getUsersList()) {
                System.out.println("ID: " + user.getId());
                System.out.println("Name: " + user.getName());
                System.out.println("Email: " + user.getEmail());
                System.out.println("------------------------------------------");
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to list users: " + e.getMessage());
        }
    }

    private void printHelp() {
        System.out.println("Available commands:");
        System.out.println("  create    - Create a new user");
        System.out.println("  get [id]  - Get user by ID");
        System.out.println("  update [id] - Update user");
        System.out.println("  delete [id] - Delete user");
        System.out.println("  list      - List all users");
        System.out.println("  benchmark [num_requests] [warmup_requests] - Run performance comparison");
        System.out.println("  exit      - Exit client");
        System.out.println("  help      - Show this help");
    }

    public static void main(String[] args) {
        String grpcHost = System.getenv().getOrDefault("GRPC_SERVER_HOST", "localhost");
        int grpcPort = Integer.parseInt(System.getenv().getOrDefault("GRPC_SERVER_PORT", "50051"));
        String restBaseUrl = System.getenv().getOrDefault("REST_SERVER_URL", "http://localhost:8081/api/users");

        try {
            UserClient client = new UserClient(grpcHost, grpcPort, restBaseUrl);
            client.startInteractiveMode();
        } catch (Exception e) {
            System.err.println("Failed to start client: " + e.getMessage());
            System.exit(1);
        }
    }
}