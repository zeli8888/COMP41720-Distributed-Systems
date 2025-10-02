## System Design Documentation
### System Overview
The system is a gRPC-based service for managing user entities, supporting full CRUD operations (Create, Read, Update, Delete). It follows a clean, layered architecture:

- **gRPC Layer**: Handles gRPC request/response communication via auto-generated Protocol Buffer stubs
- **Service Layer**: Enforces business logic, validation, and error handling
- **Data Layer**: Uses Spring Data JPA with Hibernate for database operations

### Synchronous Communication Demonstration

This system demonstrates synchronous request-response communication using gRPC, where clients block until receiving a complete response from the server. Key characteristics:

1. **Blocking Operations**: Each gRPC call blocks the client until the server responds
2. **Immediate Feedback**: Error states (invalid data, missing users) are immediately communicated through gRPC status codes
3. **Request-Response Pattern**: All operations follow the classic synchronous pattern:
    - `GetUser`: Client waits for user data or "NOT_FOUND" error
    - `CreateUser`: Client blocks until receiving the created user entity or validation error
    - `UpdateUser`: Client receives immediate confirmation of update success/failure
    - `ListUsers`: Client waits for complete user list before proceeding

### Technology Choices

#### gRPC Framework
- **Purpose**: High-performance RPC framework for synchronous communication
- **Advantages**:
    - Protocol Buffers for efficient binary serialization
    - Strongly-typed service contracts
    - Built-in support for streaming and flow control
    - Cross-language compatibility

#### H2 Database
- **Purpose**: Lightweight, in-memory database for rapid development and testing
- **Advantages**:
    - Zero configuration setup with Spring Boot
    - SQL compliance with minimal footprint
    - Ideal for prototype and testing environments

#### Spring Data JPA (Hibernate)
- **Purpose**: Simplify database operations through JPA abstractions
- **Advantages**:
    - Auto-generated repository implementations
    - Transaction management and connection pooling
    - Seamless integration with H2 database

#### Spring Boot with Spring gRPC
- **Purpose**: Streamline gRPC service development and deployment
- **Key Features**:
    - Auto-configuration for Spring Data JPA and H2.
    - Dependency injection and lifecycle management
    - Integration with Spring ecosystem (JPA, Testing, etc.)

#### Testing Framework
- **JUnit 5 + gRPC In-Process Testing**:
    - In-memory gRPC channels for fast, isolated testing
    - Comprehensive coverage of success and error scenarios
    - Mock service layer for controlled test environments

### gRPC Service Definition

```protobuf
syntax = "proto3";
option java_package = "com.example.grpc";
option java_outer_classname = "UserServiceProto";
service UserService {
  rpc GetUser (UserRequest) returns (User);
  rpc CreateUser (CreateUserRequest) returns (User);
  rpc UpdateUser (UpdateUserRequest) returns (User);
  rpc DeleteUser (UserRequest) returns (Empty);
  rpc ListUsers (Empty) returns (UserList);
}
message UserRequest {
  string id = 1;
}
message CreateUserRequest {
  string name = 1;
  string email = 2;
}
message UpdateUserRequest {
  string id = 1;
  string name = 2;
  string email = 3;
}
message User {
  string id = 1;
  string name = 2;
  string email = 3;
}
message UserList {
  repeated User users = 1;
}
message Empty {}
```

---
## Getting Started
### Run Test & Build:

```bash
mvn clean test package
```

### Build and Run Docker Image:

1. Build Image:
```bash
docker build -t grpc-lab .
```

[//]: # (docker tag grpc-lab zeli8888/grpc-lab && docker push zeli8888/grpc-lab)

2. Run Server Container:
```bash
docker run -d -p 50051:50051 -e RUN_MODE=server --name grpc-lab-server grpc-lab
```

3. Run Client Container (contains restful and grpc benchmark test option):
```bash
docker run -it --rm -e RUN_MODE=client --network="host" grpc-lab
```

[//]: # (Or use my docker image:)

[//]: # (```bash)

[//]: # (docker pull zeli8888/grpc-lab)

[//]: # (docker run -d -p 8081:8081 --name grpc-lab zeli8888/grpc-lab)

[//]: # (```)

4. Clean Up:
```bash
docker stop grpc-lab-server
docker rm grpc-lab-server
docker rmi grpc-lab
```