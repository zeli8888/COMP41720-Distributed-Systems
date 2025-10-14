FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/grpc-lab-0.0.1-SNAPSHOT.jar app.jar

ENV RUN_MODE="server"
ENV GRPC_SERVER_HOST="localhost"
ENV GRPC_SERVER_PORT="50051"

CMD ["sh", "-c", "if [ \"$RUN_MODE\" = \"client\" ]; then \
        echo \"Starting gRPC Client in interactive mode...\"; \
        echo \"Connecting to: $GRPC_SERVER_HOST:$GRPC_SERVER_PORT\"; \
        java -jar app.jar; \
    else \
        echo \"Starting gRPC Server...\"; \
        java -jar app.jar; \
    fi"]