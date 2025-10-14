package com.example;

import com.example.model.User;
import com.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class RestApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    // CREATE Operations
    @Test
    void testCreateUser_ValidRequest_Returns201AndUserObject() throws Exception {
        String userJson = """
            {
                "name": "John Doe",
                "email": "john.doe@example.com"
            }
            """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void testCreateUser_MissingRequiredFields_Returns400() throws Exception {
        // Test missing name
        String invalidUserJson1 = """
            {
                "email": "test@example.com"
            }
            """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUserJson1))
                .andExpect(status().isBadRequest());

        // Test missing email
        String invalidUserJson2 = """
            {
                "name": "Test User"
            }
            """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUserJson2))
                .andExpect(status().isBadRequest());

        // Test both missing
        String invalidUserJson3 = "{}";

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUserJson3))
                .andExpect(status().isBadRequest());
    }

    // READ Operations
    @Test
    void testGetUser_ExistingUser_Returns200AndUserObject() throws Exception {
        // Create a user first
        User user = new User();
        user.setName("Alice Smith");
        user.setEmail("alice.smith@example.com");
        User savedUser = userRepository.save(user);

        mockMvc.perform(get("/api/users/{id}", savedUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.name").value("Alice Smith"))
                .andExpect(jsonPath("$.email").value("alice.smith@example.com"));
    }

    @Test
    void testGetUser_NonExistingUser_Returns404() throws Exception {
        String nonExistingId = "non-existing-id";
        mockMvc.perform(get("/api/users/{id}", nonExistingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllUsers_ReturnsListOfUsers() throws Exception {
        // Clear existing data and create test users
        userRepository.deleteAll();

        User user1 = new User();
        user1.setName("User1 Test");
        user1.setEmail("user1@example.com");
        userRepository.save(user1);

        User user2 = new User();
        user2.setName("User2 Test");
        user2.setEmail("user2@example.com");
        userRepository.save(user2);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id").exists())
                .andExpect(jsonPath("$[*].name").exists())
                .andExpect(jsonPath("$[*].email").exists());
    }

    // UPDATE Operations
    @Test
    void testUpdateUser_ValidRequest_Returns200AndUpdatedUser() throws Exception {
        // Create a user first
        User user = new User();
        user.setName("Original Name");
        user.setEmail("original@example.com");
        User savedUser = userRepository.save(user);

        String updateJson = String.format("""
            {
                "name": "Updated User",
                "email": "updated@example.com"
            }
            """);

        mockMvc.perform(put("/api/users/{id}", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedUser.getId()))
                .andExpect(jsonPath("$.name").value("Updated User"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    void testUpdateUser_MissingRequiredFields_Returns400() throws Exception {
        // Create a user first
        User user = new User();
        user.setName("Test User");
        user.setEmail("test@example.com");
        User savedUser = userRepository.save(user);

        // Test missing name
        String invalidJson1 = """
            {
                "email": "test@example.com"
            }
            """;

        mockMvc.perform(put("/api/users/{id}", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson1))
                .andExpect(status().isBadRequest());

        // Test missing email
        String invalidJson2 = """
            {
                "name": "Test User"
            }
            """;

        mockMvc.perform(put("/api/users/{id}", savedUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson2))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateUser_NonExistingUser_Returns404() throws Exception {
        String nonExistingId = "non-existing-id";
        String json = """
            {
                "name": "Test User",
                "email": "test@example.com"
            }
            """;

        mockMvc.perform(put("/api/users/{id}", nonExistingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    // DELETE Operations
    @Test
    void testDeleteUser_ExistingUser_Returns204() throws Exception {
        // Create a user first
        User user = new User();
        user.setName("Delete Me");
        user.setEmail("delete@example.com");
        User savedUser = userRepository.save(user);

        mockMvc.perform(delete("/api/users/{id}", savedUser.getId()))
                .andExpect(status().isNoContent());

        // Verify user is deleted
        mockMvc.perform(get("/api/users/{id}", savedUser.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteUser_NonExistingUser_Returns404() throws Exception {
        String nonExistingId = "non-existing-id";
        mockMvc.perform(delete("/api/users/{id}", nonExistingId))
                .andExpect(status().isNotFound());
    }
}