package com.example;

import com.example.grpc.UserServiceGrpc;
import com.example.grpc.UserServiceProto;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Scanner;

public class UserClient {

    private final UserServiceGrpc.UserServiceBlockingStub blockingStub;
    private final Scanner scanner;

    public UserClient(String host, int port) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = UserServiceGrpc.newBlockingStub(channel);
        this.scanner = new Scanner(System.in);
    }

    public void startInteractiveMode() {
        System.out.println("=== gRPC User Client ===");
        System.out.println("Connected to server successfully!");
        System.out.println("Available commands:");
        System.out.println("1. create - Create a new user");
        System.out.println("2. get [id] - Get user by ID");
        System.out.println("3. update [id] - Update user");
        System.out.println("4. delete [id] - Delete user");
        System.out.println("5. list - List all users");
        System.out.println("6. exit - Exit client");
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
        System.out.println("  exit      - Exit client");
        System.out.println("  help      - Show this help");
    }

    public static void main(String[] args) {
        String host = System.getenv().getOrDefault("GRPC_SERVER_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("GRPC_SERVER_PORT", "50051"));

        try {
            UserClient client = new UserClient(host, port);
            client.startInteractiveMode();
        } catch (Exception e) {
            System.err.println("Failed to start client: " + e.getMessage());
            System.exit(1);
        }
    }
}