# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy the parent POM and shared-lib first to cache dependencies
COPY pom.xml .
COPY shared-lib /app/shared-lib
RUN mvn -pl shared-lib clean install -DskipTests

# Copy the product-service module and build
COPY product-service /app/product-service
RUN mvn -pl product-service clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/product-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
