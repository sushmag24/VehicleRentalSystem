@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
set "MAVEN_BIN=C:\Users\gsushm\Downloads\apache-maven-3.9.6\bin\mvn.cmd"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ===================================================
echo  Vehicle Rental System - Starting Monolith Version
echo ===================================================

echo.
echo [1/2] Building Monolith...
cd monolith
call "%MAVEN_BIN%" clean install -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Building monolith failed. Please check maven build errors.
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo [2/2] Starting Monolith Service on Port 8080...
call "%MAVEN_BIN%" spring-boot:run

pause
