/**
 * @Author : Ze Li
 * @Date : 25/09/2025 16:44
 * @Version : V1.0
 * @Description :
 */
import org.junit.jupiter.api.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ClientTest {
    private static final int TEST_PORT = 8080;
    private static final String TEST_HOST = "localhost";
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream originalOut;
    private InputStream originalIn;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() throws Exception {
        originalIn = System.in;
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outContent));

        new Thread(() -> Server.start()).start();
        waitForServerStart();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setIn(originalIn);
        System.setErr(originalErr);
        Server.stop();
    }

    private void waitForServerStart() throws InterruptedException {
        int retries = 0;
        while (retries++ < 50) {
            try (Socket s = new Socket(TEST_HOST, TEST_PORT)) {
                return;
            } catch (ConnectException e) {
                Thread.sleep(100);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        fail("Server startup timeout");
    }

    @Test
    void testClientWorkflow() throws Exception {
        String simulatedInput = "Hello\nTest Data\nexit\n";
        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));

        // start the client in a separate thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> Client.start());

        // wait for the client to exit
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            fail("Client didn't exit properly");
        }

        // verify the console output
        String consoleOutput = outContent.toString();
        System.setOut(originalOut);
        System.out.println(consoleOutput);
        assertAll(
                () -> assertTrue(consoleOutput.contains("Enter message"),
                        "Should display input prompt"),
                () -> assertTrue(consoleOutput.contains("Sent: Hello"),
                        "Should show sent message"),
                () -> assertTrue(consoleOutput.contains("Received: HELLO"),
                        "Should display server response"),
                () -> assertTrue(consoleOutput.contains("Connection closed"),
                        "Should show exit confirmation")
        );
    }

    @Test
    void testErrorHandlingForInvalidConnections() throws Exception {
        // stop the server to test error handling
        Server.stop();
        waitForServerStop();

        // simulate user input
        String simulatedInput = "Hello\nTest Data\nexit\n";
        System.setIn(new ByteArrayInputStream(simulatedInput.getBytes()));

        // catch error message
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        // start the client in a separate thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> Client.start());

        // wait for the client to exit
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            fail("Client didn't exit properly");
        }

        // verify the console output
        String consoleOutput = errContent.toString();
        System.setOut(originalOut);
        System.out.println(consoleOutput);
        assertTrue(errContent.toString().contains("Connection refused")
                        || errContent.toString().contains("connect timed out") || errContent.toString().contains("I/O error: "),
                "Should show connection error"
        );

        executor.shutdownNow();
    }

    private void waitForServerStop() throws InterruptedException {
        int retries = 0;
        while (retries++ < 20) {
            try (Socket s = new Socket(TEST_HOST, TEST_PORT)) {
                Thread.sleep(100);
            } catch (IOException e) {
                return; // server stopped
            }
        }
        fail("Server didn't stop properly");
    }
}

