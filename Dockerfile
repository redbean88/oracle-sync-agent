FROM maven:3.9.6-eclipse-temurin-21-alpine

WORKDIR /app

# Copy the pom.xml and download dependencies (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src ./src

# Run tests by default
CMD ["mvn", "test"]
