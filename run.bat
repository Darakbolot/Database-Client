@echo off
javac -d out -sourcepath app/src/main/java app/src/main/java/DatabaseClient/main.java
if %ERRORLEVEL% NEQ 0 (
    echo [!] Compile error
    pause
    exit /b
)
java -cp "lib/mysql-connector-j-9.2.0.jar;out" DatabaseClient.main
pause
