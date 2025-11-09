# Deployment Guide

Guide for deploying InvoiceMe to production.

## Building for Production

### Backend (JAR File)

```bash
# Clean build
mvn clean package -DskipTests

# Output: target/invoice-me-0.0.1-SNAPSHOT.jar
```

The JAR file is fully self-contained and can run with just:
```bash
java -jar invoice-me-0.0.1-SNAPSHOT.jar
```

### Frontend (Static Files)

```bash
cd invoice-me-frontend

# Build optimized production bundle
npm run build

# Output: dist/
```

The `dist/` folder contains all static assets (HTML, CSS, JS) ready to serve.

## Deployment Options

### Option 1: Docker (Recommended)

**Coming Soon** - We'll add Dockerfiles for both backend and frontend.

### Option 2: Traditional Server

**Backend:**
1. Copy JAR to server
2. Set environment variables
3. Run with `java -jar`

**Frontend:**
1. Copy `dist/` folder to web server
2. Configure nginx/Apache to serve files
3. Set API base URL

**Database:**
1. PostgreSQL 15+ instance
2. Create `invoiceme` database
3. Flyway migrations run automatically on backend startup

### Option 3: Cloud Platforms

#### Heroku
- Deploy JAR using Heroku Java buildpack
- Deploy frontend to Heroku static sites or separate CDN
- Add PostgreSQL addon

#### AWS
- **Backend**: Elastic Beanstalk or ECS
- **Frontend**: S3 + CloudFront
- **Database**: RDS PostgreSQL

#### Azure
- **Backend**: App Service
- **Frontend**: Static Web Apps
- **Database**: Azure Database for PostgreSQL

#### Vercel/Netlify (Frontend only)
- Connect GitHub repo
- Auto-deploy on push
- Configure build command: `cd invoice-me-frontend && npm run build`
- Publish directory: `invoice-me-frontend/dist`

## Environment Variables

### Backend

```bash
# Database
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
DATABASE_URL=jdbc:postgresql://your-host:5432/invoiceme

# Security
JWT_SECRET=your-256-bit-secret-key

# Optional
SPRING_PROFILES_ACTIVE=prod
```

### Frontend

Create `invoice-me-frontend/.env.production`:
```bash
VITE_API_BASE_URL=https://api.yourdomain.com
```

## Pre-Deployment Checklist

- [ ] All tests passing
- [ ] Environment variables configured
- [ ] Database migrations tested
- [ ] JWT secret changed from default
- [ ] CORS configured for production domain
- [ ] SSL/TLS certificates installed
- [ ] Backup strategy in place

## Production Configuration

### Backend application.yml

Create `application-prod.yml`:
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jpa:
    show-sql: false  # Disable SQL logging in production
    hibernate:
      ddl-auto: validate

  flyway:
    enabled: true
    baseline-on-migrate: true

server:
  port: ${PORT:8080}

jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000

logging:
  level:
    root: INFO
    com.invoiceme: INFO
```

Run with: `java -jar invoice-me.jar --spring.profiles.active=prod`

### Frontend Nginx Configuration

```nginx
server {
    listen 80;
    server_name yourdomain.com;
    root /var/www/invoice-me/dist;
    index index.html;

    # Serve static files
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # Proxy API requests to backend
    location /api {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
```

## Database Backup

### Automated Backup Script

```bash
#!/bin/bash
# backup-db.sh

DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backups"
DB_NAME="invoiceme"

# Create backup
pg_dump -U invoiceme $DB_NAME | gzip > $BACKUP_DIR/invoiceme_$DATE.sql.gz

# Keep only last 7 days
find $BACKUP_DIR -name "invoiceme_*.sql.gz" -mtime +7 -delete

echo "Backup completed: invoiceme_$DATE.sql.gz"
```

Add to cron:
```bash
# Daily backup at 2 AM
0 2 * * * /path/to/backup-db.sh
```

## Monitoring

### Health Check Endpoints

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Check database connection
curl http://localhost:8080/actuator/health/db
```

### Application Logs

```bash
# Backend logs
tail -f /var/log/invoiceme/application.log

# Or with systemd
journalctl -u invoiceme -f
```

## Scaling

### Horizontal Scaling (Multiple Backend Instances)

1. Use load balancer (nginx, HAProxy, AWS ALB)
2. Configure session storage (Redis, DB)
3. Use PostgreSQL connection pooling

### Database Scaling

1. Read replicas for read-heavy operations
2. Connection pooling (HikariCP already configured)
3. Optimize slow queries

## Security Checklist

- [ ] HTTPS/TLS enabled
- [ ] JWT secret is strong and unique
- [ ] Database credentials rotated regularly
- [ ] CORS configured for specific domains only
- [ ] SQL injection protection (JPA parameterized queries)
- [ ] XSS protection enabled
- [ ] CSRF protection enabled (Spring Security default)
- [ ] Rate limiting configured
- [ ] Firewall rules applied
- [ ] Regular security updates

## Rollback Procedure

If deployment fails:

1. **Backend:**
   ```bash
   # Stop current version
   systemctl stop invoiceme

   # Restore previous JAR
   cp /backups/invoice-me-previous.jar /opt/invoiceme/invoice-me.jar

   # Start service
   systemctl start invoiceme
   ```

2. **Frontend:**
   ```bash
   # Restore previous dist/
   rm -rf /var/www/invoice-me/dist
   cp -r /backups/dist-previous /var/www/invoice-me/dist
   ```

3. **Database:**
   ```bash
   # Restore from backup (if migrations were run)
   gunzip < /backups/invoiceme_YYYYMMDD.sql.gz | psql -U invoiceme invoiceme
   ```

## CI/CD Pipeline (Future)

Coming soon: GitHub Actions workflow for automated testing and deployment.

## Support

For deployment issues, check:
- Application logs
- Database connectivity
- Environment variables
- Network/firewall rules
- SSL certificates
