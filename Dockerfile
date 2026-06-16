# Use an OpenJDK base image
# FROM openjdk:17-jdk-alpine

# Use Amazon Corretto (AWS recommended)
FROM amazoncorretto:21

# Set the working directory inside the container
WORKDIR /pharma

# Copy the JAR file from the host machine to the container
COPY target/pharma.jar pharma.jar

# Expose the application port (default Spring Boot port)
# another port 8080 is exposed
EXPOSE 8081

# Run the application
CMD ["java", "-jar", "pharma.jar"]
