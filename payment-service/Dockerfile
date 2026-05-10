FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

COPY pom.xml .
COPY common-lib common-lib
COPY payment-service payment-service

RUN apk add --no-cache maven &&     mvn -pl common-lib install -DskipTests --no-transfer-progress &&     mvn -pl payment-service package -DskipTests --no-transfer-progress

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=builder /workspace/payment-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]
