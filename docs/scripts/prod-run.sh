#!/bin/bash

echo "Building and starting RiotApiProject in production mode..."

cd "$(dirname "$0")/.." || exit 1

echo "Checking Maven..."
mvn -version
if [ $? -ne 0 ]; then
    echo "Maven is not installed or not added to PATH."
    exit 1
fi

echo "Building project..."
mvn clean package
if [ $? -ne 0 ]; then
    echo "Build failed."
    exit 1
fi

echo "Starting application from jar..."
java -jar target/RiotApiPractice-1.0-SNAPSHOT.jar