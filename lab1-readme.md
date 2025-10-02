#### Check three applications, detailed documentation can be found in each readme file
- socket-lab
- rest-api-lab
- grpc-lab

#### You can run them through docker-compose.yml here (Highly recommended!)
#### or you can run them one by one from corresponding readme file
```bash
mvn clean test package
docker compose -p zeli-lab1 -f lab1-docker-compose.yml up -d --build
```

If anything goes wrong (mvn test package failed caused by different OS, mine is Windows), you can try my built images
```bash
docker compose -p zeli-lab1 -f lab1-docker-compose-zeli8888.yml up -d
```

#### To run socket-lab client:
```bash
docker run -it -e APP=client --rm --network="host" socket-lab
```

#### To run grpc-lab client (contains restful and grpc benchmark test option):
```bash
docker run -it --rm -e RUN_MODE=client --network="host" grpc-lab
```

#### To clean up
```bash
docker compose -p zeli-lab1 -f lab1-docker-compose.yml down --rmi all
```