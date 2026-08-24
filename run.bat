@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot"
set "MAVEN_BIN=C:\Users\gsushm\Downloads\apache-maven-3.9.6\bin\mvn.cmd"

echo ------------------------------------------
echo Starting Vehicle Rental System...
echo ------------------------------------------

cd /d "%~dp0"
call "%MAVEN_BIN%" spring-boot:run

pause
