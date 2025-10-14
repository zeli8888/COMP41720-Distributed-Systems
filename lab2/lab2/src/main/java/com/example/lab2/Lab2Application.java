package com.example.lab2;

import com.example.lab2.service.ReplicationModelService;
import com.example.lab2.service.UserProfileService;
import com.example.lab2.service.WriteConcernService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class Lab2Application implements CommandLineRunner {
    private final UserProfileService userProfileService;
    private final WriteConcernService writeConcernService;
    private final ReplicationModelService replicationModelService;

    public static void main(String[] args) {
        SpringApplication.run(Lab2Application.class, args);
    }

    @Override
    public void run(String... args) {
        userProfileService.checkReplicaSetStatus();
//        writeConcernService.performWriteConcernExperiments();
        replicationModelService.performReplicationExperiments();
    }
}
