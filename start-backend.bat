@echo off
set "JAVA_HOME=D:\Java\JDK_17"
set "PATH=D:\Java\JDK_17\bin;D:\Java\Maven\apache-maven-3.9.6-bin\apache-maven-3.9.6\bin;%PATH%"
cd /d "%~dp0"

if exist "backend\target\termiguard-backend-1.0.0.jar" (
    echo Starting TermaGuard Spring Boot Backend [Port 8080]...
    java -jar backend\target\termiguard-backend-1.0.0.jar
) else (
    echo Building and running with Maven...
    cd backend
    mvn spring-boot:run
)
pause
