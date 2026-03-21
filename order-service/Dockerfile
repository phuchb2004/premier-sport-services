FROM maven:3.9-eclipse-temurin-23 AS builder
WORKDIR /build
COPY . .
RUN mvn install -N -q && mvn package -pl common-lib,order-service -DskipTests -q

FROM eclipse-temurin:23-jre-noble
WORKDIR /app
COPY --from=builder /build/order-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
