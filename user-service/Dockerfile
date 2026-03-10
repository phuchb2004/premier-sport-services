FROM maven:3.9-eclipse-temurin-23 AS builder
WORKDIR /build
COPY pom.xml .
COPY common-lib ./common-lib
COPY user-service ./user-service
RUN mvn install -N -q && mvn package -pl common-lib,user-service -DskipTests -q

FROM eclipse-temurin:23-jre-jammy
WORKDIR /app
COPY --from=builder /build/user-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
