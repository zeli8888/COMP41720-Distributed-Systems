package com.example.lab2.service;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final MongoClient mongoClient;

    /**
     * Checks and displays the current replica set status
     * Helps diagnose if the replica set is properly configured
     */
    public void checkReplicaSetStatus() {
        try {
            MongoDatabase adminDb = mongoClient.getDatabase("admin");
            Document replStatus = adminDb.runCommand(new Document("replSetGetStatus", 1));

            System.out.println("🔍 Replica Set Diagnostics:");
            System.out.println("  Set Name: " + replStatus.get("set"));

            java.util.List<Document> members = (java.util.List<Document>) replStatus.get("members");
            for (Document member : members) {
                System.out.println("  Member: " + member.get("name") +
                        " - State: " + member.get("stateStr") +
                        " - Health: " + member.get("health"));
            }
        } catch (Exception e) {
            System.out.println("❌ Could not get replica set status: " + e.getMessage());
        }
    }
}
