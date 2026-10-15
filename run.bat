@echo off
where mvn >nul 2>nul
if errorlevel 1 (
  echo Apache Maven is required. Install Maven 3.8+ and ensure mvn is on PATH.
  pause
  exit /b 1
)
mvn clean package
if errorlevel 1 pause & exit /b 1
mvn exec:java
pause
