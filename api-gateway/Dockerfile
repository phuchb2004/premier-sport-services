FROM maven:3.9-eclipse-temurin-23 AS builder
WORKDIR /build
COPY . .
RUN mvn install -N -q && mvn package -pl api-gateway -DskipTests -q

FROM eclipse-temurin:23-jre-noble
WORKDIR /app
COPY --from=builder /build/api-gateway/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
