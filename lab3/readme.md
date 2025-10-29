# Introduction
## Source Code Repository Link
- Markdown version of this report is highly recommended: https://github.com/zeli8888/COMP41720-Distributed-Systems/blob/main/lab3/readme.md
- The complete code for this lab is available at: https://github.com/zeli8888/COMP41720-Distributed-Systems/tree/main/lab3
- To fetch the code and run on your computer, use:
    ```bash
    git clone https://github.com/zeli8888/COMP41720-Distributed-Systems.git
    cd lab3
    ```
## Lab Purpose
- Understand common failure modes in distributed systems, such as network issues, process crashes, and slow responses.
- Implement key resilience patterns, including Circuit Breakers, Retries, and Backoff strategies, within a distributed application. 
- Apply chaos engineering tools to simulate failures (e.g., network partitions, node shutdowns) in a controlled environment.
- Analyze and compare system behavior with and without implemented resilience patterns, identifying the practical benefits and trade-offs in terms of system availability, performance, and fault tolerance.
- Reason architecturally about designing systems that can gracefully handle partial failures and ensure reliability.
## Tools & Environment
- Client Application Language: JDK 17, Spring Boot, Maven
- Deployment Environment: Kubernetes with Minikube, Docker
- Resilience Libraries/Frameworks: Circuit Breaker and Retry & Backoff with Resilience4j
- Chaos Engineering Tool: Chaos Toolkit
## Basic Functionality
The distributed application consists of two main services:
1. **Client Service**: A Spring Boot application with interactive terminal interface to send requests to the Server Service and display responses.
2. **Server Service**: A Spring Boot application that responds to requests with a simple "Hello from server" message. Integrated with simulated delays and failures to mimic real-world conditions.



# Setup & Configuration
<!-- Detail your application components, Docker images, and Kubernetes deployment (including YAML manifests). Provide a clear
diagram of your deployed system -->
## Simple Application
- System Diagram
```bash
┌─────────────────┐     HTTP REST Calls     ┌─────────────────┐
│   ClientService │────────────────────────▶│   ServerService │
│                 │                         │                 │
│ - Interactive   │◀─────────────────────── │ - Hello Endpoint│
│   Terminal      │        Response         │ - Fail Sim      │
│                 │                         │ - Delay Sim     │
│ - Two Clients:  │                         │ - Chaos Sim     │
│   • Without     │                         │                 │
│     Resilience  │                         └─────────────────┘
│   • With        │                                   ▲
│     Resilience  │                                   │
│     (Circuit    │                         Deployment│
│     Breaker,    │                                   │
│     Retry)      │                         ┌─────────────────┐
└─────────────────┘                         │   Kubernetes    │
         ▲                                  │    Cluster      │
         │                                  │                 │
         │                                  │ - Minikube      │
         │                                  │ - Deployments   │
         │                                  │ - Services      │
         │                                  │ - ConfigMaps    │
         │                                  └─────────────────┘
         │                                          │
         │                                          │
         └──────────────────────────────────────────┘
                            Deployment


Component Flow:
User → ClientService Terminal → HTTP REST → ServerService Endpoints
              │                            │
              │                            │
              ▼                            ▼
       with/without Resilience4j     Failure/Delay/Chaos
       (Circuit Breaker, Retry)        Simulations


Kubernetes Deployment:
┌─────────────┐    Service    ┌─────────────┐
│ Client Pod  │───────────────│ Server Pod  │
│             │               │             │
│ - clientservice│            │ - serverservice│
│ - ConfigMap │               │ - Port 8080 │
│   injection │               └─────────────┘
└─────────────┘
```
- ClientService with interactive terminal for user to send hello messages using synchronous HTTP REST calls with and without resilience mechanism to server

    ```java
    public interface ClientWithoutResilience {
        @GetExchange("/hello")
        String callHello();

        @GetExchange("/hello-fail")
        String callHelloFail(@RequestParam boolean shouldFail);

        @GetExchange("/hello-delay")
        String callHelloDelay(@RequestParam long delayMs);

        @GetExchange("/hello-chaos")
        String callHelloChaos(@RequestParam int chaosPercent);
    }

    public interface ClientResilience {
        // Methods with Resilience4j annotations
        @CircuitBreaker(name = "serverService")
        @Retry(name = "serverService")
        @GetExchange("/hello")
        String callHello();

        @CircuitBreaker(name = "serverService")
        @Retry(name = "serverService")
        @GetExchange("/hello-fail")
        String callHelloFail(@RequestParam boolean shouldFail);

        @CircuitBreaker(name = "serverService")
        @Retry(name = "serverService")
        @GetExchange("/hello-delay")
        String callHelloDelay(@RequestParam long delayMs);

        @CircuitBreaker(name = "serverService")
        @Retry(name = "serverService")
        @GetExchange("/hello-chaos")
        String callHelloChaos(@RequestParam int chaosPercent);
    }
    ```
- ServerService with different endpoints for hello messages to simulate failure, delay, chaos in real-word conditions.

    ```java
    @RestController
    @RequestMapping("/api")
    public class HelloController {

        @GetMapping("/hello")
        public String hello() {
            System.out.println("Received request for /hello at " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
            return "Hello from Server";
        }

        @GetMapping("/hello-fail")
        public String hello(@RequestParam("shouldFail") boolean shouldFail) {
            System.out.println("Received request for /hello-fail with shouldFail: " + shouldFail + ", at " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
            if (shouldFail) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated failure");
            }
            return "Hello from Server";
        }

        @GetMapping("/hello-delay")
        public String helloDelay(@RequestParam("delayMs") long delayMs) throws InterruptedException {
            System.out.println("Received request for /hello-delay with delayMs: " + delayMs + ", at " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
            Thread.sleep(delayMs);
            return "Hello from Server after delay of " + delayMs + " ms";
        }

        @GetMapping("/hello-chaos")
        public String helloChaos(@RequestParam("chaosPercent") int chaosPercent) {
            System.out.println("Received request for /hello-chaos with chaosPercent: " + chaosPercent + ", at " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")));
            int randomValue = (int) (Math.random() * 100);
            if (randomValue < chaosPercent) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Simulated chaos failure");
            }
            return "Hello from Server with chaos percent of " + chaosPercent + "%";
        }
    }
    ```
## Kubernetes Deployment
- Docker Images for two Spring Boot Applications
    ```dockerfile
    FROM openjdk:17-jdk-slim
    WORKDIR /app
    COPY target/ServerService-0.0.1-SNAPSHOT.jar app.jar
    ENTRYPOINT ["java", "-jar", "app.jar"]
    ```
    ```dockerfile
    FROM openjdk:17-jdk-slim
    WORKDIR /app
    COPY target/ClientService-0.0.1-SNAPSHOT.jar app.jar
    ENTRYPOINT ["java", "-jar", "app.jar"]
    ```
- Containerize both ClientService and ServerService using Docker
    ```bash
    cd ClientService && mvn clean package && docker build -t clientservice .
    cd ../ServerService && mvn clean package && docker build -t serverservice .
    # Tag and push images to docker hub for Kubernetes to pull
    docker tag clientservice zeli8888/clientservice
    docker tag serverservice zeli8888/serverservice
    docker push zeli8888/clientservice && docker push zeli8888/serverservice
    ```
- Deploy application on a Kubernetes cluster using minikube with manifests:
  - server.yaml: one Deployment for ServerService, one Service for network access to created pods.
    ```yaml
    apiVersion: apps/v1
    kind: Deployment
    metadata:
    name: server
    spec:
    replicas: 1
    selector:
        matchLabels:
        app: server
    template:
        metadata:
        labels:
            app: server
        spec:
        containers:
        - name: server
            image: zeli8888/serverservice
            ports:
            - containerPort: 8080
    ---
    apiVersion: v1
    kind: Service
    metadata:
    name: server-service
    spec:
    selector:
        app: server
    ports:
    - port: 8080
        targetPort: 8080
    type: ClusterIP
    ```
  - client.yaml: one Deployment for ClientService, one ConfigMap for configuration injection.
    ```yaml
    apiVersion: v1
    kind: ConfigMap
    metadata:
    name: client-service-config
    data:
    server.uri: "http://server-service:8080/api/"
    ---
    apiVersion: apps/v1
    kind: Deployment
    metadata:
    name: client
    spec:
    replicas: 1
    selector:
        matchLabels:
        app: client
    template:
        metadata:
        labels:
            app: client
        spec:
        containers:
        - name: client
            image: zeli8888/clientservice
            env:
            - name: SERVER_URI
            valueFrom:
                configMapKeyRef:
                name: client-service-config
                key: server.uri
            stdin: true
            tty: true
    ```
  - deploy cluster using minikube
    ```bash
    minikube start && kubectl apply -f ../k8s/
    ```
## Baseline Test
- initial tests to confirm application functions correctly
    - Access ClientService pod terminal
        ```bash
        kubectl exec -it deployment/client -- java -jar app.jar
        ```
    - Within ClientService pod, send requests to ServerService trying different endpoints with failure, delay and chaos
        ```bash
        ClientService started. Type 'help' for available commands, 'exit' to stop.
        > hello
        Response: Hello from Server
        > hello-fail
        Use resilience mechanisms? (yes/no): no
        Should the call fail? (yes/no): yes
        Error calling server: 503 Service Unavailable: "{"timestamp":"2025-10-29T15:55:59.377+00:00","status":503,"error":"Service Unavailable","path":"/api/hello-fail"}"
        > hello-delay
        Use resilience mechanisms? (yes/no): no
        Enter delay in milliseconds: 4000
        Response: Hello from Server after delay of 4000 ms
        > hello-chaos
        Use resilience mechanisms? (yes/no): no
        Enter chaos percent (0-100): 80
        Error calling server: 503 Service Unavailable: "{"timestamp":"2025-10-29T15:57:05.561+00:00","status":503,"error":"Service Unavailable","path":"/api/hello-chaos"}"
        ```
    - Further check the Resilience mechanism is not used
        ```bash
        > status
        🔧 Resilience4j Detailed Status
        Circuit Breaker:
        State: CLOSED
        Failure Rate Threshold: 50.0%
        Sliding Window Size: 10
        Permitted Calls in Half-Open: 3
        Current Metrics:
            • Total Calls: 0
            • Successful: 0
            • Failed: 0
            • Current Failure Rate: -1.0%
        Retry:
        Max Attempts: 4
        Current Metrics:
            • Successful (no retry): 0
            • Successful (with retry): 0
            • Failed (after retry): 0
        Timeouts:
        Connection Timeout: 3 seconds
        Read Timeout: 3 seconds
        ```
- Observation for the impact when the ServerService fails or becomes slow without any resilience patterns in place (e.g., client blocking, timeouts, errors propagating directly)
    1. Direct Failure Propagation
        - Client receives raw errors immediately when server fails.
        - Client may No protection - continues sending requests to failing endpoints.
        - Wasted resources on doomed requests.
    2. Unmanaged Latency Impact
        - Client threads block indefinitely during server delays
        - No timeout mechanism to release stuck resources
        - Application freezes waiting for slow responses
    3. Chaos-Induced Instability
        - High failure probability directly impacts user experience
        - No automatic retry for transient failures
        - Manual retry attempts waste human effort


# Resilience Experiments
<!-- For each experiment (Circuit Breaker, Retries, Chaos Engineering):  Describe the specific configuration of the pattern/tool (e.g., circuit breaker thresholds, retry logic parameters, chaos
experiment YAML).  Document your observations vividly (e.g., client service logs, service behavior during failure, recovery process). Use
screenshots, log snippets, or charts as evidence.  Crucially, provide a detailed analysis of the architectural
trade-offs. Justify why you would choose these specific
resilience strategies for different failure types or business
requirements. Link your observations directly to core
distributed systems principles like the CAP Theorem, availability, performance, and fault tolerance -->
## Part B: Implementing Resilience Patterns
### Circuit Breaker Implementation
#### Configuration
- Integrate a Circuit Breaker pattern into ClientService for calls made to the ServerService
- Configure the circuit breaker with parameters such as failure threshold, a duration to wait before attempting to half-open, and a maximum number of concurrent requests allowed when half-open.
    - Configuration
        ```properties
        # Resilience4j Circuit Breaker Configuration
        ## only consider closed calls in the sliding window
        resilience4j.circuitbreaker.instances.serverService.slidingWindowSize=10
        ## failure threshold in percentage
        resilience4j.circuitbreaker.instances.serverService.failureRateThreshold=50
        ## duration to wait before attempting to half-open
        resilience4j.circuitbreaker.instances.serverService.waitDurationInOpenState=5000
        ## maximum number of concurrent requests allowed when half-open
        resilience4j.circuitbreaker.instances.serverService.permittedNumberOfCallsInHalfOpenState=3

        # Resilience4j Retry Configuration
        ## maximum number of retry attempts, including the initial call
        resilience4j.retry.instances.serverService.maxAttempts=4
        ## initial wait duration between retry attempts, 2s
        resilience4j.retry.instances.serverService.waitDuration=2000
        ## enable exponential backoff strategy, with a multiplier of 2, 2s -> 4s -> 6s
        resilience4j.retry.instances.serverService.enableExponentialBackoff=true
        resilience4j.retry.instances.serverService.exponentialBackoffMultiplier=2
        ## enable Jitter to avoid thundering herd problem
        resilience4j.retry.instances.serverService.enableRandomizedWait=true
        ## factor to calculate the random wait duration
        ## e.g., 0.25 means 25% of the wait duration on the basis of exponential backoff
        ## 2s(0.5s) ? 4s(1s) -> 6s(1.5s)
        resilience4j.retry.instances.serverService.randomizedWaitFactor=0.25

        # timeout for server calls
        timeout.connection.second=3
        timeout.read.second=3
        ```
    - Explanation
        - Circuit Breaker Configuration:
            - Sliding Window: 10 recent calls determine circuit state
            - Failure Threshold: 50% - trips to OPEN after 5 failures in 10 calls
            - Recovery Timing: 5-second wait before attempting HALF-OPEN state
            - Testing Phase: 3 test calls allowed in HALF-OPEN to verify recovery
        - Retry Configuration:
            - Max Attempts: 4 total calls (1 initial + 3 retries)
            - Backoff Strategy: Exponential with 2x multiplier (2s → 4s → 6s)
            - Jitter: ±25% random variation to prevent synchronized retries
            - Smart Delays: Progressive waiting with randomness for load distribution
        - Timeout Settings:
            - Connection Timeout: 3 seconds to establish connection
            - Read Timeout: 3 seconds to receive response
    - Protection Strategy:
        - Fast Failure: Circuit breaker prevents overwhelming failing services
        - Transient Recovery: Retry handles temporary network issues
        - Load Distribution: Jitter avoids retry storms and thundering herd



#### Experiment
- Trigger enough failures (from ServerService) to cause the circuit breaker to open. Observe the ClientService's behavior (e.g., fast failing, returning a fallback response, not even attempting the call).
    - Access ClientService pod terminal
        ```bash
        kubectl exec -it deployment/client -- java -jar app.jar
        ```
    - Within ClientService pod, send requests to ServerService with Resilience to trigger circuit breaker to open
        ```bash
        ClientService started. Type 'help' for available commands, 'exit' to stop.
        > hello-fail
        Use resilience mechanisms? (yes/no): yes
        Should the call fail? (yes/no): yes
        Error with resilience: ServiceUnavailable - 503
        > hello-fail
        Use resilience mechanisms? (yes/no): yes
        Should the call fail? (yes/no): yes
        Error with resilience: ServiceUnavailable - 503
        > hello-fail
        Use resilience mechanisms? (yes/no): yes
        Should the call fail? (yes/no): no
        Response: Hello from Server
        > hello-fail
        Use resilience mechanisms? (yes/no): yes
        Should the call fail? (yes/no): no
        Response: Hello from Server
        > status
        🔧 Resilience4j Detailed Status
        Circuit Breaker:
        State: OPEN
        Failure Rate Threshold: 50.0%
        Sliding Window Size: 10
        Permitted Calls in Half-Open: 3
        Current Metrics:
            • Total Calls: 10
            • Successful: 2
            • Failed: 8
            • Current Failure Rate: 80.0%
        Retry:
        Max Attempts: 4
        Current Metrics:
            • Successful (no retry): 2
            • Successful (with retry): 0
            • Failed (after retry): 2
        Timeouts:
        Connection Timeout: 3 seconds
        Read Timeout: 3 seconds
        ```
    - ServerService Logs
        ```bash
        Received request for /hello-fail with shouldFail: true, at 2025-10-29 19:43:52.914816
        Received request for /hello-fail with shouldFail: true, at 2025-10-29 19:43:55.048752
        Received request for /hello-fail with shouldFail: true, at 2025-10-29 19:43:59.769044
        Received request for /hello-fail with shouldFail: true, at 2025-10-29 19:44:08.573785
        Received request for /hello-fail with shouldFail: true, at 2025-10-29 19:44:14.003874
        Received request for /hello-fail with shouldFail: true, at 2025-10-29 19:44:15.711401
        Received request for /hello-fail with shouldFail: true, at 2025-10-29 19:44:19.205704
        Received request for /hello-fail with shouldFail: true, at 2025-10-29 19:44:25.266543
        Received request for /hello-fail with shouldFail: false, at 2025-10-29 19:44:32.753246
        Received request for /hello-fail with shouldFail: false, at 2025-10-29 19:44:46.696061
        ```
    
- After the configured wait duration, observe the circuit breaker attempting to half-open and allowing a limited number of requests through.
    - Within ClientService pod, send requests during half-open state of circuit breaker
        ```bash
        > hello-fail
        Use resilience mechanisms? (yes/no): yes
        Should the call fail? (yes/no): no
        Response: Hello from Server
        > status
        🔧 Resilience4j Detailed Status
        Circuit Breaker:
        State: HALF_OPEN
        Failure Rate Threshold: 50.0%
        Sliding Window Size: 10
        Permitted Calls in Half-Open: 3
        Current Metrics:
            • Total Calls: 1
            • Successful: 1
            • Failed: 0
            • Current Failure Rate: -1.0%
        Retry:
        Max Attempts: 4
        Current Metrics:
            • Successful (no retry): 3
            • Successful (with retry): 0
            • Failed (after retry): 2
        Timeouts:
        Connection Timeout: 3 seconds
        Read Timeout: 3 seconds
        ```
    - ServerService Logs
        ```bash
        Received request for /hello-fail with shouldFail: false, at 2025-10-29 19:44:53.225936
        ```
- Verify the circuit closes if successful calls resume, or re-opens if failures persist.
    - circuit closes if successful calls resume
        - ClientService pod request
            ```bash
            > hello-fail
            Use resilience mechanisms? (yes/no): yes
            Should the call fail? (yes/no): no
            Response: Hello from Server
            > hello-fail
            Use resilience mechanisms? (yes/no): yes
            Should the call fail? (yes/no): no
            Response: Hello from Server
            > status
            🔧 Resilience4j Detailed Status
            Circuit Breaker:
            State: CLOSED
            Failure Rate Threshold: 50.0%
            Sliding Window Size: 10
            Permitted Calls in Half-Open: 3
            Current Metrics:
                • Total Calls: 0
                • Successful: 0
                • Failed: 0
                • Current Failure Rate: -1.0%
            Retry:
            Max Attempts: 4
            Current Metrics:
                • Successful (no retry): 5
                • Successful (with retry): 0
                • Failed (after retry): 2
            Timeouts:
            Connection Timeout: 3 seconds
            Read Timeout: 3 seconds
            ```
        - ServerService Logs
            ```bash
            Received request for /hello-fail with shouldFail: false, at 2025-10-29 19:57:24.432811
            Received request for /hello-fail with shouldFail: false, at 2025-10-29 19:57:37.780112
            ```
    - circuit re-opens if failures persist 
        - redo experiment to set circuit breaker to half-open again first
            ```bash
            ClientService started. Type 'help' for available commands, 'exit' to stop.
            > hello-fail
            Use resilience mechanisms? (yes/no): yes
            Should the call fail? (yes/no): yes
            Error with resilience: ServiceUnavailable - 503
            > hello-fail
            Use resilience mechanisms? (yes/no): yes
            Should the call fail? (yes/no): yes
            Error with resilience: ServiceUnavailable - 503 
            > hello-fail
            Use resilience mechanisms? (yes/no): yes
            Should the call fail? (yes/no): no
            Response: Hello from Server
            > hello-fail
            Use resilience mechanisms? (yes/no): yes
            Should the call fail? (yes/no): yes
            Error with resilience: ServiceUnavailable - 503
            > status
            🔧 Resilience4j Detailed Status
            Circuit Breaker:
            State: HALF_OPEN
            Failure Rate Threshold: 50.0%
            Sliding Window Size: 10
            Permitted Calls in Half-Open: 3
            Current Metrics:
                • Total Calls: 2
                • Successful: 0
                • Failed: 2
                • Current Failure Rate: -1.0%
            Retry:
            Max Attempts: 4
            Current Metrics:
                • Successful (no retry): 1
                • Successful (with retry): 0
                • Failed (after retry): 3
            Timeouts:
            Connection Timeout: 3 seconds
            Read Timeout: 3 seconds
            ```
        - send request to trigger 50.0% failure threshold (2 failures in 3 calls) in half-open state
            ```bash
            > hello-fail
            Use resilience mechanisms? (yes/no): yes
            Should the call fail? (yes/no): no
            Response: Hello from Server
            > status
            🔧 Resilience4j Detailed Status
            Circuit Breaker:
            State: OPEN
            Failure Rate Threshold: 50.0%
            Sliding Window Size: 10
            Permitted Calls in Half-Open: 3
            Current Metrics:
                • Total Calls: 3
                • Successful: 1
                • Failed: 2
                • Current Failure Rate: 66.7%
            Retry:
            Max Attempts: 4
            Current Metrics:
                • Successful (no retry): 2
                • Successful (with retry): 0
                • Failed (after retry): 3
            Timeouts:
            Connection Timeout: 3 seconds
            Read Timeout: 3 seconds
            ```
        - ServerService Logs
            ```bash
            Received request for /hello-fail with shouldFail: true, at 2025-10-29 20:06:20.155728  
            Received request for /hello-fail with shouldFail: true, at 2025-10-29 20:06:22.045052  
            Received request for /hello-fail with shouldFail: true, at 2025-10-29 20:06:26.563399  
            Received request for /hello-fail with shouldFail: true, at 2025-10-29 20:06:34.985714  
            Received request for /hello-fail with shouldFail: true, at 2025-10-29 20:08:53.840595  
            Received request for /hello-fail with shouldFail: true, at 2025-10-29 20:08:56.195587  
            Received request for /hello-fail with shouldFail: true, at 2025-10-29 20:09:00.486345  
            Received request for /hello-fail with shouldFail: true, at 2025-10-29 20:09:08.226912  
            Received request for /hello-fail with shouldFail: false, at 2025-10-29 20:10:05.284646 
            Received request for /hello-fail with shouldFail: true, at 2025-10-29 20:10:11.303737  
            # Circuit Breaker went into half-open since then
            Received request for /hello-fail with shouldFail: true, at 2025-10-29 20:10:17.146795  
            Received request for /hello-fail with shouldFail: true, at 2025-10-29 20:10:23.240187  
            Received request for /hello-fail with shouldFail: false, at 2025-10-29 20:16:12.145349
            ```
- Document observations and analyze the trade-offs: How does the circuit breaker improve availability and protect the ClientService? What are the implications for data freshness or user experience?
    - Observations of Circuit Breaker Behavior
        - Circuit Opening Process
            - Failure Accumulation: After 8 failures in 10 calls (80% failure rate), circuit transitioned from CLOSED to OPEN state
            - Immediate Protection: Once OPEN, all requests would be fast-failed without reaching ServerService
        - Half-Open Recovery Attempt
            - Automatic Transition: After 5 seconds in OPEN state, circuit automatically moved to HALF_OPEN
            - Limited Testing: Only 3 requests permitted to test service recovery
            - Transition Decision: With 2 failures in 3 test calls (66.7% failure rate), circuit returned to OPEN state
        - Successful Recovery Scenario
            - Gradual Restoration: When 3 test calls succeeded in HALF_OPEN state (100% success rate), circuit transitioned to CLOSED
            - Metrics Reset: Circuit breaker metrics reset after successful recovery, starting fresh monitoring
            - Normal Operation: All requests flowed normally to ServerService once in CLOSED state
    - How Circuit Breaker Improves Availability & Protects ClientService
        - Resource Protection: Stops ClientService from wasting threads and resources on failing downstream calls
        - Fast Failure: Returns immediate failure responses instead of waiting for timeouts
        - Load Shedding: Reduces load on both ClientService and ServerService during outages
    - Implications for Data Freshness & User Experience
        - Data Freshness Trade-offs
            - Stale Data Risk: Users may see fallback data instead of real-time information during open state of circuit breaker even though the service has recovered
            - Eventual Consistency: System prioritizes overall system availability over immediate data consistency.
            - Recovery Catch-up: Once circuit closes, systems can synchronize missed updates
        - User Experience Impact
            - Graceful Degradation: Users receive immediate fallback responses instead of long waits or timeouts
            - Predictable Behavior: Consistent error messages help users understand system state
            - Quick Feedback: Fast failures prevent user frustration from hanging requests
    - Architectural Trade-offs Analysis
        - Availability vs. Consistency (CAP Theorem)
            - Availability Priority: Circuit breaker chooses overall system availability over consistency during partitions
            - Partial Service: Better to provide limited service than complete outage
            - Business Context: Suitable for read-heavy applications where stale data is acceptable
        - Performance vs. Completeness
            - Response Time: Fast failures maintain good response times for users
            - Functionality Loss: Some features may be temporarily unavailable
            - Progressive Enhancement: Core functionality remains while advanced features degrade
    - Resilience Strategy Justification by Failure Type
        - Partial Service Degradation
            - Strategy: Circuit breaker with 50% failure threshold
            - Rationale: Allows some traffic while protecting against cascading failures
            - Configuration: 10-call sliding window provides sufficient sample size
        - Complete Service Outage
            - Strategy: Fast failure with fallback responses
            - Rationale: Prevents resource exhaustion and maintains user experience
            - Configuration: 5-second wait balances recovery speed with stability
    - Distributed Systems Principles Applied
        - Fault Tolerance
            - Failure Acceptance: Acknowledges that distributed systems will have partial failures
            - Isolation Boundaries: Circuit breaker creates clear failure boundaries between services
            - Graceful Degradation: System continues operating with reduced functionality
        - Load Management
            - Backpressure: Circuit breaker implements backpressure by rejecting excess load when server is overwhelmed
            - Resource Conservation: Protects limited resources like threads and connections from failing downstream calls
        - Recovery Oriented Computing
            - Automated Recovery: System self-heals without manual intervention
            - Progressive Testing: Controlled testing verifies recovery before full restoration
            - Continuous Monitoring: Real-time metrics enable informed circuit state decisions
### Retries with Exponential Backoff and Jitter
#### Configuration
- Implement retry logic with an exponential backoff strategy and jitter within ClientService for calls to the ServerService.
- This is used for transient failures that might resolve themselves.
    - Configuration
        ```properties
        # Resilience4j Circuit Breaker Configuration
        ## only consider closed calls in the sliding window
        resilience4j.circuitbreaker.instances.serverService.slidingWindowSize=10
        ## failure threshold in percentage
        resilience4j.circuitbreaker.instances.serverService.failureRateThreshold=50
        ## duration to wait before attempting to half-open
        resilience4j.circuitbreaker.instances.serverService.waitDurationInOpenState=5000
        ## maximum number of concurrent requests allowed when half-open
        resilience4j.circuitbreaker.instances.serverService.permittedNumberOfCallsInHalfOpenState=3

        # Resilience4j Retry Configuration
        ## maximum number of retry attempts, including the initial call
        resilience4j.retry.instances.serverService.maxAttempts=4
        ## initial wait duration between retry attempts, 2s
        resilience4j.retry.instances.serverService.waitDuration=2000
        ## enable exponential backoff strategy, with a multiplier of 2, 2s -> 4s -> 6s
        resilience4j.retry.instances.serverService.enableExponentialBackoff=true
        resilience4j.retry.instances.serverService.exponentialBackoffMultiplier=2
        ## enable Jitter to avoid thundering herd problem
        resilience4j.retry.instances.serverService.enableRandomizedWait=true
        ## factor to calculate the random wait duration
        ## e.g., 0.25 means 25% of the wait duration on the basis of exponential backoff
        ## 2s(0.5s) ? 4s(1s) -> 6s(1.5s)
        resilience4j.retry.instances.serverService.randomizedWaitFactor=0.25

        # timeout for server calls
        timeout.connection.second=3
        timeout.read.second=3
        ```
    - Explanation
        - Circuit Breaker Configuration:
            - Sliding Window: 10 recent calls determine circuit state
            - Failure Threshold: 50% - trips to OPEN after 5 failures in 10 calls
            - Recovery Timing: 5-second wait before attempting HALF-OPEN state
            - Testing Phase: 3 test calls allowed in HALF-OPEN to verify recovery
        - Retry Configuration:
            - Max Attempts: 4 total calls (1 initial + 3 retries)
            - Backoff Strategy: Exponential with 2x multiplier (2s → 4s → 6s)
            - Jitter: ±25% random variation to prevent synchronized retries
            - Smart Delays: Progressive waiting with randomness for load distribution
        - Timeout Settings:
            - Connection Timeout: 3 seconds to establish connection
            - Read Timeout: 3 seconds to receive response
    - Protection Strategy:
        - Fast Failure: Circuit breaker prevents overwhelming failing services
        - Transient Recovery: Retry handles temporary network issues
        - Load Distribution: Jitter avoids retry storms and thundering herd
#### Experiment
- Configure the ServerService to return transient failures (e.g., HTTP 429 Too Many Requests, or intermittent 500s).
- Observe the ClientService automatically retrying the requests with increasing delays
- Demonstrate how jitter helps prevent a "thundering herd" problem of synchronized retries.
- Document observations and analyze the trade-offs: When is this pattern appropriate versus a circuit breaker? What are the potential impacts on backend load, latency, and system stability?

## Part C: Chaos Engineering Experiment
### Chaos Engineering Setup
- Choose a chaos engineering tool - Chaos Toolkit
- Define a chaos experiment targeting Kubernetes-deployed ServerService
- Recommended Experiment: Simulate a network partition that prevents communication between ClientService and ServerService, or a node failure/shutdown of the node running ServerService pod.
### Execute & Observe
- Execute the chaos experiment while ClientService is active
```bash
kubectl exec -it deployment/chaos-toolkit -- chaos run /tmp/experiments/experiment.json
```
- Observe the system's behavior in detail, especially how implemented resilience patterns react
- Collect metrics or logs from both services and the Kubernetes cluster to support observations (e.g., circuit breaker state, retry counts, error rates, pod status).
### Analysis & Justification
- Compare the observed behavior to what should expect without the resilience patterns (refer to baseline test).
- Provide a detailed architectural analysis of how the resilience patterns enabled system to continue functioning (or fail gracefully) despite the injected fault.
- Relate findings back to architectural characteristics like availability, fault tolerance, and responsiveness. Justify why these patterns are crucial for robust distributed system design, considering their costs (e.g., complexity, potential for increased latency in some cases).



# Conclusion
<!-- Summarize your key learnings about designing for resilience, any unexpected observations, and the overall impact of applying these
architectural patterns. -->