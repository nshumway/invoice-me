# InvoiceMe - Project Structure

## Table of Contents
- [File Structure](#file-structure)
- [Architecture Layers](#architecture-layers)
- [Design Patterns](#design-patterns)
- [Transaction Flow](#transaction-flow)
- [Naming Conventions](#naming-conventions)
- [Key Principles](#key-principles)

---

## File Structure

```
invoice-me/
├── src/
│   ├── main/
│   │   ├── java/com/invoiceme/
│   │   │   ├── domain/
│   │   │   │   ├── common/
│   │   │   │   │   ├── BaseEntity.java
│   │   │   │   │   ├── AuditMetadata.java
│   │   │   │   │   └── exceptions/
│   │   │   │   │       ├── ValidationException.java
│   │   │   │   │       ├── NotFoundException.java
│   │   │   │   │       └── OptimisticLockException.java
│   │   │   │   ├── user/
│   │   │   │   │   └── User.java
│   │   │   │   ├── customer/
│   │   │   │   │   └── Customer.java
│   │   │   │   ├── invoice/
│   │   │   │   │   ├── Invoice.java
│   │   │   │   │   └── InvoiceStatus.java (enum)
│   │   │   │   ├── lineitem/
│   │   │   │   │   └── LineItem.java
│   │   │   │   └── payment/
│   │   │   │       ├── Payment.java
│   │   │   │       └── PaymentMethod.java (enum)
│   │   │   │
│   │   │   ├── application/
│   │   │   │   ├── common/
│   │   │   │   │   ├── ApiResponse.java
│   │   │   │   │   ├── UserContext.java
│   │   │   │   │   └── PaginationRequest.java
│   │   │   │   ├── user/
│   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   │   ├── CreateUserRequest.java
│   │   │   │   │   │   └── UserResponse.java
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   └── UserMapper.java
│   │   │   │   ├── customer/
│   │   │   │   │   ├── dto/
│   │   │   │   │   │   ├── CreateCustomerRequest.java
│   │   │   │   │   │   ├── UpdateCustomerRequest.java
│   │   │   │   │   │   ├── DeleteCustomerRequest.java
│   │   │   │   │   │   ├── CustomerResponse.java
│   │   │   │   │   │   └── CustomerListItemResponse.java
│   │   │   │   │   ├── CustomerService.java
│   │   │   │   │   └── CustomerMapper.java
│   │   │   │   ├── invoice/
│   │   │   │   │   ├── dto/
│   │   │   │   │   ├── InvoiceService.java
│   │   │   │   │   └── InvoiceMapper.java
│   │   │   │   ├── lineitem/
│   │   │   │   │   ├── dto/
│   │   │   │   │   ├── LineItemService.java
│   │   │   │   │   └── LineItemMapper.java
│   │   │   │   └── payment/
│   │   │   │       ├── dto/
│   │   │   │       ├── PaymentService.java
│   │   │   │       └── PaymentMapper.java
│   │   │   │
│   │   │   └── infrastructure/
│   │   │       ├── persistence/
│   │   │       │   ├── config/
│   │   │       │   │   └── JpaConfig.java
│   │   │       │   ├── UserRepository.java
│   │   │       │   ├── CustomerRepository.java
│   │   │       │   ├── InvoiceRepository.java
│   │   │       │   ├── LineItemRepository.java
│   │   │       │   └── PaymentRepository.java
│   │   │       ├── web/
│   │   │       │   ├── config/
│   │   │       │   │   └── WebConfig.java
│   │   │       │   ├── AuthController.java
│   │   │       │   ├── CustomerController.java
│   │   │       │   ├── InvoiceController.java
│   │   │       │   ├── LineItemController.java
│   │   │       │   └── PaymentController.java
│   │   │       └── security/
│   │   │           ├── SecurityConfig.java
│   │   │           ├── JwtTokenProvider.java
│   │   │           └── UserDetailsServiceImpl.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           ├── V1__create_users_table.sql
│   │           ├── V2__create_customers_table.sql
│   │           ├── V3__create_invoices_table.sql
│   │           ├── V4__create_line_items_table.sql
│   │           └── V5__create_payments_table.sql
│   │
│   └── test/
│       └── java/com/invoiceme/
│           ├── integration/
│           │   ├── CustomerIntegrationTest.java
│           │   ├── InvoiceIntegrationTest.java
│           │   └── PaymentFlowIntegrationTest.java
│           └── unit/
│               ├── CustomerServiceTest.java
│               └── InvoiceServiceTest.java
│
├── .claudeignore
├── .gitignore
├── pom.xml
├── README.md
├── data.md
├── structure.md
├── foundation.md
└── customers.md
```

---

## Architecture Layers

### Domain Layer (`domain/`)
**Purpose:** Pure business logic and domain entities

**Responsibilities:**
- Define rich domain entities with behavior
- Implement business rules and invariants
- Define domain exceptions
- NO dependencies on infrastructure concerns (DB, HTTP, etc.)

**Key Components:**
- `BaseEntity` - abstract parent for all entities
- Entity classes (Customer, Invoice, Payment, etc.)
- Enums (InvoiceStatus, PaymentMethod)
- Domain exceptions

**Methods on Entities:**
- `beforeCreate()` - validation before creation
- `create()` - entity creation logic
- `afterCreate()` - side effects after creation
- Similar pattern for `update`, `delete`, and custom operations

### Application Layer (`application/`)
**Purpose:** Orchestrate use cases and define API contracts

**Responsibilities:**
- Define DTOs (Data Transfer Objects) for API requests/responses
- Implement service classes that orchestrate business flows
- Map between DTOs and domain entities
- Coordinate transactions
- Handle cross-entity operations

**Key Components:**
- Request DTOs (e.g., `CreateCustomerRequest`)
- Response DTOs (e.g., `CustomerResponse`)
- Service classes with `@Transactional` methods
- Mapper classes (DTO ↔ Entity conversion)
- Common utilities (`ApiResponse`, `UserContext`)

### Infrastructure Layer (`infrastructure/`)
**Purpose:** Technical implementations and external integrations

**Responsibilities:**
- Database access via JPA repositories
- REST controllers (HTTP/API layer)
- Security and authentication
- Configuration

**Key Components:**
- Repositories (extend `JpaRepository`)
- Controllers (REST endpoints)
- Security configuration
- Database migration scripts

---

## Design Patterns

### CQRS (Command Query Responsibility Segregation)
- **Commands (Writes):** POST, PUT, DELETE operations
- **Queries (Reads):** GET operations
- Different DTOs for reads vs writes
- Separation allows optimization of each path

### Domain-Driven Design (DDD)
- Rich domain entities with behavior (not anemic models)
- Domain logic lives in entity methods
- Entities control their own lifecycle
- Clear bounded contexts per entity

### Vertical Slice Architecture
- Build features end-to-end before moving to next feature
- Each feature is complete: Controller → Service → Entity → Repository
- Enables parallel development and testing

### Repository Pattern
- Abstract data access behind repositories
- Repositories provide query methods for related entities
- Domain entities don't directly access the database

---

## Transaction Flow

### Standard User Operation Flow
```
1. HTTP Request → REST Controller
2. Controller validates basic request structure (@Valid)
3. Controller sets UserContext
4. Controller calls Service method
5. Service starts @Transactional boundary
6. Service loads entity from repository
7. Service calls entity.beforeX() - validation
8. Service calls entity.X() - main operation
9. Service calls repository.save() - queued, not committed
10. Service calls entity.afterX() - cascading updates
11. Entity.afterX() may call other services (MANDATORY propagation)
12. Service returns DTO
13. Transaction commits (or rolls back on exception)
14. Controller returns ApiResponse wrapper
```

### System-Initiated Cascade Flow
```
1. Entity.afterX() triggers cascade
2. Entity uses repository to get related entities
3. Entity creates system update request DTO
4. Entity calls OtherEntityService.systemUpdateX()
5. OtherEntityService joins parent transaction (MANDATORY)
6. OtherEntityService follows same before/update/after flow
7. OtherEntity.afterX() may cascade further
8. All changes commit together when parent transaction completes
```

### Atomicity Guarantee
- Single `@Transactional` on service method wraps entire operation
- All database writes are batched
- Nothing commits until transaction completes successfully
- Any exception triggers full rollback
- Cascading updates join the same transaction

---

## Naming Conventions

### Entities
- Singular nouns: `Customer`, `Invoice`, `Payment`
- Located in `domain/{entity}/`

### DTOs
- Pattern: `{Verb}{Entity}Request` for commands
- Pattern: `{Entity}Response` for single entity responses
- Pattern: `{Entity}ListItemResponse` for list items (subset of fields)
- Examples: `CreateCustomerRequest`, `CustomerResponse`

### Services
- Pattern: `{Entity}Service`
- Examples: `CustomerService`, `InvoiceService`

### Repositories
- Pattern: `{Entity}Repository`
- Extend `JpaRepository<Entity, UUID>`
- Custom query methods: `findByCustomerId()`, `getInvoicesForCustomer()`

### Controllers
- Pattern: `{Entity}Controller`
- REST paths: `/api/{entities}` (plural, lowercase)
- Examples: `/api/customers`, `/api/invoices`

### Mappers
- Pattern: `{Entity}Mapper`
- Methods: `toEntity()`, `toResponse()`, `toListItem()`

---

## Key Principles

### 1. All Operations Are Atomic
- Entire operation wrapped in single transaction
- Exception anywhere = full rollback
- No partial updates

### 2. User vs System Updates
- User updates: strict validation, limited fields
- System updates: relaxed validation, can update read-only fields
- Flag: `isSystemUpdate` boolean parameter
- Both track original user in `lastModifiedBy`

### 3. Optimistic Locking
- All entities have `version` field
- Client must send current version
- Concurrent modifications detected and rejected
- First write wins, second write fails with version mismatch

### 4. Soft Deletes
- `isDeleted` flag on BaseEntity
- Deleted entities excluded from queries
- Preserves audit trail and history
- Cascade deletes also soft

### 5. Audit Trail
- All entities track: createdBy, createdAt, lastModifiedBy, lastModifiedAt
- Deleted entities track: deletedBy, deletedAt
- Uses `UserContext` to capture current user

### 6. Read-Only Fields
- Some fields calculated: `Invoice.balance` (not stored in DB)
- Some fields system-managed: `Customer.totalOutstanding` (stored, but updated by system)
- Validation prevents user from setting read-only fields

### 7. Cascading Updates
- Each entity controls its own cascades in `afterX()` methods
- Cascades go through full service layer (before/update/after)
- Multi-phase cascading: Customer → Invoice → LineItem
- All cascades in same transaction

### 8. Bean Validation + Business Validation
- Bean Validation (@NotBlank, @Email): field-level constraints
- beforeX() methods: complex business rules and cross-entity validation

### 9. DTO Boundaries
- Never expose domain entities directly via API
- Always use DTOs for requests and responses
- Mappers handle conversion
- Allows API stability independent of domain model

### 10. Clean Architecture Dependencies
- Domain layer: no dependencies (pure business logic)
- Application layer: depends on domain
- Infrastructure layer: depends on domain and application
- Dependency direction: Infrastructure → Application → Domain
