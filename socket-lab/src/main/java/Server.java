// Create ServerSocket on port 8080
// While server is running:
// - Accept client connection
// - Create input/output streams
// - Read client message
// - Process request (e.g., convert to uppercase)
// - Send response back to client
// - Close connection
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    private static ServerSocket serverSocket;
    private static final AtomicBoolean isRunning = new AtomicBoolean(false);
    private static Thread serverThread;

    public static void main(String[] args) {
        start();
    }

    public static void start() {
        if (isRunning.get()) return;

        isRunning.set(true);
        serverThread = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(8080)) {
                serverSocket = ss;
                System.out.println("Server started on port 8080");

                while (isRunning.get()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        new ClientHandler(clientSocket).start();
                    } catch (SocketException e) {
                        if (!isRunning.get()) {
                            System.out.println("Server stopped normally");
                        }
                    }
                }
            } catch (IOException e) {
                if (isRunning.get()) {
                    System.err.println("Server exception: " + e.getMessage());
                }
            } finally {
                isRunning.set(false);
            }
        });
        serverThread.start();
    }

    public static void stop() {
        if (!isRunning.get()) return;

        isRunning.set(false);
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }

    // Client handling thread
    private static class ClientHandler extends Thread {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(
                         clientSocket.getOutputStream(), true)) {

                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Continuously read client requests
                String request;
                while ((request = in.readLine()) != null) {
                    if (request.isEmpty() || "exit".equalsIgnoreCase(request.trim())) {
                        break;
                    }
                    System.out.println("Received request: " + request);

                    // Process request and send response
                    String response = request.toUpperCase();
                    out.println(response);
                    System.out.println("Sent response: " + response);
                }
                System.out.println("Client Connection closed");
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing socket: " + e.getMessage());
                }
            }
        }
    }
}

