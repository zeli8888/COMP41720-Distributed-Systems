// Create Socket connection to server
// Create input/output streams
// Send message to server
// Wait for response
// Print server response
// Close connection
import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(
                     socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(
                     new InputStreamReader(System.in))) {

            System.out.println("Connected to server at " + SERVER_IP + ":" + SERVER_PORT);

            // Synchronous communication loop
            while (true) {
                System.out.print("Enter message (type 'exit' to quit): ");
                String message = userInput.readLine();

                if (message == null || message.isEmpty() || "exit".equalsIgnoreCase(message.trim())) {
                    break;
                }

                // Send message to server
                out.println(message);
                System.out.println("Sent: " + message);

                // Wait for and display response
                String response = in.readLine();
                if (response == null) {
                    System.out.println("Server closed connection");
                    break;
                }
                System.out.println("Received: " + response);
            }

        } catch (UnknownHostException e) {
            System.err.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        }
        System.out.println("Connection closed");
    }
}

