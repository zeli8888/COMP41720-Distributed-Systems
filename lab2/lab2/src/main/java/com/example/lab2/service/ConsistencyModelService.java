package com.example.lab2.service;

import com.example.lab2.model.entity.UserProfile;
import com.mongodb.ClientSessionOptions;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.WriteConcern;
import com.mongodb.ReadConcern;
import com.mongodb.client.ClientSession;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ConsistencyModelService {
    private final MongoTemplate mongoTemplate;
    private final String TEST_COLLECTION = "consistency_test";

    /**
     * Main method to perform all consistency model experiments
     */
    public void performConsistencyExperiments() {
        System.out.println("\n=== CONSISTENCY MODEL EXPERIMENTS ===");

        prepareTestEnvironment();

        // Experiment 1: Strong Consistency
        testStrongConsistency();

        // Experiment 2: Eventual Consistency
        testEventualConsistency();

        // Experiment 3: Causal Consistency
        testCausalConsistency();

        cleanupTestEnvironment();
        System.out.println("\n=== CONSISTENCY EXPERIMENTS FINISHED ===");
    }

    /**
     * Test 1: Strong Consistency with w:majority and readConcern:majority
     */
    private void testStrongConsistency() {
        System.out.println("\n🧪 STRONG CONSISTENCY TEST");

        try {
            MongoCollection<Document> strongCollection = getStrongConsistencyCollection();
            String testId = "strong_test_" + System.currentTimeMillis();

            // Write with majority
            UserProfile testDoc = createTestDocument(testId, "strong_write", 1);
            strongCollection.insertOne(convertToDocument(testDoc));
            System.out.println("✅ Write completed with w:majority");

            // Read with majority from secondary
            MongoCollection<Document> secondaryReadCollection = mongoTemplate.getCollection(TEST_COLLECTION)
                    .withReadPreference(ReadPreference.secondary())
                    .withReadConcern(ReadConcern.MAJORITY);

            Document readResult = secondaryReadCollection.find(Filters.eq("_id", testId)).first();

            if (readResult != null) {
                System.out.println("✅ Strong consistency verified - immediate read from secondary");
                System.out.println("📊 Document content: " + readResult.getString("username"));
            } else {
                System.out.println("❌ Strong consistency failed - document not found on secondary");
            }

            // Test during network partition
            testStrongConsistencyDuringPartition(testId);

        } catch (Exception e) {
            System.out.println("❌ Strong consistency test error: " + e.getMessage());
        }
    }

    /**
     * Test strong consistency behavior during network partition
     */
    private void testStrongConsistencyDuringPartition(String testId) {
        System.out.println("  Testing during network partition...");

        CompletableFuture<Void> writeOperations = startMajorityWriteOperations();
        CompletableFuture<Void> readOperations = startMajorityReadOperations(testId);

        triggerStepDown();

        try {
            writeOperations.get(20, TimeUnit.SECONDS);
            readOperations.get(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("  Operations affected by partition: " + e.getMessage());
        }
    }

    /**
     * Test 2: Eventual Consistency with w:1 and default read concern
     */
    private void testEventualConsistency() {
        System.out.println("\n🧪 EVENTUAL CONSISTENCY TEST");

        try {
            MongoCollection<Document> eventualCollection = getEventualConsistencyCollection();
            String testId = "eventual_test_" + System.currentTimeMillis();

            // Write with w:1
            UserProfile testDoc = createTestDocument(testId, "eventual_write", 2);
            eventualCollection.insertOne(convertToDocument(testDoc));
            System.out.println("✅ Write completed with w:1");

            // Immediate read from secondary
            MongoCollection<Document> secondaryCollection = mongoTemplate.getCollection(TEST_COLLECTION)
                    .withReadPreference(ReadPreference.secondary());

            boolean found = false;
            int attempts = 0;
            long startTime = System.currentTimeMillis();

            while (!found && attempts < 10) {
                Document readResult = secondaryCollection.find(Filters.eq("_id", testId)).first();
                if (readResult != null) {
                    found = true;
                    long propagationTime = System.currentTimeMillis() - startTime;
                    System.out.println("✅ Document propagated after " + propagationTime + "ms on attempt " + (attempts + 1));
                    System.out.println("📊 Final document content: " + readResult.getString("username"));
                } else {
                    attempts++;
                    Thread.sleep(100);
                }
            }

            if (!found) {
                System.out.println("❌ Document not propagated within " + attempts + " attempts");
            }

        } catch (Exception e) {
            System.out.println("❌ Eventual consistency test error: " + e.getMessage());
        }
    }

    /**
     * Test 3: Causal Consistency
     */
    private void testCausalConsistency() {
        System.out.println("\n🧪 CAUSAL CONSISTENCY TEST");

        try (ClientSession session = mongoTemplate.getMongoDatabaseFactory().getSession(ClientSessionOptions.builder().causallyConsistent(true).build())) {

            // Create causally related operations
            String user1Id = "user_causal_1_" + System.currentTimeMillis();
            String user2Id = "user_causal_2_" + System.currentTimeMillis();

            // Operation A: Create first user
            UserProfile user1 = createTestDocument(user1Id, "causal_user1", 100);
            mongoTemplate.insert(user1, TEST_COLLECTION);
            System.out.println("✅ Created cause document: " + user1Id);

            // Operation B: Create second user that references first (causally after A)
            UserProfile user2 = createTestDocument(user2Id, "causal_user2", 101);
            user2.setEmail("ref_" + user1Id + "@example.com"); // Reference to first user
            mongoTemplate.insert(user2, TEST_COLLECTION);
            System.out.println("✅ Created effect document: " + user2Id + " (references " + user1Id + ")");

            // Verify causal order is maintained
            verifyCausalOrder(user1Id, user2Id);

        } catch (Exception e) {
            System.out.println("❌ Causal consistency test error: " + e.getMessage());
        }
    }

    /**
     * Verify that causally related operations maintain order
     */
    private void verifyCausalOrder(String firstId, String secondId) {
        try {
            MongoCollection<Document> collection = mongoTemplate.getCollection(TEST_COLLECTION)
                    .withReadPreference(ReadPreference.secondary());

            // Simulate multiple reads to check order consistency
            boolean correctOrder = true;
            int verificationRounds = 5;

            System.out.println("  Verifying causal order:");

            for (int i = 0; i < verificationRounds; i++) {
                Document firstDoc = collection.find(Filters.eq("_id", firstId)).first();
                Document secondDoc = collection.find(Filters.eq("_id", secondId)).first();

                String firstStatus = firstDoc != null ? "FOUND" : "NOT_FOUND";
                String secondStatus = secondDoc != null ? "FOUND" : "NOT_FOUND";

                System.out.println("    Round " + (i + 1) + ": Cause=" + firstStatus + ", Effect=" + secondStatus);

                // If we see the second document, we must also see the first (causal dependency)
                if (secondDoc != null && firstDoc == null) {
                    correctOrder = false;
                    System.out.println("    ❌ Causal violation: Effect found without Cause");
                    break;
                } else if (secondDoc != null && firstDoc != null) {
                    break;
                }

                Thread.sleep(50);
            }

            if (correctOrder) {
                System.out.println("✅ Causal consistency verified - related operations maintain order");
            } else {
                System.out.println("❌ Causal consistency violated");
            }

        } catch (Exception e) {
            System.out.println("❌ Causal order verification error: " + e.getMessage());
        }
    }

    /**
     * Start write operations that require majority consensus
     */
    private CompletableFuture<Void> startMajorityWriteOperations() {
        return CompletableFuture.runAsync(() -> {
            int successfulOps = 0;
            int failedOps = 0;

            MongoCollection<Document> strongCollection = getStrongConsistencyCollection();

            for (int i = 0; i < 5; i++) {
                try {
                    UserProfile doc = createTestDocument("majority_write_" + i, "majority_test", 1000 + i);
                    strongCollection.insertOne(convertToDocument(doc));
                    successfulOps++;
                } catch (Exception e) {
                    failedOps++;
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            System.out.println("  Write operations: " + successfulOps + " successful, " + failedOps + " failed");
        });
    }

    /**
     * Start read operations that require majority consensus
     */
    private CompletableFuture<Void> startMajorityReadOperations(String testId) {
        return CompletableFuture.runAsync(() -> {
            int successfulOps = 0;
            int failedOps = 0;

            MongoCollection<Document> strongCollection = getStrongConsistencyCollection();

            for (int i = 0; i < 5; i++) {
                try {
                    Document result = strongCollection.find(Filters.eq("_id", testId)).first();
                    if (result != null) {
                        successfulOps++;
                    } else {
                        failedOps++;
                    }
                } catch (Exception e) {
                    failedOps++;
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            System.out.println("  Read operations: " + successfulOps + " successful, " + failedOps + " failed");
        });
    }

    /**
     * Utility methods
     */
    private MongoCollection<Document> getStrongConsistencyCollection() {
        return mongoTemplate.getCollection(TEST_COLLECTION)
                .withWriteConcern(WriteConcern.MAJORITY)
                .withReadConcern(ReadConcern.MAJORITY);
    }

    private MongoCollection<Document> getEventualConsistencyCollection() {
        return mongoTemplate.getCollection(TEST_COLLECTION)
                .withWriteConcern(WriteConcern.W1)
                .withReadConcern(ReadConcern.DEFAULT);
    }

    private void triggerStepDown() {
        try {
            MongoDatabase adminDb = mongoTemplate.getMongoDatabaseFactory().getMongoDatabase("admin");
            Document stepDownCmd = new Document("replSetStepDown", 15).append("force", true);
            adminDb.runCommand(stepDownCmd);
            System.out.println("  🔌 StepDown command sent to simulate network partition");
        } catch (Exception e) {
            // Expected during transition
        }
    }

    private UserProfile createTestDocument(String id, String suffix, int index) {
        return new UserProfile(
                id,
                "user_id_" + suffix,
                "username_" + suffix,
                "email_" + index + "@example.com",
                Instant.now().toEpochMilli()
        );
    }

    private Document convertToDocument(UserProfile profile) {
        Document doc = new Document();
        doc.put("_id", profile.getId());
        doc.put("userId", profile.getUserId());
        doc.put("username", profile.getUsername());
        doc.put("email", profile.getEmail());
        doc.put("last_login_time", profile.getLastLoginTime());
        return doc;
    }

    private void prepareTestEnvironment() {
        mongoTemplate.dropCollection(TEST_COLLECTION);
        mongoTemplate.createCollection(TEST_COLLECTION);
        System.out.println("✅ Test collection prepared");
    }

    private void cleanupTestEnvironment() {
        mongoTemplate.dropCollection(TEST_COLLECTION);
        System.out.println("✅ Test collection cleaned up");
    }
}