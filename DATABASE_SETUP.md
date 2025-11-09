# Database Setup Instructions

## Option 1: Using Docker (Recommended)

If Docker is installed, run:

```bash
docker compose up -d
```

This will start PostgreSQL with the following credentials:
- Database: `invoiceme`
- Username: `invoiceme`
- Password: `invoiceme`
- Port: `5432`

## Option 2: Manual PostgreSQL Installation

1. Install PostgreSQL 15 or later
2. Create a database and user:

```sql
CREATE DATABASE invoiceme;
CREATE USER invoiceme WITH PASSWORD 'invoiceme';
GRANT ALL PRIVILEGES ON DATABASE invoiceme TO invoiceme;
```

## Verify Connection

You can test the connection using:

```bash
psql -U invoiceme -d invoiceme -h localhost
```

## Environment Variables

The application uses these environment variables (with defaults):
- `DB_USERNAME` (default: invoiceme)
- `DB_PASSWORD` (default: invoiceme)
- `JWT_SECRET` (default provided, change in production)

For production, set these in your environment or `.env` file.
