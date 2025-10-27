# Introduction
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
1. **Client Service**: A Spring Boot application that makes HTTP requests to the Server Service.
2. **Server Service**: A Spring Boot application that responds to requests from the Client Service, with simulated delays and failures to mimic real-world conditions.

# Setup & Configuration
<!-- Detail your application components, Docker images, and Kubernetes deployment (including YAML manifests). Provide a clear
diagram of your deployed system -->
# Resilience Experiments
<!-- For each experiment (Circuit Breaker, Retries, Chaos Engineering):  Describe the specific configuration of the pattern/tool (e.g., circuit breaker thresholds, retry logic parameters, chaos
experiment YAML).  Document your observations vividly (e.g., client service logs, service behavior during failure, recovery process). Use
screenshots, log snippets, or charts as evidence.  Crucially, provide a detailed analysis of the architectural
trade-offs. Justify why you would choose these specific
resilience strategies for different failure types or business
requirements. Link your observations directly to core
distributed systems principles like the CAP Theorem, availability, performance, and fault tolerance -->
## Part B: Implementing Resilience Patterns
### Configuration
### Observations
### Architectural Trade-offs Analysis

## Part C: Chaos Engineering Experiment

# Conclusion