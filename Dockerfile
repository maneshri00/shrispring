# Use Java 17 (recommended for Spring Boot)
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# Copy Maven wrapper and pom
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests

# Run the app
EXPOSE 8080
CMD ["java", "-jar", "target/*.jar"]
