package com.example;

import com.example.grpc.UserServiceProto;
import com.example.model.User;
import com.example.service.UserService;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GrpcTest {

    @Autowired
    private UserServiceGrpcImpl userServiceGrpc;

    @MockitoBean
    private UserService userService;

    private Server server;
    private ManagedChannel channel;
    private com.example.grpc.UserServiceGrpc.UserServiceBlockingStub blockingStub;
    private com.example.grpc.UserServiceGrpc.UserServiceStub asyncStub;

    private final String serverName = "test-server";

    @BeforeEach
    void setUp() throws Exception {
        reset(userService);
        setupNormalMockBehavior();

        // Create in-process server and channel
        server = InProcessServerBuilder.forName(serverName)
                .addService(userServiceGrpc)
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName).build();
        blockingStub = com.example.grpc.UserServiceGrpc.newBlockingStub(channel);
        asyncStub = com.example.grpc.UserServiceGrpc.newStub(channel);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (channel != null) {
            channel.shutdown();
            channel.awaitTermination(1, TimeUnit.SECONDS);
        }
        if (server != null) {
            server.shutdown();
            server.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    private void setupNormalMockBehavior() {
        // Mock data
        User user1 = createUser("1", "John Doe", "john@example.com");
        User user2 = createUser("2", "Jane Smith", "jane@example.com");
        List<User> userList = Arrays.asList(user1, user2);

        // Mock method behaviors
        when(userService.getUserById("1")).thenReturn(user1);
        when(userService.getUserById("2")).thenReturn(user2);
        when(userService.getUserById("999")).thenThrow(new RuntimeException("User not found"));

        when(userService.getAllUsers()).thenReturn(userList);

        when(userService.createUser(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return createUser("3", user.getName(), user.getEmail());
        });

        when(userService.updateUser(any(String.class), any(User.class))).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            User user = invocation.getArgument(1);
            return createUser(id, user.getName(), user.getEmail());
        });

        doNothing().when(userService).deleteUser("1");
        doNothing().when(userService).deleteUser("2");
        doThrow(new RuntimeException("User not found")).when(userService).deleteUser("999");
    }

    private User createUser(String id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    // ========== SERVICE METHOD INVOCATION TESTS ==========

    @Test
    @Order(1)
    void testGetUser_Success() {
        // When
        UserServiceProto.User response = blockingStub.getUser(
                UserServiceProto.UserRequest.newBuilder().setId("1").build());

        // Then
        assertNotNull(response);
        assertEquals("1", response.getId());
        assertEquals("John Doe", response.getName());
        assertEquals("john@example.com", response.getEmail());
    }

    @Test
    @Order(2)
    void testCreateUser_Success() {
        // Given
        UserServiceProto.CreateUserRequest request = UserServiceProto.CreateUserRequest.newBuilder()
                .setName("New User")
                .setEmail("new@example.com")
                .build();

        // When
        UserServiceProto.User response = blockingStub.createUser(request);

        // Then
        assertNotNull(response);
        assertEquals("3", response.getId());
        assertEquals("New User", response.getName());
        assertEquals("new@example.com", response.getEmail());
    }

    @Test
    @Order(3)
    void testUpdateUser_Success() {
        // Given
        UserServiceProto.UpdateUserRequest request = UserServiceProto.UpdateUserRequest.newBuilder()
                .setId("1")
                .setName("Updated Name")
                .setEmail("updated@example.com")
                .build();

        // When
        UserServiceProto.User response = blockingStub.updateUser(request);

        // Then
        assertNotNull(response);
        assertEquals("1", response.getId());
        assertEquals("Updated Name", response.getName());
        assertEquals("updated@example.com", response.getEmail());
    }

    @Test
    @Order(4)
    void testDeleteUser_Success() {
        // When & Then - should not throw exception
        assertDoesNotThrow(() -> {
            blockingStub.deleteUser(
                    UserServiceProto.UserRequest.newBuilder().setId("1").build());
        });

        verify(userService, times(1)).deleteUser("1");
    }

    @Test
    @Order(5)
    void testListUsers_Success() {
        // When
        UserServiceProto.UserList response = blockingStub.listUsers(
                UserServiceProto.Empty.newBuilder().build());

        // Then
        assertNotNull(response);
        assertEquals(2, response.getUsersCount());

        UserServiceProto.User user1 = response.getUsers(0);
        assertEquals("1", user1.getId());
        assertEquals("John Doe", user1.getName());
        assertEquals("john@example.com", user1.getEmail());

        UserServiceProto.User user2 = response.getUsers(1);
        assertEquals("2", user2.getId());
        assertEquals("Jane Smith", user2.getName());
        assertEquals("jane@example.com", user2.getEmail());
    }

    // ========== DATA SERIALIZATION/DESERIALIZATION TESTS ==========

    @Test
    @Order(6)
    void testDataSerialization_UserRequest() {
        // Given
        String expectedId = "test-123";

        // When
        UserServiceProto.UserRequest request = UserServiceProto.UserRequest.newBuilder()
                .setId(expectedId)
                .build();

        // Then
        assertEquals(expectedId, request.getId());
    }

    @Test
    @Order(7)
    void testDataSerialization_CreateUserRequest() {
        // Given
        String expectedName = "Test User";
        String expectedEmail = "test@example.com";

        // When
        UserServiceProto.CreateUserRequest request = UserServiceProto.CreateUserRequest.newBuilder()
                .setName(expectedName)
                .setEmail(expectedEmail)
                .build();

        // Then
        assertEquals(expectedName, request.getName());
        assertEquals(expectedEmail, request.getEmail());
    }

    @Test
    @Order(8)
    void testDataSerialization_UserResponse() {
        // Given
        String expectedId = "123";
        String expectedName = "Serialization Test";
        String expectedEmail = "serialization@test.com";

        // When
        UserServiceProto.User user = UserServiceProto.User.newBuilder()
                .setId(expectedId)
                .setName(expectedName)
                .setEmail(expectedEmail)
                .build();

        // Then
        assertEquals(expectedId, user.getId());
        assertEquals(expectedName, user.getName());
        assertEquals(expectedEmail, user.getEmail());
    }

    @Test
    @Order(9)
    void testDataSerialization_UserList() {
        // Given
        UserServiceProto.User user1 = UserServiceProto.User.newBuilder()
                .setId("1")
                .setName("User 1")
                .setEmail("user1@test.com")
                .build();

        UserServiceProto.User user2 = UserServiceProto.User.newBuilder()
                .setId("2")
                .setName("User 2")
                .setEmail("user2@test.com")
                .build();

        // When
        UserServiceProto.UserList userList = UserServiceProto.UserList.newBuilder()
                .addUsers(user1)
                .addUsers(user2)
                .build();

        // Then
        assertEquals(2, userList.getUsersCount());
        assertEquals("User 1", userList.getUsers(0).getName());
        assertEquals("User 2", userList.getUsers(1).getName());
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    @Order(10)
    void testGetUser_NotFound() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        // When
        asyncStub.getUser(UserServiceProto.UserRequest.newBuilder().setId("999").build(),
                new StreamObserver<UserServiceProto.User>() {
                    @Override
                    public void onNext(UserServiceProto.User value) {
                        // Should not be called for error case
                    }

                    @Override
                    public void onError(Throwable t) {
                        errorRef.set(t);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        // Should not be called for error case
                    }
                });

        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        Throwable error = errorRef.get();
        assertNotNull(error);
        assertTrue(error.getMessage().contains("User not found"));
    }

    @Test
    @Order(11)
    void testCreateUser_InvalidData() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        // Mock service to throw exception
        when(userService.createUser(any(User.class))).thenThrow(new RuntimeException("Invalid user data"));

        // When
        asyncStub.createUser(UserServiceProto.CreateUserRequest.newBuilder()
                        .setName("")
                        .setEmail("invalid-email")
                        .build(),
                new StreamObserver<UserServiceProto.User>() {
                    @Override
                    public void onNext(UserServiceProto.User value) {
                        // Should not be called for error case
                    }

                    @Override
                    public void onError(Throwable t) {
                        errorRef.set(t);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        // Should not be called for error case
                    }
                });

        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        Throwable error = errorRef.get();
        assertNotNull(error);
        assertTrue(error.getMessage().contains("Invalid user data"));
    }

    @Test
    @Order(12)
    void testUpdateUser_ServiceError() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        // Mock service to throw exception
        when(userService.updateUser(any(String.class), any(User.class)))
                .thenThrow(new RuntimeException("Update failed"));

        // When
        asyncStub.updateUser(UserServiceProto.UpdateUserRequest.newBuilder()
                        .setId("999")
                        .setName("Test")
                        .setEmail("test@test.com")
                        .build(),
                new StreamObserver<UserServiceProto.User>() {
                    @Override
                    public void onNext(UserServiceProto.User value) {
                        // Should not be called for error case
                    }

                    @Override
                    public void onError(Throwable t) {
                        errorRef.set(t);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        // Should not be called for error case
                    }
                });

        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        Throwable error = errorRef.get();
        assertNotNull(error);
        assertTrue(error.getMessage().contains("Update failed"));
    }

    @Test
    @Order(13)
    void testDeleteUser_ServiceError() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        // Mock service to throw exception
        doThrow(new RuntimeException("Delete failed")).when(userService).deleteUser("999");

        // When
        asyncStub.deleteUser(UserServiceProto.UserRequest.newBuilder()
                        .setId("999")
                        .build(),
                new StreamObserver<UserServiceProto.Empty>() {
                    @Override
                    public void onNext(UserServiceProto.Empty value) {
                        // Should not be called for error case
                    }

                    @Override
                    public void onError(Throwable t) {
                        errorRef.set(t);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        // Should not be called for error case
                    }
                });

        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        Throwable error = errorRef.get();
        assertNotNull(error);
        assertTrue(error.getMessage().contains("Delete failed"));
    }

    @Test
    @Order(14)
    void testListUsers_ServiceError() throws InterruptedException {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();

        // Mock service to throw exception
        when(userService.getAllUsers())
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        asyncStub.listUsers(UserServiceProto.Empty.newBuilder().build(),
                new StreamObserver<UserServiceProto.UserList>() {
                    @Override
                    public void onNext(UserServiceProto.UserList value) {
                        // Should not be called for error case
                    }

                    @Override
                    public void onError(Throwable t) {
                        errorRef.set(t);
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        // Should not be called for error case
                    }
                });

        // Then
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        Throwable error = errorRef.get();
        assertNotNull(error);
        assertTrue(error.getMessage().contains("Database connection failed"));
    }
}