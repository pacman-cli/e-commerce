# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy parent POM
COPY pom.xml .
COPY shared-lib/pom.xml shared-lib/
COPY api-gateway/pom.xml api-gateway/
COPY user-service/pom.xml user-service/
COPY order-service/pom.xml order-service/
COPY payment-service/pom.xml payment-service/
COPY notification-service/pom.xml notification-service/

# Copy shared-lib source and build it first
COPY shared-lib/src shared-lib/src
RUN mvn install -pl shared-lib -DskipTests

# Copy api-gateway source
COPY api-gateway/src api-gateway/src

# Build api-gateway
RUN mvn clean package -pl api-gateway -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/api-gateway/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
