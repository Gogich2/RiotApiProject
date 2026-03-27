#!/bin/bash

echo "Starting RiotApiProject in development mode..."

cd "$(dirname "$0")/.." || exit 1

echo "Checking Maven..."
mvn -version
if [ $? -ne 0 ]; then
    echo "Maven is not installed or not added to PATH."
    exit 1
fi

echo "Running Spring Boot application..."
mvn spring-boot:run