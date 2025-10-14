# COMP41720 Distributed Systems Lab 2 Report
## Spring Boot Implementation with Spring Data MongoDB
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

Docker Compose File:
```dockerfile
version: '3.8'

services:
  mongodb1:
    image: mongo:7.0.5
    container_name: mongodb1
    command: ["--replSet", "rs0", "--bind_ip_all", "--port", "27017"]
    volumes:
      - mongodb1_data:/data/db
      - mongodb1_config:/data/configdb
    ports:
      - "27017:27017"
    networks:
      - mongo-network

  mongodb2:
    image: mongo:7.0.5
    container_name: mongodb2
    command: ["--replSet", "rs0", "--bind_ip_all", "--port", "27018"]
    volumes:
      - mongodb2_data:/data/db
      - mongodb2_config:/data/configdb
    ports:
      - "27018:27018"
    networks:
      - mongo-network

  mongodb3:
    image: mongo:7.0.5
    container_name: mongodb3
    command: ["--replSet", "rs0", "--bind_ip_all", "--port", "27019"]
    volumes:
      - mongodb3_data:/data/db
      - mongodb3_config:/data/configdb
    ports:
      - "27019:27019"
    networks:
      - mongo-network

  spring-app:
    image: maven:3.8.5-openjdk-17
    working_dir: /app
    volumes:
      - ./lab2:/app
    ports:
      - "8080:8080"
    command: /bin/bash
    stdin_open: true
    tty: true
    networks:
      - mongo-network

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

networks:
  mongo-network:
    driver: bridge
```

Start the cluster:
```bash
docker-compose up -d
```

Initialize the replica set and check status:
```js
docker exec -it mongodb1 mongosh
rs.initiate(
  {
    _id: "rs0",
    members: [
        {_id: 0, host: 'mongodb1:27017'},
        {_id: 1, host: 'mongodb2:27018'},
        {_id: 2, host: 'mongodb3:27019'}
    ]
  }
)
rs.status()
```

Part of Output:
```js
members: [
  {
    _id: 0,
    name: 'mongodb1:27017',
    health: 1,
    state: 1,
    stateStr: 'PRIMARY',
    uptime: 19,
    optime: { ts: Timestamp({ t: 1760396535, i: 5 }), t: Long('3') },    
    optimeDate: ISODate('2025-10-13T23:02:15.000Z'),
    lastAppliedWallTime: ISODate('2025-10-13T23:02:15.440Z'),
    lastDurableWallTime: ISODate('2025-10-13T23:02:15.440Z'),
    syncSourceHost: '',
    syncSourceId: -1,
    infoMessage: 'Could not find member to sync from',
    electionTime: Timestamp({ t: 1760396535, i: 1 }),
    electionDate: ISODate('2025-10-13T23:02:15.000Z'),
    configVersion: 1,
    configTerm: 3,
    self: true,
    lastHeartbeatMessage: ''
  },
  {
    _id: 1,
    name: 'mongodb2:27018',
    health: 1,
    state: 2,
    stateStr: 'SECONDARY',
    uptime: 12,
    optime: { ts: Timestamp({ t: 1760396535, i: 5 }), t: Long('3') },    
    optimeDurable: { ts: Timestamp({ t: 1760396535, i: 5 }), t: Long('3') },
    optimeDate: ISODate('2025-10-13T23:02:15.000Z'),
    optimeDurableDate: ISODate('2025-10-13T23:02:15.000Z'),
    lastAppliedWallTime: ISODate('2025-10-13T23:02:15.440Z'),
    lastDurableWallTime: ISODate('2025-10-13T23:02:15.440Z'),
    lastHeartbeat: ISODate('2025-10-13T23:02:17.428Z'),
    lastHeartbeatRecv: ISODate('2025-10-13T23:02:17.428Z'),
    pingMs: Long('0'),
    lastHeartbeatMessage: '',
    syncSourceHost: 'mongodb1:27017',
    syncSourceId: 0,
    infoMessage: '',
    configVersion: 1,
    configTerm: 3
  },
  {
    _id: 2,
    name: 'mongodb3:27019',
    health: 1,
    state: 2,
    stateStr: 'SECONDARY',
    uptime: 11,
    optime: { ts: Timestamp({ t: 1760396535, i: 5 }), t: Long('3') },
    optimeDurable: { ts: Timestamp({ t: 1760396535, i: 5 }), t: Long('3') },
    optimeDate: ISODate('2025-10-13T23:02:15.000Z'),
    optimeDurableDate: ISODate('2025-10-13T23:02:15.000Z'),
    lastAppliedWallTime: ISODate('2025-10-13T23:02:15.440Z'),
    lastDurableWallTime: ISODate('2025-10-13T23:02:15.440Z'),
    lastHeartbeat: ISODate('2025-10-13T23:02:17.423Z'),
    lastHeartbeatRecv: ISODate('2025-10-13T23:02:15.922Z'),
    pingMs: Long('0'),
    lastHeartbeatMessage: '',
    syncSourceHost: 'mongodb1:27017',
    syncSourceId: 0,
    infoMessage: '',
    configVersion: 1,
    configTerm: 3
  }
]
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

Replication factor 3 is already set during initial setup.
Experiment Results for different write concerns:
```bash
🧪 Testing Write Concern: w:all (averaging 500 runs, 400 docs per run)
📊 w:all Results:
  Average Latency: 10 ms
  Min Latency: 6 ms
  Max Latency: 111 ms
  Std Deviation: 5.88 ms
  Success Count: 500/500
  Total Documents: 200000

🧪 Testing Write Concern: w:1 (averaging 500 runs, 400 docs per run)
📊 w:1 Results:
  Average Latency: 7 ms
  Min Latency: 5 ms
  Max Latency: 39 ms
  Std Deviation: 2.44 ms
  Success Count: 500/500
  Total Documents: 200000

🧪 Testing Write Concern: w:majority (averaging 500 runs, 400 docs per run)
📊 w:majority Results:
  Average Latency: 9 ms
  Min Latency: 6 ms
  Max Latency: 93 ms
  Std Deviation: 4.41 ms
  Success Count: 500/500
  Total Documents: 200000
```

2. Leader-Follower (Primary-Backup) Model:
    - demonstrate writes and reads against the primary and how data propagates to followers
    - Simulate a primary node failure and observe how the system elects a new primary and handles ongoing operations. Note any downtime or data inconsistencies

Experiment Results for writes against primary and propagates to followers
```bash
🧪 TEST 1: WRITE PROPAGATION TO FOLLOWERS
📝 Writing 50 documents to primary...
✅ Primary write completed in 427ms
🔍 Verifying data propagation to secondaries...
📊 Data Propagation: 50/50 documents
📈 Success rate: 100.00%
```

Experiment Results for reads against primary and followers
```bash
🧪 TEST 2: READ PREFERENCE BEHAVIOR
  Testing: Primary
    ✅ Document found - Latency: 0ms
  Testing: Secondary
    ✅ Document found - Latency: 0ms
  Testing: Primary Preferred
    ✅ Document found - Latency: 0ms
```

Experiment Results for primary node failure, new primary election and downtime data inconsistencies
```bash
🧪 TEST 3: PRIMARY FAILOVER WITH stepDown COMMAND
  Current Primary: mongodb1:27017
  Starting concurrent operations during failover...
  🔌 Triggering stepDown on primary...
    ✅ StepDown command sent
  📊 Monitoring failover process...
2025-10-13T23:34:57.608Z  INFO 356 --- [lab2] [-mongodb1:27017] org.mongodb.driver.cluster               : Monitor thread successfully connected to server with description ServerDescription{address=mongodb1:27017, type=REPLICA_S
ET_OTHER, cryptd=false, state=CONNECTED, ok=true, minWireVersion=0, maxWireVersion=21, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=44824577, minRoundTripTimeNanos=1016514, setName='rs0', canonic
alAddress=mongodb1:27017, hosts=[mongodb2:27018, mongodb1:27017, mongodb3:27019], passives=[], arbiters=[], primary='mongodb1:27017', tagSet=TagSet{[]}, electionId=7fffffff0000000000000003, setVersion=1, topologyVersion=TopologyVersion{processId=68ed84e6d29ce3200db99020, counter=7}, lastWriteDate=Mon Oct 13 23:34:57 UTC 2025, lastUpdateTimeNanos=18478316614068}
2025-10-13T23:34:57.608Z  INFO 356 --- [lab2] [-mongodb1:27017] org.mongodb.driver.cluster               : Monitor thread successfully connected to server with description ServerDescription{address=mongodb1:27017, type=REPLICA_S
ET_SECONDARY, cryptd=false, state=CONNECTED, ok=true, minWireVersion=0, maxWireVersion=21, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=44824577, minRoundTripTimeNanos=1016514, setName='rs0', can
onicalAddress=mongodb1:27017, hosts=[mongodb2:27018, mongodb1:27017, mongodb3:27019], passives=[], arbiters=[], primary='null', tagSet=TagSet{[]}, electionId=null, setVersion=1, topologyVersion=TopologyVersion{processId=68ed84e6d29ce3200db99020, counter=8}, lastWriteDate=Mon Oct 13 23:34:57 UTC 2025, lastUpdateTimeNanos=18478317135965}
2025-10-13T23:34:57.623Z  INFO 356 --- [lab2] [onPool-worker-1] org.mongodb.driver.cluster               : Waiting for server to become available for operation with ID 120. Remaining time: 29993 ms. Selector: WritableServerSelec
tor, topology description: {type=REPLICA_SET, servers=[{address=mongodb1:27017, type=REPLICA_SET_SECONDARY, roundTripTime=44.8 ms, state=CONNECTED}, {address=mongodb2:27018, type=REPLICA_SET_SECONDARY, roundTripTime=55.8 ms, state=CONNECTED}, {address=mongodb3:27019, type=REPLICA_SET_SECONDARY, roundTripTime=55.8 ms, state=CONNECTED}].
2025-10-13T23:34:58.607Z  INFO 356 --- [lab2] [           main] org.mongodb.driver.cluster               : Waiting for server to become available for operation with ID 124. Remaining time: 29999 ms. Selector: ReadPreferenceServe
rSelector{readPreference=primary}, topology description: {type=REPLICA_SET, servers=[{address=mongodb1:27017, type=REPLICA_SET_SECONDARY, roundTripTime=44.8 ms, state=CONNECTED}, {address=mongodb2:27018, type=REPLICA_SET_SECONDARY, roundTripTime=55.8 ms, state=CONNECTED}, {address=mongodb3:27019, type=REPLICA_SET_SECONDARY, roundTripTime=55.8 ms, state=CONNECTED}].
2025-10-13T23:35:06.411Z  INFO 356 --- [lab2] [-mongodb3:27019] org.mongodb.driver.cluster               : Monitor thread successfully connected to server with description ServerDescription{address=mongodb3:27019, type=REPLICA_S
ET_SECONDARY, cryptd=false, state=CONNECTED, ok=true, minWireVersion=0, maxWireVersion=21, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=35976155, minRoundTripTimeNanos=702078, setName='rs0', cano
nicalAddress=mongodb3:27019, hosts=[mongodb2:27018, mongodb1:27017, mongodb3:27019], passives=[], arbiters=[], primary='null', tagSet=TagSet{[]}, electionId=null, setVersion=1, topologyVersion=TopologyVersion{processId=68ed84e670714eb00eb3c089, counter=4}, lastWriteDate=Mon Oct 13 23:34:57 UTC 2025, lastUpdateTimeNanos=18487120497754}
2025-10-13T23:35:06.412Z  INFO 356 --- [lab2] [-mongodb2:27018] org.mongodb.driver.cluster               : Monitor thread successfully connected to server with description ServerDescription{address=mongodb2:27018, type=REPLICA_S
ET_SECONDARY, cryptd=false, state=CONNECTED, ok=true, minWireVersion=0, maxWireVersion=21, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=36042697, minRoundTripTimeNanos=707011, setName='rs0', cano
nicalAddress=mongodb2:27018, hosts=[mongodb2:27018, mongodb1:27017, mongodb3:27019], passives=[], arbiters=[], primary='null', tagSet=TagSet{[]}, electionId=null, setVersion=1, topologyVersion=TopologyVersion{processId=68ed84e6f4c321496b97dc1e, counter=4}, lastWriteDate=Mon Oct 13 23:34:57 UTC 2025, lastUpdateTimeNanos=18487121339836}
2025-10-13T23:35:07.898Z  INFO 356 --- [lab2] [-mongodb3:27019] org.mongodb.driver.cluster               : Monitor thread successfully connected to server with description ServerDescription{address=mongodb3:27019, type=REPLICA_S
ET_SECONDARY, cryptd=false, state=CONNECTED, ok=true, minWireVersion=0, maxWireVersion=21, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=35976155, minRoundTripTimeNanos=702078, setName='rs0', cano
nicalAddress=mongodb3:27019, hosts=[mongodb2:27018, mongodb1:27017, mongodb3:27019], passives=[], arbiters=[], primary='mongodb3:27019', tagSet=TagSet{[]}, electionId=7fffffff0000000000000004, setVersion=1, topologyVersion=TopologyVersion{processId=68ed84e670714eb00eb3c089, counter=5}, lastWriteDate=Mon Oct 13 23:34:57 UTC 2025, lastUpdateTimeNanos=18488606844809}
2025-10-13T23:35:07.905Z  INFO 356 --- [lab2] [-mongodb3:27019] org.mongodb.driver.cluster               : Monitor thread successfully connected to server with description ServerDescription{address=mongodb3:27019, type=REPLICA_S
ET_PRIMARY, cryptd=false, state=CONNECTED, ok=true, minWireVersion=0, maxWireVersion=21, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=35976155, minRoundTripTimeNanos=702078, setName='rs0', canoni
calAddress=mongodb3:27019, hosts=[mongodb2:27018, mongodb1:27017, mongodb3:27019], passives=[], arbiters=[], primary='mongodb3:27019', tagSet=TagSet{[]}, electionId=7fffffff0000000000000004, setVersion=1, topologyVersion=TopologyVersion{processId=68ed84e670714eb00eb3c089, counter=7}, lastWriteDate=Mon Oct 13 23:35:07 UTC 2025, lastUpdateTimeNanos=18488613733567}
2025-10-13T23:35:07.905Z  INFO 356 --- [lab2] [-mongodb3:27019] org.mongodb.driver.cluster               : Discovered replica set primary mongodb3:27019 with max election id 7fffffff0000000000000004 and max set version 1        
    ⚡ New primary elected after 10300ms
    📊 Concurrent operations: 20 successful, 0 failed
  New Primary: mongodb3:27019
✅ Failover successful
```
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