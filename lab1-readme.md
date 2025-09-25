#### Check three applications
- socket-lab
- rest-api-lab
- grpc-lab
#### Detailed documentation can be found in each readme file

#### You can run them one by one following corresponding readme file, or run docker-compose.yml here
```bash
docker-compose -p zeli-lab1 -f lab1-docker-compose.yml up -d --build
```

#### To run socket-lab client:
```bash
docker run -it -e APP=client --rm --network="host" socket-lab
```

#### To clean up
```bash
docker compose -p zeli-lab1 -f lab1-docker-compose.yml down --rmi all
```