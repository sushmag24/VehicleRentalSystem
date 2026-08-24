@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
set "MAVEN_BIN=C:\Users\gsushm\Downloads\apache-maven-3.9.6\bin\mvn.cmd"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ===================================================
echo  Vehicle Rental System - Starting Microservices Suite
echo ===================================================

echo.
echo [1/3] Building all microservices modules...
call "%MAVEN_BIN%" clean install -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Building project failed. Please check maven build errors.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/3] Starting Eureka Discovery Server...
start "Eureka Server [Port 8761]" cmd /k "call \"%MAVEN_BIN%\" -pl eureka-server spring-boot:run"
echo Waiting 12 seconds for Eureka Server to fully initialize...
timeout /t 12

echo.
echo [3/3] Starting microservices...
echo Starting Auth Service...
start "Auth Service [Port 8081]" cmd /k "call \"%MAVEN_BIN%\" -pl auth-service spring-boot:run"

echo Starting Vehicle Service...
start "Vehicle Service [Port 8082]" cmd /k "call \"%MAVEN_BIN%\" -pl vehicle-service spring-boot:run"

echo Starting Reservation Service...
start "Reservation Service [Port 8083]" cmd /k "call \"%MAVEN_BIN%\" -pl reservation-service spring-boot:run"

echo Starting Payment Service...
start "Payment Service [Port 8084]" cmd /k "call \"%MAVEN_BIN%\" -pl payment-service spring-boot:run"

echo Waiting 10 seconds for services to register with Eureka...
timeout /t 10

echo Starting API Gateway...
start "API Gateway [Port 8080]" cmd /k "call \"%MAVEN_BIN%\" -pl api-gateway spring-boot:run"

echo.
echo ===================================================
echo  All services have been launched!
echo  - Eureka Server: http://localhost:8761
echo  - API Gateway / Static Site: http://localhost:8080
echo ===================================================
echo.
pause
