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

public class Server {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Server started on port 8080");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Create new thread for each client connection
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
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
                    if (request.isEmpty()) {
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

