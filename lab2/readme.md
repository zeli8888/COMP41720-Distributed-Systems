# COMP41720 Distributed Systems Lab 2 Report
## Spring Boot Implementation with Spring Data MongoDB
## Introduction
### Source Code Repository Link
The complete code for this lab is available at: https://github.com/zeli8888/COMP41720-Distributed-Systems/tree/main/lab2
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

        1. Create localhost volume directory:
            ```bash
            mkdir -p ./data_volume/mongodb1/db
            mkdir -p ./data_volume/mongodb1/configdb
            mkdir -p ./data_volume/mongodb2/db
            mkdir -p ./data_volume/mongodb2/configdb
            mkdir -p ./data_volume/mongodb3/db
            mkdir -p ./data_volume/mongodb3/configdb
            ```
        2. Create Docker Compose file to define 3 MongoDB nodes and a Spring Boot application container.

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
                container_name: spring-app
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


        3. Start the cluster:

            ```bash
            docker compose up -d
            ```

        4.  Connect to one MongoDB instance and initiate the replica set:

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
            parts of output:
            ```js
              members: [
                {
                  _id: 0,
                  name: 'mongodb1:27017',
                  health: 1,
                  state: 1,
                  stateStr: 'PRIMARY',
                  uptime: 45,
                  optime: { ts: Timestamp({ t: 1760443988, i: 15 }), t: Long('1') }, 
                  optimeDate: ISODate('2025-10-14T12:13:08.000Z'),
                  lastAppliedWallTime: ISODate('2025-10-14T12:13:08.654Z'),
                  lastDurableWallTime: ISODate('2025-10-14T12:13:08.654Z'),
                  syncSourceHost: '',
                  syncSourceId: -1,
                  infoMessage: 'Could not find member to sync from',
                  electionTime: Timestamp({ t: 1760443988, i: 1 }),
                  electionDate: ISODate('2025-10-14T12:13:08.000Z'),
                  configVersion: 1,
                  configTerm: 1,
                  self: true,
                  lastHeartbeatMessage: ''
                },
                {
                  _id: 1,
                  name: 'mongodb2:27018',
                  health: 1,
                  state: 2,
                  stateStr: 'SECONDARY',
                  uptime: 11,
                  optime: { ts: Timestamp({ t: 1760443977, i: 1 }), t: Long('-1') }, 
                  optimeDurable: { ts: Timestamp({ t: 1760443977, i: 1 }), t: Long('-1') },
                  optimeDate: ISODate('2025-10-14T12:12:57.000Z'),
                  optimeDurableDate: ISODate('2025-10-14T12:12:57.000Z'),
                  lastAppliedWallTime: ISODate('2025-10-14T12:12:57.916Z'),
                  lastDurableWallTime: ISODate('2025-10-14T12:12:57.916Z'),
                  lastHeartbeat: ISODate('2025-10-14T12:13:08.468Z'),
                  lastHeartbeatRecv: ISODate('2025-10-14T12:13:08.967Z'),
                  pingMs: Long('0'),
                  lastHeartbeatMessage: '',
                  syncSourceHost: '',
                  syncSourceId: -1,
                  infoMessage: '',
                  configVersion: 1,
                  configTerm: 1
                },
                {
                  _id: 2,
                  name: 'mongodb3:27019',
                  health: 1,
                  state: 2,
                  stateStr: 'SECONDARY',
                  uptime: 11,
                  optime: { ts: Timestamp({ t: 1760443977, i: 1 }), t: Long('-1') }, 
                  optimeDurable: { ts: Timestamp({ t: 1760443977, i: 1 }), t: Long('-1') },
                  optimeDate: ISODate('2025-10-14T12:12:57.000Z'),
                  optimeDurableDate: ISODate('2025-10-14T12:12:57.000Z'),
                  lastAppliedWallTime: ISODate('2025-10-14T12:12:57.916Z'),
                  lastDurableWallTime: ISODate('2025-10-14T12:12:57.916Z'),
                  lastHeartbeat: ISODate('2025-10-14T12:13:08.467Z'),
                  lastHeartbeatRecv: ISODate('2025-10-14T12:13:08.967Z'),
                  pingMs: Long('0'),
                  lastHeartbeatMessage: '',
                  syncSourceHost: '',
                  syncSourceId: -1,
                  infoMessage: '',
                  configVersion: 1,
                  configTerm: 1
                }
              ]
            ```
2. Simple Data Model:
    - UserProfile (String user_id, String username, String email, Long last_login_time)
    - Initial Data Insertion

      1. Connect to primary node
          ```bash
          docker exec -it mongodb1 mongosh
          ```

      2. Insert initial data
          ```bash
          use testdb;
          db.UserProfile.insertOne({
            user_id: "user1",
            username: "john_doe",
            email: "john@example.com",
            last_login_time: new Date().getTime()
          });
          ```

          output:
          ```bash
          {
            acknowledged: true,
            insertedId: ObjectId('68ee3e79a1ab7ca189331f75')
          }
          ```


## Replication & Consistency Experiments
<!-- For each experiment in Part B and Part C: Describe the specific configuration (e.g., replication factor, write/read concern). Document your observations (e.g., latency, data visibility, behaviour during failures). Use screenshots or console output snippets as evidence. Crucially, provide a detailed analysis of the architectural trade-offs. Justify why you would choose this specific configuration for a given business requirement, linking back to the CAP theorem and the course's emphasis on "why" over "how". For example, when would strong consistency be paramount, and what are its costs? When would eventual consistency be a better fit? -->
### Part B: Replication Strategies
1. Replication Factor / Write Concern:
    - use replication factor (RF) at 3
    - Demonstrate how different write concerns/levels (w:1, w:majority, w:all in MongoDB) affect write latency and durability across the cluster. Provide observations

      1. Replication factor 3 is already set during initial cluster setup.
      2. Run the Spring Boot application with write-concerns test:
          ```bash
          docker compose exec spring-app /bin/bash
          mvn spring-boot:run -Dspring-boot.run.arguments="--service=write-concern"
          ```

      3. Experiment Results for different write concerns:
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

      1. Run the Spring Boot application with replication test:
          ```bash
          docker compose exec spring-app /bin/bash
          mvn spring-boot:run -Dspring-boot.run.arguments="--service=replication"
          ```

      2. Experiment Results for writes against primary and propagates to followers
          ```bash
          🧪 TEST 1: WRITE PROPAGATION TO FOLLOWERS
          📝 Writing 50 documents to primary...
          ✅ Primary write completed in 427ms
          🔍 Verifying data propagation to secondaries...
          📊 Data Propagation: 50/50 documents
          📈 Success rate: 100.00%
          ```

      3. Experiment Results for reads against primary and followers
          ```bash
          🧪 TEST 2: READ PREFERENCE BEHAVIOR
            Testing: Primary
              ✅ Document found - Latency: 0ms
            Testing: Secondary
              ✅ Document found - Latency: 0ms
            Testing: Primary Preferred
              ✅ Document found - Latency: 0ms
          ```

      4. Experiment Results for primary node failure, new primary election and downtime data inconsistencies
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
3. Mongodb doesn't support Leaderless (Multi-Primary) Model, so this part is skipped.
### Part C: Consistency Models
1. Strong Consistency:
    - Configure both writes and reads to demand strong consistency (w:majority for writes and readConcern:majority for reads in MongoDB).
    - Perform a write operation on one node and immediately attempt to read it from another node. Verify that the data is immediately consistent.
    - Introduce a network partition or node failure during this experiment. Observe the impact on write/read operations (e.g., does it block, throw an error, become unavailable?). Relate this observation directly to the CAP theorem.

      1. Run the Spring Boot application with consistency test:
          ```bash
          docker compose exec spring-app /bin/bash
          mvn spring-boot:run -Dspring-boot.run.arguments="--service=consistency"
          ```

      2. Experiment Results for Strong Consistency with immediate read from secondary after write in primary
          ```bash
          🧪 STRONG CONSISTENCY TEST
          ✅ Write completed with w:majority
          ✅ Strong consistency verified - immediate read from secondary
          📊 Document content: username_strong_write
          ```

      3. Experiment Results for Strong Consistency with network partition during write/read
          ```bash
            Testing during network partition...
            🔌 StepDown command sent to simulate network partition
          2025-10-14T01:18:07.999Z  INFO 1655 --- [lab2] [-mongodb3:27019] org.mongodb.driver.cluster               : Monitor thread successfully connected to server with description ServerDescription{address=mongodb3:27019, type=REPLICA_
          SET_OTHER, cryptd=false, state=CONNECTED, ok=true, minWireVersion=0, maxWireVersion=21, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=42095790, minRoundTripTimeNanos=807238, setName='rs0', canonic
          alAddress=mongodb3:27019, hosts=[mongodb2:27018, mongodb1:27017, mongodb3:27019], passives=[], arbiters=[], primary='mongodb3:27019', tagSet=TagSet{[]}, electionId=7fffffff000000000000000d, setVersion=1, topologyVersion=TopologyVersion{processId=68ed84e670714eb00eb3c089, counter=25}, lastWriteDate=Tue Oct 14 01:18:07 UTC 2025, lastUpdateTimeNanos=24669120445431}
          2025-10-14T01:18:08.000Z  INFO 1655 --- [lab2] [-mongodb3:27019] org.mongodb.driver.cluster               : Monitor thread successfully connected to server with description ServerDescription{address=mongodb3:27019, type=REPLICA_
          SET_SECONDARY, cryptd=false, state=CONNECTED, ok=true, minWireVersion=0, maxWireVersion=21, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=42095790, minRoundTripTimeNanos=807238, setName='rs0', can
          onicalAddress=mongodb3:27019, hosts=[mongodb2:27018, mongodb1:27017, mongodb3:27019], passives=[], arbiters=[], primary='null', tagSet=TagSet{[]}, electionId=null, setVersion=1, topologyVersion=TopologyVersion{processId=68ed84e670714eb00eb3c089, counter=26}, lastWriteDate=Tue Oct 14 01:18:07 UTC 2025, lastUpdateTimeNanos=24669120986302}
          2025-10-14T01:18:08.018Z  INFO 1655 --- [lab2] [onPool-worker-2] org.mongodb.driver.cluster               : Waiting for server to become available for operation with ID 18. Remaining time: 29992 ms. Selector: ReadPreferenceServe
          rSelector{readPreference=primary}, topology description: {type=REPLICA_SET, servers=[{address=mongodb1:27017, type=REPLICA_SET_SECONDARY, roundTripTime=52.4 ms, state=CONNECTED}, {address=mongodb2:27018, type=REPLICA_SET_SECONDARY, roundTripTime=52.4 ms, state=CONNECTED}, {address=mongodb3:27019, type=REPLICA_SET_SECONDARY, roundTripTime=42.1 ms, state=CONNECTED}].
          2025-10-14T01:18:08.018Z  INFO 1655 --- [lab2] [onPool-worker-1] org.mongodb.driver.cluster               : Waiting for server to become available for operation with ID 17. Remaining time: 29991 ms. Selector: WritableServerSelec
          tor, topology description: {type=REPLICA_SET, servers=[{address=mongodb1:27017, type=REPLICA_SET_SECONDARY, roundTripTime=52.4 ms, state=CONNECTED}, {address=mongodb2:27018, type=REPLICA_SET_SECONDARY, roundTripTime=52.4 ms, state=CONNECTED}, {address=mongodb3:27019, type=REPLICA_SET_SECONDARY, roundTripTime=42.1 ms, state=CONNECTED}].
          2025-10-14T01:18:17.101Z  INFO 1655 --- [lab2] [-mongodb1:27017] org.mongodb.driver.cluster               : Monitor thread successfully connected to server with description ServerDescription{address=mongodb1:27017, type=REPLICA_
          SET_SECONDARY, cryptd=false, state=CONNECTED, ok=true, minWireVersion=0, maxWireVersion=21, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=33822214, minRoundTripTimeNanos=506768, setName='rs0', can
          onicalAddress=mongodb1:27017, hosts=[mongodb2:27018, mongodb1:27017, mongodb3:27019], passives=[], arbiters=[], primary='null', tagSet=TagSet{[]}, electionId=null, setVersion=1, topologyVersion=TopologyVersion{processId=68ed84e6d29ce3200db99020, counter=34}, lastWriteDate=Tue Oct 14 01:18:07 UTC 2025, lastUpdateTimeNanos=24678222455004}
          2025-10-14T01:18:17.110Z  INFO 1655 --- [lab2] [-mongodb2:27018] org.mongodb.driver.cluster               : Monitor thread successfully connected to server with description ServerDescription{address=mongodb2:27018, type=REPLICA_
          SET_SECONDARY, cryptd=false, state=CONNECTED, ok=true, minWireVersion=0, maxWireVersion=21, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=33778090, minRoundTripTimeNanos=581942, setName='rs0', can
          onicalAddress=mongodb2:27018, hosts=[mongodb2:27018, mongodb1:27017, mongodb3:27019], passives=[], arbiters=[], primary='null', tagSet=TagSet{[]}, electionId=null, setVersion=1, topologyVersion=TopologyVersion{processId=68ed84e6f4c321496b97dc1e, counter=26}, lastWriteDate=Tue Oct 14 01:18:07 UTC 2025, lastUpdateTimeNanos=24678231400472}
          2025-10-14T01:18:18.854Z  INFO 1655 --- [lab2] [-mongodb1:27017] org.mongodb.driver.cluster               : Monitor thread successfully connected to server with description ServerDescription{address=mongodb1:27017, type=REPLICA_
          SET_SECONDARY, cryptd=false, state=CONNECTED, ok=true, minWireVersion=0, maxWireVersion=21, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=33822214, minRoundTripTimeNanos=506768, setName='rs0', can
          onicalAddress=mongodb1:27017, hosts=[mongodb2:27018, mongodb1:27017, mongodb3:27019], passives=[], arbiters=[], primary='mongodb1:27017', tagSet=TagSet{[]}, electionId=7fffffff000000000000000e, setVersion=1, topologyVersion=TopologyVersion{processId=68ed84e6d29ce3200db99020, counter=35}, lastWriteDate=Tue Oct 14 01:18:07 UTC 2025, lastUpdateTimeNanos=24679975644681}
          2025-10-14T01:18:18.862Z  INFO 1655 --- [lab2] [-mongodb1:27017] org.mongodb.driver.cluster               : Monitor thread successfully connected to server with description ServerDescription{address=mongodb1:27017, type=REPLICA_
          SET_PRIMARY, cryptd=false, state=CONNECTED, ok=true, minWireVersion=0, maxWireVersion=21, maxDocumentSize=16777216, logicalSessionTimeoutMinutes=30, roundTripTimeNanos=33822214, minRoundTripTimeNanos=506768, setName='rs0', canon
          icalAddress=mongodb1:27017, hosts=[mongodb2:27018, mongodb1:27017, mongodb3:27019], passives=[], arbiters=[], primary='mongodb1:27017', tagSet=TagSet{[]}, electionId=7fffffff000000000000000e, setVersion=1, topologyVersion=TopologyVersion{processId=68ed84e6d29ce3200db99020, counter=37}, lastWriteDate=Tue Oct 14 01:18:18 UTC 2025, lastUpdateTimeNanos=24679982752638}
          2025-10-14T01:18:18.862Z  INFO 1655 --- [lab2] [-mongodb1:27017] org.mongodb.driver.cluster               : Discovered replica set primary mongodb1:27017 with max election id 7fffffff000000000000000e and max set version 1       
            Read operations: 5 successful, 0 failed
            Write operations: 5 successful, 0 failed
          ```


2. Eventual Consistency:
    - Configure writes and reads for eventual consistency (w:1 for writes and default read concern in MongoDB).
    - Perform a write operation on one node. Immediately attempt to read it from another node. Observe if you can read stale (old) data before it propagates.
    - Implement a simple loop that repeatedly reads the data until the latest value is observed, demonstrating the "eventual" nature.
    - Discuss scenarios where eventual consistency is acceptable or even desirable (e.g., social media likes, sensor data) and why this choice is beneficial in those contexts (e.g., availability, performance).
      1. Run the Spring Boot application with consistency test:
          ```bash
          docker compose exec spring-app /bin/bash
          mvn spring-boot:run -Dspring-boot.run.arguments="--service=consistency"
          ```
      2. Experiment Results for Eventual Consistency with read from secondary after write in primary
          ```bash
          🧪 EVENTUAL CONSISTENCY TEST
          ✅ Write completed with w:1
          ✅ Document propagated after 104ms on attempt 2
          📊 Final document content: username_eventual_write
          ```

3. Causal Consistency (Optional / Bonus):
    - design an experiment to demonstrate that causally related operations are observed in order, even if other concurrent operations are not.
      1. Run the Spring Boot application with consistency test:
          ```bash
          docker compose exec spring-app /bin/bash
          mvn spring-boot:run -Dspring-boot.run.arguments="--service=consistency"
          ```
      2. Experiment Results for Causal Consistency with related operations observed in order
          ```bash
          🧪 CAUSAL CONSISTENCY TEST
          ✅ Created cause document: user_causal_1_1760404699902
          ✅ Created effect document: user_causal_2_1760404699902 (references user_causal_1_1760404699902)
            Verifying causal order:
              Round 1: Cause=FOUND, Effect=FOUND
          ✅ Causal consistency verified - related operations maintain order
          ```


## Distributed Transactions
<!-- Detailed conceptual analysis of the e-commerce workflow, contrasting ACID with Saga patterns and their trade-offs. -->
### Part D: Distributed Transactions (Conceptual / Optional Coding)
1. Review the challenges of distributed transactions (e.g., Saga pattern)
    - Challenges of Two/Three-Phase Commit Pattern:

      1. Performance Bottlenecks: Long-lived locks held on resources across all participating services for the entire transaction duration.

      2. Single Points of Failure: The central transaction coordinator becomes a critical bottleneck and a single point of failure.

      3. Scalability Issues: Distributed locks and synchronous communication hinder horizontal scaling.

      4. Blocking Problem: If the coordinator fails, participants may remain in a blocking state with locks held, awaiting a decision.

      5. Protocol Overhead: Multiple rounds of network communication (prepare, commit) increase latency.

    - Challenges of the Saga Pattern:

      1. Lack of Isolation: The absence of long-lived locks can lead to dirty reads, lost updates, or other phenomena if not managed.

      2. Complexity of Compensating Actions: Designing and implementing correct, idempotent rollback logic for each step adds significant development complexity.

      3. Eventual Consistency: The system is only eventually consistent, which can be challenging for business logic that requires immediate, strong consistency.

      4. Debugging and Monitoring: Tracing a long-running, distributed business flow across multiple services is more complex than monitoring a single database transaction.

      5. Dependency Management: In choreography-based Sagas, the event-driven flow can become complex and difficult to manage as the number of services grows.

2. Conceptual Exercise: for a simple multi-service workflow (e-commerce order involving OrderService, PaymentService, InventoryService)
    - Describe how this workflow would be managed using: 
        1. ACID transactions (and why it's problematic in a truly distributed system).

            - In a traditional ACID model, this workflow would be managed as a single distributed transaction spanning all three services. This would typically employ protocols like Two-Phase Commit (2PC) or Three-Phase Commit (3PC) to maintain atomicity across service boundaries.
            - Key Characteristics
              - Atomicity: All operations commit or rollback together
              - Consistency: Strong consistency guarantees throughout
              - Isolation: Intermediate states are not visible externally
              - Durability: Once committed, changes are permanent
            - Problems
              - Availability Impact: The entire transaction holds resources (locks) across services until completion, creating potential deadlocks and reducing system availability.

              - Performance Bottlenecks: Synchronous coordination between services introduces significant latency due to network round-trips for coordination messages, blocking while waiting for participant responses and lock contention across service boundaries

              - Scalability Limitations: Tight coupling prevents independent scaling of services and creates single points of failure.

              - Operational Complexity: Recovery from coordinator failures requires complex reconciliation procedures, often necessitating manual intervention.

        2. Sagas (Orchestrated or Choreographed).
            Sagas break the distributed transaction into a sequence of local transactions, each with corresponding compensation actions for rollback scenarios.
            1. Orchestrated Saga Implementation
                - Workflow Execution:
                  - Saga Orchestrator initiates the workflow
                  - Sends command to OrderService → creates order in "PENDING" state
                  - Sends command to PaymentService → processes payment
                  - Sends command to InventoryService → reserves inventory
                  - Updates order to "CONFIRMED" state upon success
                - Compensation Sequence:
                  - If inventory reservation fails: triggers payment refund → order cancellation
                  - If payment fails: triggers immediate order cancellation
                  - Each compensation is a separate transaction with its own durability
            2. Choreographed Saga Implementation
                - Event-Driven Flow:
                  - OrderService publishes OrderCreated event
                  - PaymentService consumes event → processes payment → publishes PaymentProcessed
                  - InventoryService consumes payment event → reserves stock → publishes InventoryReserved
                  - OrderService consumes inventory event → updates order status
                - Compensation via Events:
                  - Services listen for failure events and execute their compensation logic
                  - PaymentService listens for InventoryReservationFailed → triggers refund
                  - OrderService listens for compensation events → updates order status accordingly
    - Analyze the trade-offs between these approaches in terms of consistency, complexity, fault tolerance, and performance. You do not need to implement this part; a detailed conceptual explanation is sufficient.

      1. Consistency Trade-offs
          - ACID Transactions:
            - Strong Consistency: All services see consistent state at all times
            - Immediate Rollback: Failed transactions leave no side effects
            - Simplified Reasoning: Linear, predictable execution paths
          - Saga Pattern:
            - Eventual Consistency: Temporary inconsistencies during execution
            - Compensation Latency: Rollback is asynchronous and may have delays
            - Complex Failure States: Must handle partial failures and compensation failures
      2. Complexity Trade-offs
          - ACID Transactions:
            - Lower Business Logic Complexity: Simple commit/rollback semantics
            - Higher Infrastructure Complexity: Requires sophisticated distributed transaction managers
            - Simplified Error Handling: Binary success/failure outcomes
          - Saga Pattern:
            - Higher Business Logic Complexity: Must design and test compensation logic
            - Lower Infrastructure Complexity: Leverages existing messaging infrastructure
            - Complex Error Recovery: Must handle compensation failures and idempotency
          - Orchestrated vs. Choreographed Complexity:
            - Orchestrated: Centralized control simplifies monitoring but creates single point of failure
            - Choreographed: Better decoupling but harder to debug and monitor distributed logic

      3. Fault Tolerance Evaluation
          - ACID Transactions:
            - Brittle Failure Modes: Single service failure blocks entire system
            - Recovery Challenges: In-doubt transactions require manual resolution
            - Cascading Failures: Performance issues in one service affect all participants

          - Saga Pattern:
            - Resilient Design: Individual service failures don't block entire system
            - Graceful Degradation: Can operate partially while waiting for recovery
            - Recovery Automation: Built-in compensation mechanisms enable self-healing
      4. Performance Characteristics
          - ACID Transactions:
            - High Latency: Synchronous coordination and locking overhead
            - Poor Throughput: Limited by slowest participant service
            - Resource Intensive: Long-held locks and connections
          - Saga Pattern:
            - Better Responsiveness: Asynchronous execution improves user experience
            - Higher Throughput: Parallel execution opportunities in choreographed approach
            - Resource Efficiency: Shorter-lived transactions and connections
          
      5. Strategic Considerations
          - Use ACID Transactions When:
            - Strong consistency is non-negotiable (financial settlements, regulatory requirements)
            - Transaction scope is limited to services with co-located databases
            - Performance requirements permit synchronous coordination
            - Operational team has expertise in distributed transaction management

          - Use Saga Pattern When:
            - Services require autonomy and independent scalability
            - Eventual consistency is acceptable for the business domain
            - High availability and fault tolerance are critical requirements
            - System must remain responsive under partial failures


## Conclusion
<!-- Summary of key learnings and any unexpected observations. -->