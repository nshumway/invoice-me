# Phase 1: Foundation & Infrastructure

## Overview
This phase establishes the foundational infrastructure for both backend and frontend, including project setup, database configuration, base entities, and core utilities. No business features are implemented yet - this is pure infrastructure.

**Goal:** Have a running backend API and frontend UI that can communicate, with all common infrastructure in place.

**Duration Estimate:** 1-2 days

---

## User Stories

### US-1: Set up Spring Boot backend project
**As a developer, I need to set up the Spring Boot backend project so that I have the basic application structure**

**Acceptance Criteria:**
- Maven project created with proper groupId/artifactId
- Spring Boot 3.x starter dependencies added
- Main application class exists and runs
- Application starts successfully on port 8080
- Health check endpoint responds

**Implementation Details:**

1. Create Maven project structure:
```
invoice-me/
├── src/
│   ├── main/
│   │   ├── java/com/invoiceme/
│   │   │   └── InvoiceMeApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   └── test/
│       └── java/com/invoiceme/
├── pom.xml
└── README.md
```

2. Add dependencies to `pom.xml`:
```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>

    <!-- Flyway for migrations -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>

    <!-- JWT for authentication -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

3. Create `InvoiceMeApplication.java`:
```java
package com.invoiceme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InvoiceMeApplication {
    public static void main(String[] args) {
        SpringApplication.run(InvoiceMeApplication.class, args);
    }
}
```

4. Create basic `application.yml`:
```yaml
spring:
  application:
    name: invoice-me

server:
  port: 8080
```

**Testing:**
- Run `mvn clean install`
- Run `mvn spring-boot:run`
- Verify application starts without errors
- Access http://localhost:8080 (should get 401 from Spring Security)

---

### US-2: Set up React frontend project
**As a developer, I need to set up the React frontend project so that I have the basic UI structure**

**Acceptance Criteria:**
- Vite + React + TypeScript project created
- Tailwind CSS configured and working
- Basic folder structure in place
- Development server runs on port 5173
- Can render "Hello World" component

**Implementation Details:**

1. Create Vite project:
```bash
npm create vite@latest invoice-me-frontend -- --template react-ts
cd invoice-me-frontend
npm install
```

2. Install Tailwind CSS:
```bash
npm install -D tailwindcss postcss autoprefixer @tailwindcss/forms
npx tailwindcss init -p
```

3. Configure `tailwind.config.js`:
```javascript
module.exports = {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#eff6ff',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
        },
        danger: {
          500: '#ef4444',
          600: '#dc2626',
        },
      },
    },
  },
  plugins: [require('@tailwindcss/forms')],
};
```

4. Update `src/index.css`:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

5. Create folder structure:
```
src/
├── main.tsx
├── App.tsx
├── index.css
├── models/
├── api/
├── viewmodels/
├── views/
├── components/
│   ├── layout/
│   └── common/
├── context/
├── hooks/
└── utils/
```

6. Update `src/App.tsx`:
```typescript
function App() {
  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto p-6">
        <h1 className="text-3xl font-bold text-blue-600">InvoiceMe</h1>
        <p className="text-gray-600 mt-2">Foundation setup complete</p>
      </div>
    </div>
  );
}

export default App;
```

**Testing:**
- Run `npm run dev`
- Access http://localhost:5173
- Verify Tailwind styles are applied (blue heading, gray background)

---

### US-3: Configure PostgreSQL database
**As a developer, I need to configure PostgreSQL database so that the application can persist data**

**Acceptance Criteria:**
- PostgreSQL running locally
- Database `invoiceme` created
- Backend can connect to database
- Connection pool configured

**Implementation Details:**

1. Install PostgreSQL locally (or use Docker):
```bash
# Using Docker
docker run --name invoiceme-postgres \
  -e POSTGRES_DB=invoiceme \
  -e POSTGRES_USER=invoiceme \
  -e POSTGRES_PASSWORD=invoiceme \
  -p 5432:5432 \
  -d postgres:15
```

2. Update `application.yml`:
```yaml
spring:
  application:
    name: invoice-me

  datasource:
    url: jdbc:postgresql://localhost:5432/invoiceme
    username: ${DB_USERNAME:invoiceme}
    password: ${DB_PASSWORD:invoiceme}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # Flyway manages schema
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        default_schema: public
        jdbc:
          time_zone: UTC

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

server:
  port: 8080

jwt:
  secret: ${JWT_SECRET:your-secret-key-min-256-bits-change-in-production}
  expiration: 86400000  # 24 hours in milliseconds
```

3. Create JPA configuration:
```java
package com.invoiceme.infrastructure.persistence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.invoiceme.infrastructure.persistence")
@EnableJpaAuditing
@EnableTransactionManagement
public class JpaConfig {
}
```

**Testing:**
- Verify PostgreSQL is running: `psql -U invoiceme -d invoiceme -h localhost`
- Run Spring Boot application
- Check logs for successful database connection
- Verify Flyway baseline migration runs

---

### US-4: Implement BaseEntity
**As a developer, I need to implement BaseEntity so that all entities share common audit fields**

**Acceptance Criteria:**
- BaseEntity class with id, audit fields, version, soft delete
- @PrePersist and @PreUpdate hooks work
- Soft delete method implemented
- All fields properly annotated for JPA

**Implementation Details:**

Create `src/main/java/com/invoiceme/domain/common/BaseEntity.java`:

```java
package com.invoiceme.domain.common;

import com.invoiceme.application.common.UserContext;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false, updatable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private Instant lastModifiedAt;

    @Column(nullable = false)
    private UUID lastModifiedBy;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    private Instant deletedAt;

    private UUID deletedBy;

    // === Lifecycle Methods ===

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        UUID currentUser = UserContext.getCurrentUser();

        this.createdAt = now;
        this.createdBy = currentUser;
        this.lastModifiedAt = now;
        this.lastModifiedBy = currentUser;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedAt = Instant.now();
        this.lastModifiedBy = UserContext.getCurrentUser();
    }

    // === Soft Delete ===

    public void markAsDeleted() {
        this.isDeleted = true;
        this.deletedAt = Instant.now();
        this.deletedBy = UserContext.getCurrentUser();
    }

    // === Getters and Setters ===

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public UUID getLastModifiedBy() {
        return lastModifiedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public UUID getDeletedBy() {
        return deletedBy;
    }
}
```

**Testing:**
- Will be tested when first entity (User) is created
- Verify @PrePersist sets audit fields on create
- Verify @PreUpdate updates lastModifiedAt/By
- Verify markAsDeleted() sets soft delete fields

---

### US-5: Implement UserContext
**As a developer, I need UserContext for tracking current user so that audit fields are populated automatically**

**Acceptance Criteria:**
- ThreadLocal-based storage for current user ID
- setCurrentUser(), getCurrentUser(), clear() methods
- Throws exception if no user context available
- Thread-safe implementation

**Implementation Details:**

Create `src/main/java/com/invoiceme/application/common/UserContext.java`:

```java
package com.invoiceme.application.common;

import java.util.UUID;

/**
 * ThreadLocal storage for current user ID throughout request lifecycle.
 * Used for audit trail (createdBy, lastModifiedBy) even during cascading updates.
 */
public class UserContext {

    private static final ThreadLocal<UUID> currentUserId = new ThreadLocal<>();

    public static void setCurrentUser(UUID userId) {
        currentUserId.set(userId);
    }

    public static UUID getCurrentUser() {
        UUID userId = currentUserId.get();
        if (userId == null) {
            throw new IllegalStateException("No user context available");
        }
        return userId;
    }

    public static void clear() {
        currentUserId.remove();
    }
}
```

**Testing:**
- Unit test setting/getting user ID
- Unit test clear() removes value
- Unit test getCurrentUser() throws when not set
- Integration test with JWT filter will verify it works in request lifecycle

---

### US-6: Implement domain exception classes
**As a developer, I need domain exception classes so that I can handle validation and business errors**

**Acceptance Criteria:**
- ValidationException for business rule violations
- NotFoundException for missing entities
- OptimisticLockException for version conflicts
- All extend RuntimeException

**Implementation Details:**

Create `src/main/java/com/invoiceme/domain/common/exceptions/`:

```java
package com.invoiceme.domain.common.exceptions;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
```

```java
package com.invoiceme.domain.common.exceptions;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
```

```java
package com.invoiceme.domain.common.exceptions;

public class OptimisticLockException extends RuntimeException {
    public OptimisticLockException(String message) {
        super(message);
    }
}
```

**Testing:**
- Unit tests verify exceptions can be thrown and caught
- Integration tests verify GlobalExceptionHandler catches them

---

### US-7: Implement GlobalExceptionHandler
**As a developer, I need GlobalExceptionHandler so that API errors are returned in a consistent format**

**Acceptance Criteria:**
- Catches ValidationException → 400 Bad Request
- Catches NotFoundException → 404 Not Found
- Catches OptimisticLockException → 409 Conflict
- Catches MethodArgumentNotValidException → 400 Bad Request
- Catches generic Exception → 500 Internal Server Error
- Returns ApiResponse wrapper for all errors

**Implementation Details:**

Create `src/main/java/com/invoiceme/infrastructure/web/config/GlobalExceptionHandler.java`:

```java
package com.invoiceme.infrastructure.web.config;

import com.invoiceme.application.common.ApiResponse;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.OptimisticLockException;
import com.invoiceme.domain.common.exceptions.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(ValidationException ex) {
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFoundException(NotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLockException(OptimisticLockException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleBeanValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.failure(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("An unexpected error occurred: " + ex.getMessage()));
    }
}
```

**Testing:**
- Create test controller that throws each exception type
- Verify correct HTTP status code returned
- Verify ApiResponse structure in response body
- Verify error message included

---

### US-8: Implement ApiResponse wrapper
**As a developer, I need ApiResponse wrapper so that all API responses follow the same structure**

**Acceptance Criteria:**
- Generic ApiResponse<T> class
- success, message, data fields
- Factory methods: success(data), success(message, data), failure(message)
- Used by all controllers and exception handler

**Implementation Details:**

Create `src/main/java/com/invoiceme/application/common/ApiResponse.java`:

```java
package com.invoiceme.application.common;

/**
 * Standard API response wrapper for all endpoints.
 */
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    // === Constructors ===

    private ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // === Factory Methods ===

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> failure(String message) {
        return new ApiResponse<>(false, message, null);
    }

    // === Getters ===

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
```

**Testing:**
- Unit test factory methods create correct structure
- Verify success=true for success()
- Verify success=false for failure()
- Verify message and data set correctly

---

## Phase 1 Completion Checklist

- [ ] Backend runs successfully on port 8080
- [ ] Frontend runs successfully on port 5173
- [ ] PostgreSQL database connected
- [ ] BaseEntity compiles and has all fields
- [ ] UserContext provides thread-local user ID
- [ ] Domain exceptions defined
- [ ] GlobalExceptionHandler configured
- [ ] ApiResponse wrapper implemented
- [ ] All code compiles without errors
- [ ] Basic unit tests pass

## Next Phase

Phase 2: Authentication (User entity, JWT, Login/Signup endpoints and UI)
