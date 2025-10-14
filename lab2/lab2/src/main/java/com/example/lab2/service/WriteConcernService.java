package com.example.lab2.service;

import com.example.lab2.model.entity.UserProfile;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WriteConcernService {
    private final MongoTemplate mongoTemplate;
    private final String TEST_COLLECTION = "write_concern_perf_test";
    private final int testCount = 500; // Number of test iterations for each write concern
    private final int warmUpCount = 5; // Number of warm-up iterations
    private final int batchSize = 400; // Number of documents to insert in each test

    /**
     * Main method to perform write concern performance experiments
     * Compares different write concern levels with batch insert operations
     */
    public void performWriteConcernExperiments() {
        System.out.println("\n=== WRITE CONCERN EXPERIMENTS ===");

        prepareTestEnvironment();

        System.out.println("\n=== WARM-UP PHASE ===");
        warmUpWriteConcernTests();

        System.out.println("\n=== FORMAL TESTING PHASE ===");

        // Test different write concern levels
        testWriteConcernWithAverage(WriteConcern.W3, "w:all");
        testWriteConcernWithAverage(WriteConcern.ACKNOWLEDGED, "w:1");
        testWriteConcernWithAverage(WriteConcern.MAJORITY, "w:majority");
        cleanupTestEnvironment();
        System.out.println("\n=== WRITE CONCERN EXPERIMENTS FINISHED ===");
    }

    /**
     * Prepares the test environment by creating a fresh collection
     */
    private void prepareTestEnvironment() {
        mongoTemplate.dropCollection(TEST_COLLECTION);
        mongoTemplate.createCollection(TEST_COLLECTION);
        System.out.println("✅ Test collection prepared: " + TEST_COLLECTION);
    }

    /**
     * Cleans up the test environment after experiments
     */
    private void cleanupTestEnvironment() {
        mongoTemplate.dropCollection(TEST_COLLECTION);
        System.out.println("✅ Test collection cleaned up");
    }

    /**
     * Performs warm-up tests to initialize connections and JIT compilation
     */
    private void warmUpWriteConcernTests() {
        System.out.println("Running warm-up tests...");

        for (int i = 0; i < warmUpCount; i++) {
            runBatchInsertTest(WriteConcern.ACKNOWLEDGED, "w:1_warmup_" + i, false);
            runBatchInsertTest(WriteConcern.MAJORITY, "w:majority_warmup_" + i, false);
            runBatchInsertTest(WriteConcern.W3.withWTimeout(5, java.util.concurrent.TimeUnit.SECONDS), "w:all_warmup_" + i, false);
        }

        mongoTemplate.getCollection(TEST_COLLECTION).deleteMany(new Document());
        System.out.println("✅ Warm-up completed and test data cleared");
    }

    /**
     * Tests a specific write concern level and calculates average performance metrics
     * @param writeConcern The write concern level to test
     * @param concernType Descriptive name for the write concern being tested
     */
    private void testWriteConcernWithAverage(WriteConcern writeConcern, String concernType) {
        System.out.println("\n🧪 Testing Write Concern: " + concernType +
                " (averaging " + testCount + " runs, " + batchSize + " docs per run)");

        long totalLatency = 0;
        java.util.List<Long> latencies = new java.util.ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < testCount; i++) {
            long latency = runBatchInsertTest(writeConcern, concernType + "_run_" + (i + 1), false);

            if (latency > 0) {
                totalLatency += latency;
                latencies.add(latency);
                successCount++;
            }

            // Small delay between tests to avoid overwhelming the system
            if (i < testCount - 1) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }

        if (successCount == 0) {
            System.out.println("❌ No successful tests for " + concernType);
            return;
        }

        // Calculate performance statistics
        long avgLatency = totalLatency / successCount;
        long minLatency = latencies.stream().min(Long::compareTo).orElse(0L);
        long maxLatency = latencies.stream().max(Long::compareTo).orElse(0L);

        double variance = latencies.stream()
                .mapToDouble(l -> Math.pow(l - avgLatency, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        // Display results
        System.out.println("📊 " + concernType + " Results:");
        System.out.println("  Average Latency: " + avgLatency + " ms");
        System.out.println("  Min Latency: " + minLatency + " ms");
        System.out.println("  Max Latency: " + maxLatency + " ms");
        System.out.println("  Std Deviation: " + String.format("%.2f", stdDev) + " ms");
        System.out.println("  Success Count: " + successCount + "/" + testCount);
        System.out.println("  Total Documents: " + (successCount * batchSize));

        // Clean up test data
        mongoTemplate.getCollection(TEST_COLLECTION).deleteMany(new Document());
    }

    /**
     * Performs a single batch insert test with the specified write concern
     * @param writeConcern The write concern level to use
     * @param testId Identifier for this test run
     * @param logError Whether to log errors
     * @return The latency in milliseconds, or -1 if failed
     */
    private long runBatchInsertTest(WriteConcern writeConcern, String testId, boolean logError) {
        try {
            MongoCollection<Document> collection = mongoTemplate.getCollection(TEST_COLLECTION)
                    .withWriteConcern(writeConcern);

            // Generate batch of test documents
            List<Document> documents = new ArrayList<>();
            long baseTimestamp = System.nanoTime();

            for (int i = 0; i < batchSize; i++) {
                UserProfile testUser = new UserProfile(
                        "test_id_" + testId + "_" + baseTimestamp + "_" + i,
                        "test_user_id_" + testId + "_" + baseTimestamp + "_" + i,
                        "test_user_" + baseTimestamp + "_" + i,
                        "test_" + baseTimestamp + "_" + i + "@example.com",
                        Instant.now().toEpochMilli()
                );

                Document doc = new Document();
                mongoTemplate.getConverter().write(testUser, doc);
                documents.add(doc);
            }

            // Perform batch insert and measure latency
            long startTime = System.nanoTime();
            collection.insertMany(documents);
            long endTime = System.nanoTime();

            long latency = (endTime - startTime) / 1_000_000;

            if (logError) {
                System.out.println("    " + testId + " completed: " + latency + " ms (" + batchSize + " docs)");
            }

            return latency;

        } catch (Exception e) {
            if (logError) {
                System.out.println("❌ Error in test " + testId + ": " + e.getMessage());
            }
            return -1;
        }
    }
}