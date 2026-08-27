# =========================================================
# Multi-Stage Dockerfile for Vehicle Rental System on Render
# =========================================================

# Stage 1: Build Angular Frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend-angular/package*.json ./
RUN npm install
COPY frontend-angular/ ./
RUN npm run build

# Stage 2: Build Spring Boot Backend
FROM maven:3.9-eclipse-temurin-17-alpine AS backend-builder
WORKDIR /app
COPY pom.xml ./
COPY monolith/pom.xml monolith/
COPY eureka-server/pom.xml eureka-server/
COPY api-gateway/pom.xml api-gateway/
COPY auth-service/pom.xml auth-service/
COPY vehicle-service/pom.xml vehicle-service/
COPY reservation-service/pom.xml reservation-service/
COPY payment-service/pom.xml payment-service/

COPY monolith/ monolith/
COPY eureka-server/ eureka-server/
COPY api-gateway/ api-gateway/
COPY auth-service/ auth-service/
COPY vehicle-service/ vehicle-service/
COPY reservation-service/ reservation-service/
COPY payment-service/ payment-service/

# Copy compiled Angular assets into static web resources
COPY --from=frontend-builder /app/frontend/dist/frontend-angular/browser/ monolith/src/main/resources/static/
COPY --from=frontend-builder /app/frontend/dist/frontend-angular/browser/ api-gateway/src/main/resources/static/

# Build monolith JAR (optimized for single-container cloud hosting)
RUN mvn -f monolith/pom.xml clean package -DskipTests

# Stage 3: Lightweight Runtime Container
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend-builder /app/monolith/target/*.jar app.jar

ENV PORT=8080
ENV JAVA_OPTS="-Xmx384m -Xss512k -XX:+UseG1GC"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
