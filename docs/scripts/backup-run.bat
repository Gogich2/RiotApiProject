@echo off
echo Starting RiotApiProject backup...

cd /d %~dp0\..\..

if not exist backup mkdir backup
if not exist backup\database mkdir backup\database
if not exist backup\config mkdir backup\config
if not exist backup\build mkdir backup\build
if not exist backup\logs mkdir backup\logs

echo Creating database backup...
pg_dump -U riot_user -d riot_api_project > backup\database\riot_api_project_backup.sql

echo Copying configuration...
copy src\main\resources\application.properties backup\config\application.properties.bak

echo Copying jar file...
copy target\RiotApiPractice-1.0-SNAPSHOT.jar backup\build\RiotApiPractice-1.0-SNAPSHOT.jar.bak

if exist logs (
    echo Copying logs...
    xcopy logs backup\logs\ /E /I /Y
)

echo Backup completed.
pause