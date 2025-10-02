package com.example;
import com.example.grpc.UserServiceProto;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class UserServiceGrpcImpl extends com.example.grpc.UserServiceGrpc.UserServiceImplBase {

    @Autowired
    private com.example.service.UserService userService;

    @Override
    public void getUser(UserServiceProto.UserRequest request,
                        StreamObserver<UserServiceProto.User> responseObserver) {
        try {
            com.example.model.User user = userService.getUserById(request.getId());
            UserServiceProto.User response = UserServiceProto.User.newBuilder()
                    .setId(user.getId())
                    .setName(user.getName())
                    .setEmail(user.getEmail())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void createUser(UserServiceProto.CreateUserRequest request,
                           StreamObserver<UserServiceProto.User> responseObserver) {
        try {
            com.example.model.User user = new com.example.model.User();
            user.setName(request.getName());
            user.setEmail(request.getEmail());

            com.example.model.User createdUser = userService.createUser(user);
            UserServiceProto.User response = UserServiceProto.User.newBuilder()
                    .setId(createdUser.getId())
                    .setName(createdUser.getName())
                    .setEmail(createdUser.getEmail())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateUser(UserServiceProto.UpdateUserRequest request,
                           StreamObserver<UserServiceProto.User> responseObserver) {
        try {
            com.example.model.User user = new com.example.model.User();
            user.setId(request.getId());
            user.setName(request.getName());
            user.setEmail(request.getEmail());

            com.example.model.User updatedUser = userService.updateUser(request.getId(), user);
            UserServiceProto.User response = UserServiceProto.User.newBuilder()
                    .setId(updatedUser.getId())
                    .setName(updatedUser.getName())
                    .setEmail(updatedUser.getEmail())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteUser(UserServiceProto.UserRequest request,
                           StreamObserver<UserServiceProto.Empty> responseObserver) {
        try {
            userService.deleteUser(request.getId());
            responseObserver.onNext(UserServiceProto.Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void listUsers(UserServiceProto.Empty request,
                          StreamObserver<UserServiceProto.UserList> responseObserver) {
        try {
            java.util.List<com.example.model.User> users = userService.getAllUsers();
            UserServiceProto.UserList.Builder userListBuilder = UserServiceProto.UserList.newBuilder();

            for (com.example.model.User user : users) {
                UserServiceProto.User grpcUser = UserServiceProto.User.newBuilder()
                        .setId(user.getId())
                        .setName(user.getName())
                        .setEmail(user.getEmail())
                        .build();
                userListBuilder.addUsers(grpcUser);
            }

            responseObserver.onNext(userListBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }
}