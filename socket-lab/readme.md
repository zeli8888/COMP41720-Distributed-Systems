## System Design Documentation
### System Overview

This system demonstrates a synchronous client-server communication model using Java sockets. 
The server processes client requests by converting input text to uppercase and returns the result immediately, 
enforcing a strict request-response pattern.

### Key Components

#### Server

- Port Binding: Listens on port 8080 using ServerSocket.
- Thread-Per-Client: Spawns a new ClientHandler thread for each connected client to handle I/O operations.
- Request Handling:
  - Reads messages from clients via BufferedReader
  - Processes requests (converts text to uppercase)
  - Sends responses back via PrintWriter.
- Graceful Shutdown:
  - Uses AtomicBoolean to manage server state.
  - Closes ServerSocket and client connections cleanly via stop().

#### Client

- Synchronous Workflow
    1. Connects to the server via Socket.
    2. Sends messages through PrintWriter.
    3. Blocks and waits for a response using BufferedReader.readLine().
    4. Repeats until the user types exit.
- Blocking I/O
  - The client thread pauses execution while awaiting server responses, ensuring strict request-response ordering.

### Synchronous Communication Demonstration

The system enforces synchronous communication through:
- Blocking Operations:
  - The client’s in.readLine() blocks until the server sends a response.
  - The server’s clientSocket.accept() blocks until a new connection arrives. 
- Sequential Processing:
  - Each client request must complete (send → process → reply) before the next input is accepted.
- No Asynchronous Callbacks:
  - No background threads or non-blocking I/O are used on the client side, ensuring simplicity and deterministic behavior.


### Technology Choices

#### Java Sockets
 - Why: Native support for TCP/IP communication, ideal for demonstrating low-level synchronous I/O.
 - Benefits: Simplicity, cross-platform compatibility, and direct control over connection lifecycle.

#### Multi-threaded Server
 - Why: Allows handling multiple clients simultaneously without blocking new connections.
 - Implementation: Each ClientHandler runs in a dedicated thread, isolating client sessions.

#### JUnit Testing
 - Why: Validates critical paths (connection handling, message processing, error recovery).
 - Key Tests:
   - testMessageSendingReceiving: Verifies uppercase conversion and response integrity.
   - testErrorHandlingForInvalidConnections: Ensures proper handling of refused connections.
   - testClientWorkflow: Simulates user input and validates end-to-end interaction.

#### AtomicBoolean for State Management
 - Why: Thread-safe flag to coordinate server shutdown across threads.

## Getting Started
### Run Test & Build:

```bash
mvn clean test package
```

### Build and Run Docker Image:

1. Build Image:
```bash
docker build -t socket-lab .
```

[//]: # (docker tag socket-lab zeli8888/socket-lab && docker push zeli8888/socket-lab)

2. Run Server Container:
```bash
docker run -d -p 8080:8080 -e APP=server --name socket-lab-server socket-lab
```

[//]: # (telnet localhost 8080)

3. Run Client Container:
```bash
docker run -it -e APP=client --rm --network="host" socket-lab
```
Or use my docker image: 
```bash
docker pull zeli8888/socket-lab
docker run -d -p 8080:8080 -e APP=server --name socket-lab zeli8888/socket-lab
docker run -it -e APP=client --rm --network="host" zeli8888/socket-lab-client
```

4. Clean Up:
```bash
docker stop socket-lab-server
docker rm socket-lab-server
docker rmi socket-lab zeli8888/socket-lab
```