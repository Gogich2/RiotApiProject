@echo off
set JAVA_HOME=C:\Users\egors\.jdks\valhalla-ea-23-valhalla+1-90
set PATH=%JAVA_HOME%\bin;%PATH%

echo Running Checkstyle before commit...

call mvnw.cmd checkstyle:check
if errorlevel 1 (
    echo Checkstyle failed. Commit aborted.
    exit /b 1
)

echo Checkstyle passed.
exit /b 0