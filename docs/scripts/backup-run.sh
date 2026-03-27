#!/bin/bash

echo "Starting RiotApiProject backup..."

cd "$(dirname "$0")/../.." || exit 1

mkdir -p backup/database
mkdir -p backup/config
mkdir -p backup/build
mkdir -p backup/logs

echo "Creating database backup..."
pg_dump -U riot_user -d riot_api_project > backup/database/riot_api_project_backup.sql

echo "Copying configuration..."
cp src/main/resources/application.properties backup/config/application.properties.bak

echo "Copying jar file..."
cp target/RiotApiPractice-1.0-SNAPSHOT.jar backup/build/RiotApiPractice-1.0-SNAPSHOT.jar.bak

if [ -d logs ]; then
    echo "Copying logs..."
    cp -r logs/* backup/logs/ 2>/dev/null
fi

echo "Backup completed."