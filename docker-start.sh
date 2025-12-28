#!/bin/bash

# Docker Quick Start Script for ABAC System

set -e

echo "🚀 Starting ABAC Policy Management System with Docker..."
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running. Please start Docker and try again."
    exit 1
fi

# Build and start services
echo "📦 Building and starting services..."
docker-compose up -d --build

echo ""
echo "⏳ Waiting for services to be ready..."
sleep 10

# Check if services are running
echo ""
echo "📊 Service Status:"
docker-compose ps

echo ""
echo "✅ Services started successfully!"
echo ""
echo "📍 Application URL: http://localhost:8081"
echo "📍 Swagger UI: http://localhost:8081/swagger-ui.html"
echo "📍 API Health Check: http://localhost:8081/api/public"
echo ""
echo "📝 View logs: docker-compose logs -f"
echo "🛑 Stop services: docker-compose down"
echo ""

