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
## Simple Distributed Application
- two services: a ClientService and a ServerService
- ServerService has a basic endpoint that occasionally introduces simulated delays or failures to allow for testing resilience patterns
- ClientService make synchronous HTTP REST calls to ServerService
## Kubernetes Deployment
- Containerize both ClientService and ServerService using Docker
```bash
cd ClientService && mvn clean package && docker build -t clientservice .
cd ../ServerService && mvn clean package && docker build -t serverservice .
# Tag and push images to docker hub for Kubernetes to pull
docker tag clientservice zeli8888/clientservice
docker tag serverservice zeli8888/serverservice
docker push zeli8888/clientservice && docker push zeli8888/serverservice
```
- Deploy application on a Kubernetes cluster, Kubernetes manifests:
  - server.yaml
  - client.yaml
```bash
minikube start && kubectl apply -f k8s/
```
## Baseline Test
- initial tests to confirm application functions correctly
```bash
# Access ClientService pod terminal
kubectl exec -it $(kubectl get pod -l app=client -o jsonpath='{.items[0].metadata.name}') -- java -jar app.jar
# Within ClientService pod, send requests to ServerService
```
- Observation for the impact when the ServerService fails or becomes slow without any resilience patterns in place (e.g., client blocking, timeouts, errors propagating directly)



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
#### Experiment
- Trigger enough failures (from ServerService) to cause the circuit breaker to open. Observe the ClientService's behavior (e.g., fast failing, returning a fallback response, not even attempting the call).
- After the configured wait duration, observe the circuit breaker attempting to half-open and allowing a limited number of requests through.
- Verify the circuit closes if successful calls resume, or re-opens if failures persist.
- Document observations and analyze the trade-offs: How does the circuit breaker improve availability and protect the ClientService? What are the implications for data freshness or user experience?

### Retries with Exponential Backoff and Jitter
#### Configuration
- Implement retry logic with an exponential backoff strategy and jitter within your ClientService for calls to the ServerService.
- This is used for transient failures that might resolve themselves.
#### Experiment
- Configure the ServerService to return transient failures (e.g., HTTP 429 Too Many Requests, or intermittent 500s).
- Observe the ClientService automatically retrying the requests with increasing delays
- Demonstrate how jitter helps prevent a "thundering herd" problem of synchronized retries.
- Document observations and analyze the trade-offs: When is this pattern appropriate versus a circuit breaker? What are the potential impacts on backend load, latency, and system stability?

## Part C: Chaos Engineering Experiment
### Chaos Engineering Setup
- Choose a chaos engineering tool - Chaos Toolkit
- Define a chaos experiment targeting Kubernetes-deployed ServerService
- Recommended Experiment: Simulate a network partition that prevents communication between your ClientService and ServerService, or a node failure/shutdown of the node running your ServerService pod.
### Execute & Observe
- Execute the chaos experiment while your ClientService is active
- Observe the system's behavior in detail, especially how your implemented resilience patterns react
- Collect metrics or logs from both services and the Kubernetes cluster to support your observations (e.g., circuit breaker state, retry counts, error rates, pod status).
### Analysis & Justification
- Compare the observed behavior to what you would expect without the resilience patterns (refer to your baseline test).
- Provide a detailed architectural analysis of how the resilience patterns enabled your system to continue functioning (or fail gracefully) despite the injected fault.
- Relate your findings back to architectural characteristics like availability, fault tolerance, and responsiveness. Justify why these patterns are crucial for robust distributed system design, considering their costs (e.g., complexity, potential for increased latency in some cases).



# Conclusion
<!-- Summarize your key learnings about designing for resilience, any unexpected observations, and the overall impact of applying these
architectural patterns. -->