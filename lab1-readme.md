#### Check three applications, detailed documentation can be found in each readme file
- socket-lab
- rest-api-lab
- grpc-lab

#### You can run them through docker-compose.yml here (Highly recommended!)
#### or you can run them one by one from corresponding readme file
```bash
docker compose -p zeli-lab1 -f lab1-docker-compose.yml up -d --build
```

#### To run socket-lab client:
```bash
docker run -it -e APP=client --rm --network="host" socket-lab
```

#### To run grpc-lab client:
```bash
docker run -it --rm -e RUN_MODE=client --network="host" grpc-lab
```

#### To clean up
```bash
docker compose -p zeli-lab1 -f lab1-docker-compose.yml down --rmi all
```