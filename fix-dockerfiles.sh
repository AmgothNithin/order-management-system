write_dockerfile() {
  SERVICE=$1
  PORT=$2

  cat > $SERVICE/Dockerfile << EOL
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /workspace

COPY . .

RUN mvn clean install -DskipTests --no-transfer-progress

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /workspace/$SERVICE/target/*.jar app.jar

EXPOSE $PORT

ENTRYPOINT ["java","-jar","app.jar"]
EOL
}

write_dockerfile order-service 8081
write_dockerfile payment-service 8082
write_dockerfile inventory-service 8083
write_dockerfile notification-service 8084
