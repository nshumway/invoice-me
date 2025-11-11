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

The application uses these environment variables:

### Local Development (Docker Compose)
Default `DATABASE_URL` in `.env.example` works automatically:
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/invoiceme?user=invoiceme&password=invoiceme
```

### Production (Neon, RDS, etc.)
Set `DATABASE_URL` with your database connection string:
```bash
# For Neon (add jdbc: prefix to Neon connection string)
DATABASE_URL=jdbc:postgresql://username:password@ep-xyz.region.aws.neon.tech/invoiceme?sslmode=require

# For other PostgreSQL providers
DATABASE_URL=jdbc:postgresql://host:5432/invoiceme?user=username&password=password
```

### Security
Also required:
- `JWT_SECRET` - Generate with: `openssl rand -base64 32`

For local development, copy `.env.example` to `.env` and update values.
