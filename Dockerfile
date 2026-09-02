# =========================================================
# Backend Dockerfile for Vehicle Rental System on Render
# =========================================================

# Stage 1: Build Spring Boot Backend
FROM maven:3.9-eclipse-temurin-17-alpine AS backend-builder
WORKDIR /app
COPY pom.xml ./
COPY monolith/pom.xml monolith/
COPY monolith/ monolith/

# Build monolith JAR
RUN mvn -f monolith/pom.xml clean package -DskipTests

# Stage 2: Lightweight Runtime Container
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/monolith/target/*.jar app.jar

ENV PORT=8080
ENV JAVA_OPTS="-Xmx384m -Xss512k -XX:+UseG1GC"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
