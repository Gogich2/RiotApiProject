@echo off
echo Building and starting RiotApiProject in production mode...

cd /d %~dp0\..

echo Checking Maven...
mvn -version
if errorlevel 1 (
    echo Maven is not installed or not added to PATH.
    pause
    exit /b 1
)

echo Building project...
mvn clean package
if errorlevel 1 (
    echo Build failed.
    pause
    exit /b 1
)

echo Starting application from jar...
java -jar target\RiotApiPractice-1.0-SNAPSHOT.jar

pause