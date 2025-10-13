# COMP41720 Distributed Systems Lab 2 Report
# Spring Boot Implementation with Spring Data MongoDB
## Introduction
### Source Code Repository Link
The complete Spring Boot application code for this lab is available at: https://github.com/zeli8888/COMP41720-Distributed-Systems/tree/main/lab2
### Lab's purpose
1. Understanding data storage challenges across multiple nodes.
2. Comparing different write concern levels and replication strategies (primary-backup vs leaderless)
3. Experimenting with consistency models (strong, eventual, causal).
4. Analyzing CAP Theorem trade-offs in specific replication and consistency settings.
5. Applying architectural thinking to justify design decisions.
### Tools & Environment
- NoSQL Database: MongoDB with Replica Sets
- Client Application: Spring Boot with Spring Data MongoDB
- Environment: Docker Compose



## Setup & Configuration
### Part A: Setup & Baseline
1. Database Cluster Setup:
    - Set up a distributed cluster for MongoDB with 3 nodes
    - Documentation of setup process (Docker Compose files, commands, configurations for replication)
Create localhost volume directory:
```bash
mkdir -p ./data_volume/mongodb1/db
mkdir -p ./data_volume/mongodb1/configdb
mkdir -p ./data_volume/mongodb2/db
mkdir -p ./data_volume/mongodb2/configdb
mkdir -p ./data_volume/mongodb3/db
mkdir -p ./data_volume/mongodb3/configdb
```

Create Docker Compose file:
```bash
touch docker-compose.yml
```
Docker Compose File:
```dockerfile
version: '3.8'

services:
  mongodb1:
    image: mongo:5.0
    container_name: mongodb1
    command: ["--replSet", "rs0", "--bind_ip_all", "--port", "27017"]
    volumes:
      - mongodb1_data:/data/db
      - mongodb1_config:/data/configdb
    network_mode: "host"

  mongodb2:
    image: mongo:5.0
    container_name: mongodb2
    command: ["--replSet", "rs0", "--bind_ip_all", "--port", "27018"]
    volumes:
      - mongodb2_data:/data/db
      - mongodb2_config:/data/configdb
    network_mode: "host"

  mongodb3:
    image: mongo:5.0
    container_name: mongodb3
    command: ["--replSet", "rs0", "--bind_ip_all", "--port", "27019"]
    volumes:
      - mongodb3_data:/data/db
      - mongodb3_config:/data/configdb
    network_mode: "host"

volumes:
  mongodb1_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data_volume/mongodb1/db
  mongodb1_config:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data_volume/mongodb1/configdb
  mongodb2_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data_volume/mongodb2/db
  mongodb2_config:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data_volume/mongodb2/configdb
  mongodb3_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data_volume/mongodb3/db
  mongodb3_config:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: ./data_volume/mongodb3/configdb
```

Start the cluster:
```bash
docker-compose up -d
```

Initialize the replica set and check status:
```js
docker exec -it mongodb1 mongosh --port 27017
rs.initiate(
  {
    _id: "rs0",
    members: [
        {_id: 0, host: 'localhost:27017'},
        {_id: 1, host: 'localhost:27018'},
        {_id: 2, host: 'localhost:27019'}
    ]
  }
)
rs.status()
```

Output:
```js
{
  set: 'rs0',
  date: ISODate('2025-10-13T17:39:12.328Z'),
  myState: 1,
  term: Long('1'),
  syncSourceHost: '',
  syncSourceId: -1,
  heartbeatIntervalMillis: Long('2000'),
  majorityVoteCount: 2,
  writeMajorityCount: 2,
  votingMembersCount: 3,
  writableVotingMembersCount: 3,
  optimes: {
    lastCommittedOpTime: { ts: Timestamp({ t: 1760377144, i: 1 }), t: Long('1') },        
    lastCommittedWallTime: ISODate('2025-10-13T17:39:04.420Z'),
    readConcernMajorityOpTime: { ts: Timestamp({ t: 1760377144, i: 1 }), t: Long('1') },  
    appliedOpTime: { ts: Timestamp({ t: 1760377144, i: 1 }), t: Long('1') },
    durableOpTime: { ts: Timestamp({ t: 1760377144, i: 1 }), t: Long('1') },
    lastAppliedWallTime: ISODate('2025-10-13T17:39:04.420Z'),
    lastDurableWallTime: ISODate('2025-10-13T17:39:04.420Z')
  },
  lastStableRecoveryTimestamp: Timestamp({ t: 1760377113, i: 1 }),
  electionCandidateMetrics: {
    lastElectionReason: 'electionTimeout',
    lastElectionDate: ISODate('2025-10-13T17:38:44.324Z'),
    electionTerm: Long('1'),
    lastCommittedOpTimeAtElection: { ts: Timestamp({ t: 1760377113, i: 1 }), t: Long('-1') },
    lastSeenOpTimeAtElection: { ts: Timestamp({ t: 1760377113, i: 1 }), t: Long('-1') },  
    numVotesNeeded: 2,
    priorityAtElection: 1,
    electionTimeoutMillis: Long('10000'),
    numCatchUpOps: Long('0'),
    newTermStartDate: ISODate('2025-10-13T17:38:44.397Z'),
    wMajorityWriteAvailabilityDate: ISODate('2025-10-13T17:38:45.059Z')
  },
  members: [
    {
      _id: 0,
      name: 'localhost:27017',
      health: 1,
      state: 1,
      stateStr: 'PRIMARY',
      uptime: 90,
      optime: { ts: Timestamp({ t: 1760377144, i: 1 }), t: Long('1') },
      optimeDate: ISODate('2025-10-13T17:39:04.000Z'),
      lastAppliedWallTime: ISODate('2025-10-13T17:39:04.420Z'),
      lastDurableWallTime: ISODate('2025-10-13T17:39:04.420Z'),
      syncSourceHost: '',
      syncSourceId: -1,
      infoMessage: 'Could not find member to sync from',
      electionTime: Timestamp({ t: 1760377124, i: 1 }),
      electionDate: ISODate('2025-10-13T17:38:44.000Z'),
      configVersion: 1,
      configTerm: 1,
      self: true,
      lastHeartbeatMessage: ''
    },
    {
      _id: 1,
      name: 'localhost:27018',
      health: 1,
      state: 2,
      stateStr: 'SECONDARY',
      uptime: 39,
      optime: { ts: Timestamp({ t: 1760377144, i: 1 }), t: Long('1') },
      optimeDurable: { ts: Timestamp({ t: 1760377144, i: 1 }), t: Long('1') },
      optimeDate: ISODate('2025-10-13T17:39:04.000Z'),
      optimeDurableDate: ISODate('2025-10-13T17:39:04.000Z'),
      lastAppliedWallTime: ISODate('2025-10-13T17:39:04.420Z'),
      lastDurableWallTime: ISODate('2025-10-13T17:39:04.420Z'),
      lastHeartbeat: ISODate('2025-10-13T17:39:10.364Z'),
      lastHeartbeatRecv: ISODate('2025-10-13T17:39:11.367Z'),
      pingMs: Long('0'),
      lastHeartbeatMessage: '',
      syncSourceHost: 'localhost:27017',
      syncSourceId: 0,
      infoMessage: '',
      configVersion: 1,
      configTerm: 1
    },
    {
      _id: 2,
      name: 'localhost:27019',
      health: 1,
      state: 2,
      stateStr: 'SECONDARY',
      uptime: 39,
      optime: { ts: Timestamp({ t: 1760377144, i: 1 }), t: Long('1') },
      optimeDurable: { ts: Timestamp({ t: 1760377144, i: 1 }), t: Long('1') },
      optimeDate: ISODate('2025-10-13T17:39:04.000Z'),
      optimeDurableDate: ISODate('2025-10-13T17:39:04.000Z'),
      lastAppliedWallTime: ISODate('2025-10-13T17:39:04.420Z'),
      lastDurableWallTime: ISODate('2025-10-13T17:39:04.420Z'),
      lastHeartbeat: ISODate('2025-10-13T17:39:10.364Z'),
      lastHeartbeatRecv: ISODate('2025-10-13T17:39:11.367Z'),
      pingMs: Long('0'),
      lastHeartbeatMessage: '',
      syncSourceHost: 'localhost:27017',
      syncSourceId: 0,
      infoMessage: '',
      configVersion: 1,
      configTerm: 1
    }
  ],
  ok: 1,
  '$clusterTime': {
    clusterTime: Timestamp({ t: 1760377144, i: 1 }),
    signature: {
      hash: Binary.createFromBase64('AAAAAAAAAAAAAAAAAAAAAAAAAAA=', 0),
      keyId: Long('0')
    }
  },
  operationTime: Timestamp({ t: 1760377144, i: 1 })
}
```

2. Simple Data Model:
    - UserProfile (String user_id, String username, String email, Long last_login_time)
    - Initial Data Insertion
Connect to primary node
```bash
docker exec -it mongodb1 mongosh
```
Insert initial data
```bash
use testdb;
db.UserProfile.insertOne({
  user_id: "user1",
  username: "john_doe",
  email: "john@example.com",
  last_login_time: new Date().getTime()
});
```

Output:
```bash
{
  acknowledged: true,
  insertedId: ObjectId('68ed160628fe19bcf6544ca7')
}
```


## Replication & Consistency Experiments
<!-- For each experiment in Part B and Part C: Describe the specific configuration (e.g., replication factor, write/read concern). Document your observations (e.g., latency, data visibility, behaviour during failures). Use screenshots or console output snippets as evidence. Crucially, provide a detailed analysis of the architectural trade-offs. Justify why you would choose this specific configuration for a given business requirement, linking back to the CAP theorem and the course's emphasis on "why" over "how". For example, when would strong consistency be paramount, and what are its costs? When would eventual consistency be a better fit? -->
### Part B: Replication Strategies
1. Replication Factor / Write Concern:
    - use replication factor (RF) at 3
    - Demonstrate how different write concerns/levels (w:1, w:majority, w:all in MongoDB) affect write latency and durability across the cluster. Provide observations

I already set replication factor to 3 during initial setup.


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