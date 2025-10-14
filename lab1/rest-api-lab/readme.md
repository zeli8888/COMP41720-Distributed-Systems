## System Design Documentation
### System Overview
The system is a RESTful API for managing user entities, 
supporting CRUD operations (Create, Read, Update, Delete). 
It follows a layered architecture:
- API Layer: Handles HTTP requests/responses via Spring MVC controllers.
- Service Layer: Enforces business logic, validation, and error handling.
- Data Layer: Uses Spring Data JPA with Hibernate for database operations and H2 as an in-memory database.
### Synchronous Communication Demonstration
The API uses synchronous request-response communication, where clients block until a response is received. 
For example:
1. POST /api/users:
   - Client waits for a 201 Created response with the created user or an error (e.g., 409 Conflict).
2. GET /api/users/{id}:
   - Client blocks until the server returns either the user (200 OK) or 404 Not Found.
3. Error states (e.g., invalid data, missing fields) are immediately communicated via HTTP status codes (4xx), ensuring clients receive instant feedback.
### Technology Choices
- H2 Database
  - Purpose: Lightweight, in-memory database for rapid development/testing.
  - Advantages:
     - Zero configuration for integration with Spring Boot.
     - Supports SQL and persists data only during the application lifecycle (ideal for testing).
- Spring Data JPA (Hibernate)
  - Purpose: Simplify database operations via JPA abstractions.
  - Advantages:
     - Auto-generated queries via method names (e.g., findById, deleteById).
     - Transaction management and optimistic locking.
     - Seamless integration with H2.
- Spring Boot
  - Purpose: Streamline application setup and dependency management.
  - Key Features:
     - Auto-configuration for Spring Data JPA and H2.
     - Embedded Tomcat server for HTTP handling.
     - Dependency injection via @Autowired and Lombok’s @RequiredArgsConstructor.
- Testing Framework
  - JUnit 5 + MockMvc:
     - Simulate HTTP requests and validate responses.
     - Test edge cases (e.g., duplicate IDs, invalid payloads).
### API Specification

| **Endpoint**            | **Method** | **Description**                              | **Success Status** | **Error Cases**                          |  
|--------------------------|------------|----------------------------------------------|--------------------|------------------------------------------|  
| `/api/users`             | `GET`      | Fetch all users                              | `200 OK`           | N/A                                      |  
| `/api/users/{id}`        | `GET`      | Fetch user by ID                             | `200 OK`           | `404 Not Found`                          |  
| `/api/users`             | `POST`     | Create a user                                | `201 Created`      | `400 Bad Request`       |  
| `/api/users/{id}`        | `PUT`      | Update a user                                | `200 OK`           | `400 Bad Request`, `404 Not Found`       |  
| `/api/users/{id}`        | `DELETE`   | Delete a user                                | `204 No Content`   | `404 Not Found`                          |  "

### Data Model
```bash
public class User {  
    private String id;          // Unique identifier 
    private String name;   // Required field, allow deplicate name
    private String email;    // Required field, allow deplicate email (we allow users to register multiple accounts with same email)
}  
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
docker build -t rest-api-lab .
```

[//]: # (docker tag rest-api-lab zeli8888/rest-api-lab && docker push zeli8888/rest-api-lab)

2. Run Container:
```bash
docker run -d -p 8081:8081 --name rest-api-lab rest-api-lab
```

[//]: # (Or use my docker image:)

[//]: # (```bash)

[//]: # (docker pull zeli8888/rest-api-lab)

[//]: # (docker run -d -p 8081:8081 --name rest-api-lab zeli8888/rest-api-lab)

[//]: # (```)

3. Clean Up:
```bash
docker stop rest-api-lab
docker rm rest-api-lab
docker rmi rest-api-lab
```