package com.example.lab2.service;

import com.example.lab2.model.entity.UserProfile;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ReplicationModelService {
    private final MongoTemplate mongoTemplate;
    private final String TEST_COLLECTION = "replication_model_test";
    private final int OPERATION_COUNT = 50;

    /**
     * Main method to perform Leader-Follower replication model experiments
     */
    public void performReplicationExperiments() {
        System.out.println("\n=== LEADER-FOLLOWER REPLICATION MODEL EXPERIMENTS ===");

        prepareTestEnvironment();

        // Experiment 1: Basic write propagation to followers
        testWritePropagationToFollowers();

        // Experiment 2: Read preferences
        testReadPreferences();

        // Experiment 3: Primary failure simulation using stepDown
        simulatePrimaryFailureWithStepDown();

        cleanupTestEnvironment();
        System.out.println("\n=== REPLICATION EXPERIMENTS FINISHED ===");
    }

    /**
     * Test 1: Demonstrate writes to primary and propagation to followers
     */
    private void testWritePropagationToFollowers() {
        System.out.println("\n🧪 TEST 1: WRITE PROPAGATION TO FOLLOWERS");

        try {
            // Write to primary
            System.out.println("📝 Writing " + OPERATION_COUNT + " documents to primary...");

            List<UserProfile> writtenDocuments = new ArrayList<>();
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < OPERATION_COUNT; i++) {
                UserProfile document = createTestDocument("primary_write_" + i, i);
                mongoTemplate.insert(document, TEST_COLLECTION);
                writtenDocuments.add(document);
            }

            long writeTime = System.currentTimeMillis() - startTime;
            System.out.println("✅ Primary write completed in " + writeTime + "ms");

            // Verify data propagation to secondaries
            verifyDataPropagation(writtenDocuments);

        } catch (Exception e) {
            System.out.println("❌ Write propagation test failed: " + e.getMessage());
        }
    }

    /**
     * Test 2: Test different read preferences
     */
    private void testReadPreferences() {
        System.out.println("\n🧪 TEST 2: READ PREFERENCE BEHAVIOR");

        // Create test data
        UserProfile testDoc = createTestDocument("read_pref_test", 999);
        mongoTemplate.insert(testDoc, TEST_COLLECTION);

        // Test different read preferences
        testReadPreference("Primary", ReadPreference.primary(), testDoc.getId());
        testReadPreference("Secondary", ReadPreference.secondary(), testDoc.getId());
        testReadPreference("Primary Preferred", ReadPreference.primaryPreferred(), testDoc.getId());
    }

    /**
     * Test 3: Simulate primary node failure using stepDown command
     */
    private void simulatePrimaryFailureWithStepDown() {
        System.out.println("\n🧪 TEST 3: PRIMARY FAILOVER WITH stepDown COMMAND");

        try {
            // Get initial state
            Document initialStatus = getReplicaSetStatus();
            String originalPrimary = getCurrentPrimary(initialStatus);
            System.out.println("  Current Primary: " + originalPrimary);

            if ("UNKNOWN".equals(originalPrimary)) {
                System.out.println("❌ Cannot identify current primary");
                return;
            }

            // Start concurrent operations
            System.out.println("  Starting concurrent operations during failover...");
            CompletableFuture<Void> operations = startConcurrentOperations();

            // Trigger stepDown on current primary
            System.out.println("  🔌 Triggering stepDown on primary...");
            triggerStepDown();

            // Monitor failover process
            monitorFailoverProcess(originalPrimary);

            // Wait for operations to complete
            try {
                operations.get(20, TimeUnit.SECONDS);
            } catch (Exception e) {
                System.out.println("  ⚠️ Operations interrupted: " + e.getMessage());
            }

            // Verify new primary
            Document newStatus = getReplicaSetStatus();
            String newPrimary = getCurrentPrimary(newStatus);
            System.out.println("  New Primary: " + newPrimary);

            if (!originalPrimary.equals(newPrimary)) {
                System.out.println("✅ Failover successful");
            } else {
                System.out.println("❌ Failover failed - same primary");
            }

        } catch (Exception e) {
            System.out.println("❌ Primary failure simulation error: " + e.getMessage());
        }
    }

    /**
     * Trigger stepDown command on current primary
     */
    private void triggerStepDown() {
        try {
            MongoDatabase adminDb = mongoTemplate.getMongoDatabaseFactory().getMongoDatabase("admin");

            // Force primary to step down for 30 seconds
            Document stepDownCmd = new Document("replSetStepDown", 30)
                    .append("force", true);

            adminDb.runCommand(stepDownCmd);
            System.out.println("    ✅ StepDown command sent");

        } catch (Exception e) {
            System.out.println("    ⚠️ StepDown command may have failed (expected during transition): " + e.getMessage());
        }
    }

    /**
     * Monitor the failover process
     */
    private void monitorFailoverProcess(String originalPrimary) {
        System.out.println("  📊 Monitoring failover process...");

        long startTime = System.currentTimeMillis();
        long timeout = TimeUnit.SECONDS.toMillis(25);
        boolean newPrimaryElected = false;

        while (System.currentTimeMillis() - startTime < timeout && !newPrimaryElected) {
            try {
                Document status = getReplicaSetStatus();
                String currentPrimary = getCurrentPrimary(status);

                if (!"UNKNOWN".equals(currentPrimary) && !currentPrimary.equals(originalPrimary)) {
                    newPrimaryElected = true;
                    long failoverTime = System.currentTimeMillis() - startTime;
                    System.out.println("    ⚡ New primary elected after " + failoverTime + "ms");
                }

                Thread.sleep(1000);
            } catch (Exception e) {
                // Expected during election
            }
        }

        if (!newPrimaryElected) {
            System.out.println("    ⏰ No new primary elected within timeout");
        }
    }

    /**
     * Start concurrent operations during failover
     */
    private CompletableFuture<Void> startConcurrentOperations() {
        return CompletableFuture.runAsync(() -> {
            int successfulWrites = 0;
            int failedWrites = 0;

            for (int i = 0; i < 20; i++) {
                try {
                    UserProfile doc = createTestDocument("failover_write_" + i, 2000 + i);
                    mongoTemplate.insert(doc, TEST_COLLECTION);
                    successfulWrites++;
                    Thread.sleep(200);
                } catch (Exception e) {
                    failedWrites++;
                    // Expected during failover
                }
            }

            System.out.println("    📊 Concurrent operations: " + successfulWrites + " successful, " + failedWrites + " failed");
        });
    }

    /**
     * Verify data propagation to secondaries
     */
    private void verifyDataPropagation(List<UserProfile> expectedDocuments) {
        System.out.println("🔍 Verifying data propagation to secondaries...");

        MongoCollection<Document> secondaryCollection = mongoTemplate.getCollection(TEST_COLLECTION)
                .withReadPreference(ReadPreference.secondary());

        int successfullyPropagated = 0;

        for (UserProfile expectedDoc : expectedDocuments) {
            try {
                Document actualDoc = secondaryCollection
                        .find(Filters.eq("_id", expectedDoc.getId()))
                        .first();

                if (actualDoc != null) {
                    successfullyPropagated++;
                }
            } catch (Exception e) {
                // Expected if secondary is unavailable
            }
        }

        System.out.println("📊 Data Propagation: " + successfullyPropagated + "/" + expectedDocuments.size() + " documents");

        double successRate = (double) successfullyPropagated / expectedDocuments.size() * 100;
        System.out.println("📈 Success rate: " + String.format("%.2f", successRate) + "%");
    }

    /**
     * Test specific read preference
     */
    private void testReadPreference(String testName, ReadPreference readPreference, String documentId) {
        System.out.println("  Testing: " + testName);

        try {
            MongoCollection<Document> collection = mongoTemplate.getCollection(TEST_COLLECTION)
                    .withReadPreference(readPreference);

            long startTime = System.nanoTime();
            Document result = collection.find(Filters.eq("_id", documentId)).first();
            long latency = (System.nanoTime() - startTime) / 1_000_000;

            if (result != null) {
                System.out.println("    ✅ Document found - Latency: " + latency + "ms");
            } else {
                System.out.println("    ⚠️ Document not found");
            }
        } catch (Exception e) {
            System.out.println("    ❌ Read failed: " + e.getMessage());
        }
    }

    /**
     * Utility methods
     */
    private UserProfile createTestDocument(String suffix, int index) {
        return new UserProfile(
                "test_id_" + suffix + "_" + System.currentTimeMillis(),
                "user_id_" + suffix,
                "username_" + suffix,
                "email_" + index + "@example.com",
                Instant.now().toEpochMilli()
        );
    }

    private Document getReplicaSetStatus() {
        try {
            MongoDatabase adminDb = mongoTemplate.getMongoDatabaseFactory().getMongoDatabase("admin");
            return adminDb.runCommand(new Document("replSetGetStatus", 1));
        } catch (Exception e) {
            return new Document("members", new ArrayList<>());
        }
    }

    private String getCurrentPrimary(Document replStatus) {
        try {
            List<Document> members = (List<Document>) replStatus.get("members");
            for (Document member : members) {
                if (member.getInteger("state") == 1) { // PRIMARY state
                    return member.getString("name");
                }
            }
        } catch (Exception e) {
            // Fall through to return UNKNOWN
        }
        return "UNKNOWN";
    }

    private void prepareTestEnvironment() {
        mongoTemplate.dropCollection(TEST_COLLECTION);
        mongoTemplate.createCollection(TEST_COLLECTION);
        System.out.println("✅ Test collection prepared: " + TEST_COLLECTION);
    }

    private void cleanupTestEnvironment() {
        mongoTemplate.dropCollection(TEST_COLLECTION);
        System.out.println("✅ Test collection cleaned up");
    }
}