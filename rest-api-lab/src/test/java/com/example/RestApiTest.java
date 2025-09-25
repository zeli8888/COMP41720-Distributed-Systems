package com.example;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class RestApiTest {

    @Autowired
    private MockMvc mockMvc;

    // CREATE Operations
    @Test
    void testCreateUser_ValidRequest_Returns201AndUserObject() throws Exception {
        String userId = UUID.randomUUID().toString();
        String userJson = String.format("""
            {
                "id": "%s",
                "firstName": "John",
                "lastName": "Doe"
            }
            """, userId);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void testCreateUser_MissingRequiredFields_Returns400() throws Exception {
        String invalidUserJson = """
            {
                "firstName": "John",
                "lastName": "Doe"
            }
            """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUserJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateUser_DuplicateId_Returns409() throws Exception {
        String userId = UUID.randomUUID().toString();
        String userJson = String.format("""
            {
                "id": "%s",
                "firstName": "John",
                "lastName": "Doe"
            }
            """, userId);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isConflict());
    }

    // READ Operations
    @Test
    void testGetUser_ExistingUser_Returns200AndUserObject() throws Exception {
        String userId = UUID.randomUUID().toString();
        String userJson = String.format("""
            {
                "id": "%s",
                "firstName": "Alice",
                "lastName": "Smith"
            }
            """, userId);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Smith"));
    }

    @Test
    void testGetUser_NonExistingUser_Returns404() throws Exception {
        String nonExistingId = UUID.randomUUID().toString();
        mockMvc.perform(get("/api/users/{id}", nonExistingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetAllUsers_ReturnsListOfUsers() throws Exception {
        String user1 = UUID.randomUUID().toString();
        String user2 = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                    {
                        "id": "%s",
                        "firstName": "User1",
                        "lastName": "Test"
                    }
                    """, user1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                    {
                        "id": "%s",
                        "firstName": "User2",
                        "lastName": "Test"
                    }
                    """, user2)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[*].id", allOf(hasItem(user1), hasItem(user2))))
                .andExpect(jsonPath("$[*].firstName").exists())
                .andExpect(jsonPath("$[*].lastName").exists());
    }

    // UPDATE Operations
    @Test
    void testUpdateUser_ValidRequest_Returns200AndUpdatedUser() throws Exception {
        String userId = UUID.randomUUID().toString();
        String createJson = String.format("""
            {
                "id": "%s",
                "firstName": "Original",
                "lastName": "Name"
            }
            """, userId);

        String updateJson = String.format("""
            {
                "id": "%s",
                "firstName": "Updated",
                "lastName": "User"
            }
            """, userId);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("User"));
    }

    @Test
    void testUpdateUser_IdMismatch_Returns400() throws Exception {
        String userId = UUID.randomUUID().toString();
        String json = String.format("""
            {
                "id": "differentId",
                "firstName": "Test",
                "lastName": "User"
            }
            """);

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateUser_NonExistingUser_Returns404() throws Exception {
        String nonExistingId = UUID.randomUUID().toString();
        String json = String.format("""
            {
                "id": "%s",
                "firstName": "Test",
                "lastName": "User"
            }
            """, nonExistingId);

        mockMvc.perform(put("/api/users/{id}", nonExistingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    // DELETE Operations
    @Test
    void testDeleteUser_ExistingUser_Returns204() throws Exception {
        String userId = UUID.randomUUID().toString();
        String json = String.format("""
            {
                "id": "%s",
                "firstName": "Delete",
                "lastName": "Me"
            }
            """, userId);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteUser_NonExistingUser_Returns404() throws Exception {
        String nonExistingId = UUID.randomUUID().toString();
        mockMvc.perform(delete("/api/users/{id}", nonExistingId))
                .andExpect(status().isNotFound());
    }
}
