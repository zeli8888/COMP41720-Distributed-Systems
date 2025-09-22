import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class SocketTest {
    private static final int TEST_TIMEOUT = 5;
    private static ExecutorService serverExecutor;
    private static int serverPort;
    private static volatile boolean serverRunning;

    @BeforeAll
    static void setup() throws IOException, InterruptedException {
        // Find available port
        ServerSocket tempSocket = new ServerSocket(0);
        serverPort = tempSocket.getLocalPort();
        tempSocket.close();

        // Start server
        serverExecutor = Executors.newSingleThreadExecutor();
        serverRunning = true;
        serverExecutor.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
                System.out.println("[TEST SERVER] Listening on port " + serverPort);
                while (serverRunning) {
                    try {
                        Socket client = serverSocket.accept();
                        handleClient(client);
                    } catch (SocketException e) {
                        System.out.println("[TEST SERVER] Shutting down");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Wait for server startup
        Thread.sleep(1000);
    }

    private static void handleClient(Socket client) {
        new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                 PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

                String input;
                while ((input = in.readLine()) != null) {
                    out.println(input.toUpperCase());
                }
            } catch (IOException e) {
                System.out.println("[TEST SERVER] Client handling error: " + e.getMessage());
            } finally {
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @AfterAll
    static void tearDown() {
        serverRunning = false;
        serverExecutor.shutdownNow();
        try (Socket wakeup = new Socket()) {
            wakeup.connect(new InetSocketAddress("localhost", serverPort), 100);
        } catch (IOException ignored) {
        }
    }

    @Test
    @Timeout(TEST_TIMEOUT)
    void testConnectionEstablishment() throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", serverPort), 1000);
            assertTrue(socket.isConnected(), "Should establish connection");
        }
    }

    @Test
    @Timeout(TEST_TIMEOUT)
    void testMessageExchange() throws IOException {
        try (Socket socket = new Socket("localhost", serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Test single message
            out.println("hello");
            assertEquals("HELLO", in.readLine(), "Should uppercase request");

            // Test message sequence
            out.println("test1");
            out.println("test2");
            assertEquals("TEST1", in.readLine());
            assertEquals("TEST2", in.readLine());
        }
    }

    @Test
    @Timeout(TEST_TIMEOUT)
    void testErrorConditions() {
        // Test invalid port
        assertThrows(ConnectException.class, () -> {
            try (Socket socket = new Socket("localhost", serverPort + 1)) {
                fail("Should not connect to invalid port");
            }
        }, "Should throw for invalid port");

        // Test closed server
        tearDown();
        assertThrows(ConnectException.class, () -> {
            try (Socket socket = new Socket("localhost", serverPort)) {
                fail("Should not connect to stopped server");
            }
        }, "Should throw when server offline");
    }
}
