FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY pom.xml .
COPY common-lib common-lib
COPY order-service order-service
COPY payment-service payment-service
COPY inventory-service inventory-service
COPY notification-service notification-service
RUN mvn -pl common-lib install -DskipTests --no-transfer-progress && \
    mvn -pl payment-service package -DskipTests --no-transfer-progress

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring
COPY --from=builder /workspace/payment-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]
