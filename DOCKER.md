# Docker Setup Guide

This guide explains how to run the ABAC Policy Management System using Docker and Docker Compose.

---

## Prerequisites

- Docker Engine 20.10+
- Docker Compose 2.0+

---

## Quick Start

### 1. Build and Start Services

```bash
# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f

# View logs for specific service
docker-compose logs -f app
docker-compose logs -f postgres
```

### 2. Stop Services

```bash
# Stop services
docker-compose down

# Stop and remove volumes (⚠️ deletes database data)
docker-compose down -v
```

### 3. Rebuild After Code Changes

```bash
# Rebuild and restart
docker-compose up -d --build

# Force rebuild without cache
docker-compose build --no-cache
docker-compose up -d
```

---

## Services

### Application (app)
- **Port**: 8081
- **URL**: http://localhost:8081
- **Swagger UI**: http://localhost:8081/swagger-ui.html
- **Health Check**: http://localhost:8081/api/public

### PostgreSQL (postgres)
- **Port**: 5432
- **Database**: abacdb
- **Username**: postgres
- **Password**: Rin25052001 (change in docker-compose.yml)

---

## Environment Variables

You can customize the application by setting environment variables in `docker-compose.yml`:

```yaml
app:
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/abacdb
    SPRING_DATASOURCE_USERNAME: postgres
    SPRING_DATASOURCE_PASSWORD: your_password
    SPRING_JPA_HIBERNATE_DDL_AUTO: update
    SERVER_PORT: 8081
```

---

## Database Initialization

### Option 1: SQL Scripts (Recommended)

Place SQL initialization scripts in `src/main/resources/sql/` directory. They will be automatically executed when the PostgreSQL container starts for the first time.

Example:
```sql
-- src/main/resources/sql/01-init.sql
CREATE TABLE IF NOT EXISTS ...
```

### Option 2: Manual Initialization

1. Connect to the database:
```bash
docker-compose exec postgres psql -U postgres -d abacdb
```

2. Run your SQL scripts:
```sql
\i /path/to/initial_data.sql
```

---

## Development Workflow

### 1. Local Development with Docker

```bash
# Start only database
docker-compose up -d postgres

# Run application locally
./gradlew bootRun
```

### 2. Full Docker Development

```bash
# Start all services
docker-compose up -d

# View application logs
docker-compose logs -f app

# Execute commands in container
docker-compose exec app sh
```

### 3. Database Access

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres -d abacdb

# Backup database
docker-compose exec postgres pg_dump -U postgres abacdb > backup.sql

# Restore database
docker-compose exec -T postgres psql -U postgres abacdb < backup.sql
```

---

## Troubleshooting

### Application Won't Start

1. **Check if database is ready:**
```bash
docker-compose ps
docker-compose logs postgres
```

2. **Check application logs:**
```bash
docker-compose logs app
```

3. **Verify database connection:**
```bash
docker-compose exec app sh
# Inside container, test connection
```

### Database Connection Issues

1. **Check if postgres is healthy:**
```bash
docker-compose ps postgres
```

2. **Verify connection string:**
```bash
docker-compose exec app env | grep SPRING_DATASOURCE
```

3. **Test connection manually:**
```bash
docker-compose exec postgres psql -U postgres -d abacdb -c "SELECT 1;"
```

### Port Already in Use

If port 8081 or 5432 is already in use:

1. **Change ports in docker-compose.yml:**
```yaml
app:
  ports:
    - "8082:8081"  # Host:Container

postgres:
  ports:
    - "5433:5432"  # Host:Container
```

2. **Update application.yml** if running locally:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/abacdb
```

### Rebuild Issues

```bash
# Clean build
docker-compose down -v
docker system prune -f
docker-compose build --no-cache
docker-compose up -d
```

---

## Production Considerations

### 1. Security

- **Change default passwords** in docker-compose.yml
- **Use secrets** for sensitive data:
```yaml
secrets:
  db_password:
    file: ./secrets/db_password.txt

services:
  app:
    secrets:
      - db_password
```

### 2. Resource Limits

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 3. Health Checks

The Dockerfile includes a health check. Monitor with:
```bash
docker-compose ps
```

### 4. Logging

Configure logging driver:
```yaml
services:
  app:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

---

## Useful Commands

```bash
# View running containers
docker-compose ps

# View logs
docker-compose logs -f

# Restart a service
docker-compose restart app

# Stop all services
docker-compose stop

# Remove all containers and volumes
docker-compose down -v

# Execute command in container
docker-compose exec app sh
docker-compose exec postgres psql -U postgres -d abacdb

# View resource usage
docker stats

# Clean up unused resources
docker system prune -a
```

---

## Dockerfile Details

The Dockerfile uses a multi-stage build:

1. **Build Stage**: Uses Gradle to build the application
2. **Runtime Stage**: Uses slim JDK image with only the JAR file

This results in a smaller final image (~200MB vs ~800MB).

---

## Network

Services communicate via the `abac-network` bridge network:
- Application connects to database using service name: `postgres:5432`
- No need to expose PostgreSQL port externally (remove from ports if desired)

---

**Last Updated:** 2025

