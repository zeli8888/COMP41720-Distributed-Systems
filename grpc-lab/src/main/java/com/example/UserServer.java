package com.example;

public class UserServer {
    private Server server;

    public void start() throws IOException {
        server = ServerBuilder.forPort(8080)
                .addService(new UserServiceImpl())
                .build()
                .start();
    }

    static class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
        @Override
        public void getUser(UserRequest request, StreamObserver<User> responseObserver) {
            // Implement getUser logic
        }

        // Implement other methods
    }
}