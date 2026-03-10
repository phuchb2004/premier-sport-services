FROM maven:3.9-eclipse-temurin-23 AS builder
WORKDIR /build
COPY pom.xml .
COPY api-gateway ./api-gateway
RUN mvn install -N -q && mvn package -pl api-gateway -DskipTests -q

FROM eclipse-temurin:23-jre-jammy
WORKDIR /app
COPY --from=builder /build/api-gateway/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
