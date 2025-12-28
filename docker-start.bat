@echo off
REM Docker Quick Start Script for ABAC System (Windows)

echo 🚀 Starting ABAC Policy Management System with Docker...
echo.

REM Check if Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo ❌ Error: Docker is not running. Please start Docker and try again.
    exit /b 1
)

REM Build and start services
echo 📦 Building and starting services...
docker-compose up -d --build

echo.
echo ⏳ Waiting for services to be ready...
timeout /t 10 /nobreak >nul

REM Check if services are running
echo.
echo 📊 Service Status:
docker-compose ps

echo.
echo ✅ Services started successfully!
echo.
echo 📍 Application URL: http://localhost:8081
echo 📍 Swagger UI: http://localhost:8081/swagger-ui.html
echo 📍 API Health Check: http://localhost:8081/api/public
echo.
echo 📝 View logs: docker-compose logs -f
echo 🛑 Stop services: docker-compose down
echo.

pause

