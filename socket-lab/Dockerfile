FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY target/socket-lab-1.0-SNAPSHOT-server.jar /app/server.jar
COPY target/socket-lab-1.0-SNAPSHOT-client.jar /app/client.jar
CMD ["sh", "-c", "java -jar ${APP}.jar"]