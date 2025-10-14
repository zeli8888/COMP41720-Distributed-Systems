package com.example.lab2;

import com.example.lab2.service.ConsistencyModelService;
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
    private final ConsistencyModelService consistencyModelService;

    public static void main(String[] args) {
        SpringApplication.run(Lab2Application.class, args);
    }

    @Override
    public void run(String... args) {
        userProfileService.checkReplicaSetStatus();

        String serviceToRun = parseServiceArgument(args);

        switch (serviceToRun) {
            case "write-concern":
                writeConcernService.performWriteConcernExperiments();
                break;
            case "replication":
                replicationModelService.performReplicationExperiments();
                break;
            case "consistency":
                consistencyModelService.performConsistencyExperiments();
                break;
            default:
                System.out.println("no valid args, existing");
        }
    }

    private String parseServiceArgument(String... args) {
        for (String arg : args) {
            if (arg.startsWith("--service=")) {
                return arg.substring("--service=".length());
            }
        }
        return "";
    }
}
