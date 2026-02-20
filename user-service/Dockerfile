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

# Install parent pom
RUN mvn install -N

# Copy shared-lib source and build it first
COPY shared-lib/src shared-lib/src
RUN mvn install -pl shared-lib -DskipTests

# Copy user-service source
COPY user-service/src user-service/src

# Build user-service
RUN mvn clean package -pl user-service -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy jar from build stage
COPY --from=build /app/user-service/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
