# Development Guide

Quick reference for developing InvoiceMe.

## Initial Setup

**First time only:**
```bash
# Install Git hooks for pre-commit validation
./setup-hooks.sh
```

This sets up automatic validation before each commit:
- Backend compilation check
- Frontend build check
- TypeScript type checking
- ESLint validation
- Prettier formatting check

## Quick Start

### Using the Dev Script (Recommended)

The easiest way to start all services:

```bash
# Start everything (database, backend, frontend)
./dev.sh start

# Check status
./dev.sh status

# Stop everything
./dev.sh stop

# Restart everything
./dev.sh restart
```

**What it does:**
- Starts PostgreSQL in Docker on port 5432
- Starts Spring Boot backend on port 8080
- Starts Vite frontend on port 5173
- Logs output to `backend.log` and `frontend.log`

**View logs:**
```bash
tail -f backend.log
tail -f frontend.log
```

### Manual Startup

If you prefer to run services individually:

**Terminal 1 - Database:**
```bash
docker compose up -d
```

**Terminal 2 - Backend:**
```bash
mvn spring-boot:run
```

**Terminal 3 - Frontend:**
```bash
cd invoice-me-frontend
npm run dev
```

## Hot Reloading

### Frontend (Vite)
✅ **Automatic** - Changes to React/TypeScript files reload instantly

### Backend (Spring Boot DevTools)
✅ **Automatic** - Changes trigger recompile and restart
- Your IDE (IntelliJ, VS Code) triggers recompiles automatically
- Or run `mvn compile` manually

### Database
⚠️ **Manual** - Schema changes require Flyway migrations
- Create migration file in `src/main/resources/db/migration/`
- Restart backend to apply

## Accessing Services

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080
- **Database**: localhost:5432
  - Database: `invoiceme`
  - Username: `invoiceme`
  - Password: `invoiceme`

## Database Management

### Connect to PostgreSQL
```bash
# Using docker exec
docker exec -it invoiceme-postgres psql -U invoiceme -d invoiceme

# Or using psql directly
psql -U invoiceme -d invoiceme -h localhost
```

### Common psql commands
```sql
\dt              -- List tables
\d table_name    -- Describe table
\q               -- Quit
```

### Reset Database
```bash
./dev.sh stop
docker volume rm invoice-me_postgres_data
./dev.sh start
```

## Common Development Tasks

### Run Backend Tests
```bash
mvn test
```

### Run Frontend Tests
```bash
cd invoice-me-frontend
npm test
```

### Build for Production
```bash
# Backend
mvn clean package -DskipTests
# Output: target/invoice-me-0.0.1-SNAPSHOT.jar

# Frontend
cd invoice-me-frontend
npm run build
# Output: dist/
```

### Check for Errors
```bash
# Backend compilation
mvn compile

# Frontend type checking
cd invoice-me-frontend
npm run build
```

## Troubleshooting

### Backend won't start
```bash
# Check if port 8080 is in use
lsof -i :8080

# Check database connection
docker ps | grep postgres

# View full backend logs
tail -f backend.log
```

### Frontend won't start
```bash
# Check if port 5173 is in use
lsof -i :5173

# Reinstall dependencies
cd invoice-me-frontend
rm -rf node_modules package-lock.json
npm install
```

### Database connection issues
```bash
# Restart PostgreSQL
docker compose restart

# Check PostgreSQL logs
docker logs invoiceme-postgres
```

### Kill stuck processes
```bash
# Kill backend
pkill -f "spring-boot:run"

# Kill frontend
pkill -f "vite"

# Stop database
docker compose down
```

## Project Structure Quick Reference

```
invoice-me/
├── src/main/java/com/invoiceme/     # Backend Java code
│   ├── domain/                      # Entities and business logic
│   ├── application/                 # Services and DTOs
│   └── infrastructure/              # Controllers, repositories, config
├── src/main/resources/
│   ├── application.yml              # Backend configuration
│   └── db/migration/                # Database migrations
├── invoice-me-frontend/src/         # Frontend React code
│   ├── models/                      # TypeScript types
│   ├── api/                         # API clients
│   ├── views/                       # Page components
│   └── components/                  # Reusable components
├── dev.sh                           # Development startup script
└── docker-compose.yml               # PostgreSQL configuration
```

## Git Workflow

```bash
# Create feature branch
git checkout -b feature/my-feature

# Make changes and commit
git add .
git commit -m "Add my feature"

# Push and create PR (when ready)
git push origin feature/my-feature
```

## Environment Variables

### Backend Setup (Required)

The backend automatically loads variables from `.env` file:

```bash
# 1. Copy the template
cp .env.example .env

# 2. Generate a JWT secret
openssl rand -base64 32

# 3. Edit .env and paste your generated secret
# Replace JWT_SECRET value with the generated one
```

**Important**: The application will fail to start if `JWT_SECRET` is not set. This is a security feature.

Key variables:
- `DATABASE_URL` - Database connection string (default works with Docker Compose)
- `JWT_SECRET` - **REQUIRED** - Generate with: `openssl rand -base64 32`

### Frontend Setup (Optional)

```bash
# Create invoice-me-frontend/.env
VITE_API_BASE_URL=http://localhost:8080
```

## IDE Setup

### IntelliJ IDEA
1. File → Open → Select `invoice-me` directory
2. Maven projects will auto-import
3. Run configuration: Maven → `spring-boot:run`

### VS Code
1. Install extensions:
   - Spring Boot Extension Pack
   - ESLint
   - Prettier
2. Open workspace: `invoice-me` directory
3. Terminal → Run `./dev.sh start`

## Next Steps

See [phase2-authentication.md](phase2-authentication.md) for what to implement next.
