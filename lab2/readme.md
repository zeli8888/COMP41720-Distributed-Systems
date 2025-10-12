## Introduction
### Source Code Repository Link
link to code repository containing all client application code used for this experiments: https://github.com/zeli8888/COMP41720-Distributed-Systems/tree/main/lab2
### Lab's purpose
1. Understand the challenges of storing and managing data across multiple nodes in a distributed environment.
2. Configure and compare different data replication strategies, including primary-backup (leader-follower), multi-primary (multi-leader), and leaderless architectures.
3. Experiment with and differentiate between various consistency models, such as strong consistency, eventual consistency, and (optionally) causal consistency.
4. Analyze the practical implications and architectural trade-offs of choosing specific replication and consistency settings, particularly concerning the CAP Theorem (Consistency, Availability, Partition Tolerance).
5. Apply architectural thinking by justifying why certain data management decisions are made, rather than just how they are implemented.
### Tools & Environment
- NoSQL Database: MongoDB (with Replica Sets)
- Client Application Language: Java
- Environment: Docker Compose



## Setup & Configuration
### Part A: Setup & Baseline
1. Database Cluster Setup:
    - Set up a distributed cluster for MongoDB with 3 nodes
    - Documentation of setup process (Docker Compose files, commands, configurations for replication)
2. Simple Data Model:
    - UserProfile (String user_id, String username, String email, Long last_login_time)
    - Initial Data Insertion



## Replication & Consistency Experiments
<!-- For each experiment in Part B and Part C: Describe the specific configuration (e.g., replication factor, write/read concern). Document your observations (e.g., latency, data visibility, behaviour during failures). Use screenshots or console output snippets as evidence. Crucially, provide a detailed analysis of the architectural trade-offs. Justify why you would choose this specific configuration for a given business requirement, linking back to the CAP theorem and the course's emphasis on "why" over "how". For example, when would strong consistency be paramount, and what are its costs? When would eventual consistency be a better fit? -->
### Part B: Replication Strategies
1. Replication Factor / Write Concern:
    - use replication factor (RF) at 3
    - Demonstrate how different write concerns/levels (w:1, w:majority, w:all in MongoDB) affect write latency and
durability across the cluster. Provide observations
2. Leader-Follower (Primary-Backup) Model:
    - demonstrate writes and reads against the primary and how data propagates to followers
    - Simulate a primary node failure and observe how the system elects a new primary and handles ongoing operations. Note any downtime or data inconsistencies
3. Mongodb doesn't support Leaderless (Multi-Primary) Model
### Part C: Consistency Models
1. Strong Consistency:
    - Configure both writes and reads to demand strong consistency (w:majority for writes and readConcern:majority for reads in MongoDB).
    - Perform a write operation on one node and immediately attempt to read it from another node. Verify that the data is immediately consistent.
    - Introduce a network partition or node failure during this experiment. Observe the impact on write/read operations (e.g., does it block, throw an error, become unavailable?). Relate this observation directly to the CAP theorem.
2. Eventual Consistency:
    - Configure writes and reads for eventual consistency (w:1 for writes and default read concern in MongoDB).
    - Perform a write operation on one node. Immediately attempt to read it from another node. Observe if you can read stale (old) data before it propagates.
    - Implement a simple loop that repeatedly reads the data until the latest value is observed, demonstrating the "eventual" nature.
    - Discuss scenarios where eventual consistency is acceptable or even desirable (e.g., social media likes, sensor data) and why this choice is beneficial in those contexts (e.g., availability, performance).
3. Causal Consistency (Optional / Bonus):
    - design an experiment to demonstrate that causally related operations are observed in order, even if other concurrent operations are not.



## Distributed Transactions
<!-- Detailed conceptual analysis of the e-commerce workflow, contrasting ACID with Saga patterns and their trade-offs. -->
### Part D: Distributed Transactions (Conceptual / Optional Coding)
1. Review the challenges of distributed transactions (e.g., Saga pattern)
2. Conceptual Exercise: for a simple multi-service workflow (e-commerce order involving OrderService, PaymentService, InventoryService)
    - Describe how this workflow would be managed using: 
        1. ACID transactions (and why it's problematic in a truly distributed system).
        2. Sagas (Orchestrated or Choreographed).
    - Analyze the trade-offs between these approaches in terms of consistency, complexity, fault tolerance, and performance. You do not need to implement this part; a detailed conceptual explanation is sufficient.



## Conclusion
<!-- Summary of key learnings and any unexpected observations. -->