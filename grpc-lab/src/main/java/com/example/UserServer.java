package com.example;

// no need for this, since I'm using spring boot + grpc
//public class UserServer {
//    private Server server;
//
//    public void start() throws IOException {
//        server = ServerBuilder.forPort(8080)
//                .addService(new UserServiceImpl())
//                .build()
//                .start();
//    }
//
//    static class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {
//        @Override
//        public void getUser(UserRequest request, StreamObserver<User> responseObserver) {
//            // Implement getUser logic
//        }
//
//        // Implement other methods
//    }
//}

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UserServer {

    // import UserClient to compile by force
    @SuppressWarnings("unused")
    private static final Class<?> USER_CLIENT_CLASS = UserClient.class;

    public static void main(String[] args) {
        String runMode = System.getenv().getOrDefault("RUN_MODE", "server");

        if ("client".equals(runMode)) {
            // run client
            UserClient.main(args);
        } else {
            // run server
            SpringApplication.run(UserServer.class, args);
        }
    }
}
