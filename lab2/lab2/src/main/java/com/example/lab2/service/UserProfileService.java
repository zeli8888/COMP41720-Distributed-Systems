package com.example.lab2.service;

import com.example.lab2.model.entity.UserProfile;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final MongoTemplate mongoTemplate;

    public void performWriteConcernExperiments() {
        System.out.println("\n=== WRITE CONCERN EXPERIMENTS ===");
        mongoTemplate.executeCommand("{ isMaster: 1 }");
        System.out.println("✅ MongoDB连接成功");

        // 测试副本集状态
        Document replStatus = mongoTemplate.executeCommand("{ replSetGetStatus: 1 }");
        System.out.println("✅ 副本集状态: " + replStatus.get("set"));

//        // Test w:1 (Acknowledged)
//        testWriteConcern(WriteConcern.ACKNOWLEDGED, "w:1");
//
//        // Test w:majority
//        testWriteConcern(WriteConcern.MAJORITY, "w:majority");
//
//        // Test w:all
//        testWriteConcern(WriteConcern.W3.withWTimeout(5, TimeUnit.SECONDS), "w:all");

        System.out.println("\n=== WRITE CONCERN EXPERIMENTS FINISHED===");
    }

    private void testWriteConcern(WriteConcern writeConcern, String concernType) {
        System.out.println("\nTesting Write Concern: " + concernType);
        mongoTemplate.dropCollection(UserProfile.class);
        mongoTemplate.createCollection(UserProfile.class);
        System.out.println("Collection cleared and recreated");
        UserProfile testUser = new UserProfile(
                "test_id_" + concernType.replace(":", "_"),
                "test_user_id_" + concernType.replace(":", "_"),
                "test_user_" + System.currentTimeMillis(),
                "test@example.com",
                Instant.now().toEpochMilli()
        );
        MongoCollection<Document> collection = mongoTemplate.getCollection(mongoTemplate.getCollectionName(UserProfile.class))
                .withWriteConcern(writeConcern);
        Document doc = new Document();
        mongoTemplate.getConverter().write(testUser, doc);
        try {
            wait(1000);
        } catch (InterruptedException e) {
            System.out.printf("Error during wait: %s%n", e.getMessage());
            throw new RuntimeException(e);
        }
        long startTime = System.nanoTime();
        collection.insertOne(doc);
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        System.out.println("Write with " + concernType + " completed successfully");
        System.out.println("Latency: " + durationMs + " ms");
    }
}
