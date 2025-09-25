import org.junit.jupiter.api.*;
import java.io.*;
import java.net.*;
import static org.junit.jupiter.api.Assertions.*;

public class ServerTest {
    private static final int TEST_PORT = 8080;
    private static final String TEST_HOST = "localhost";
    private static final int INVALID_PORT = 8081;

    @BeforeEach
    void startServer() throws InterruptedException {
        // Start server in separate thread
        new Thread(() -> Server.start()).start();

        // Wait for server initialization
        int retries = 0;
        while (retries++ < 50) { // 5 second timeout
            try (Socket s = new Socket(TEST_HOST, TEST_PORT)) {
                return;
            } catch (ConnectException e) {
                Thread.sleep(100);
            } catch (IOException e) {
                fail("Unexpected error: " + e.getMessage());
            }
        }
        fail("Server failed to start within 5 seconds");
    }

    @AfterEach
    void stopServer() {
        Server.stop();
    }

    @Test
    void testConnectionEstablishment() {
        // Verify client can establish connection
        assertDoesNotThrow(() -> {
            try (Socket socket = new Socket(TEST_HOST, TEST_PORT)) {
                assertTrue(socket.isConnected());
            }
        }, "Should successfully connect to server");
    }

    @Test
    void testMessageSendingReceiving() throws IOException {
        // Test complete request-response cycle
        try (Socket socket = new Socket(TEST_HOST, TEST_PORT);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(
                     socket.getOutputStream(), true)) {

            // Send test message
            out.println("junit test");

            // Verify response
            assertEquals("JUNIT TEST", in.readLine(),
                    "Should receive uppercase response");
        }
    }

    @Test
    void testErrorHandlingForInvalidConnections() {
        // Test connection to invalid port
        ConnectException exception = assertThrows(ConnectException.class, () -> {
            try (Socket socket = new Socket(TEST_HOST, INVALID_PORT)) {
                fail("Should not connect to invalid port");
            }
        });

        // Verify exception message
        assertTrue(exception.getMessage().contains("Connection refused")
                        || exception.getMessage().contains("connect timed out"),
                "Should throw proper connection error");
    }
}
