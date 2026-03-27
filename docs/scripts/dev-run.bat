@echo off
echo Starting RiotApiProject in development mode...

cd /d %~dp0\..

echo Checking Maven...
mvn -version
if errorlevel 1 (
    echo Maven is not installed or not added to PATH.
    pause
    exit /b 1
)

echo Running Spring Boot application...
mvn spring-boot:run

pause