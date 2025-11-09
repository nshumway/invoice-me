# Phase 4: Invoice CRUD (Full Stack)

## Table of Contents
- [Overview](#overview)
- [Architecture Context](#architecture-context)
- [Data Model Reference](#data-model-reference)
- [Phase 4A: Create Invoice](#phase-4a-create-invoice)
  - [US-33: Invoice Entity & Database](#us-33-invoice-entity--database)
  - [US-34: Invoice DTOs & Mapper](#us-34-invoice-dtos--mapper)
  - [US-35: Invoice Service - Create](#us-35-invoice-service---create)
  - [US-36: Invoice Controller - Create](#us-36-invoice-controller---create)
  - [US-37: Frontend - Invoice Form (Create)](#us-37-frontend---invoice-form-create)
- [Phase 4B: List Invoices](#phase-4b-list-invoices)
  - [US-38: Invoice Service - List](#us-38-invoice-service---list)
  - [US-39: Invoice Controller - List](#us-39-invoice-controller---list)
  - [US-40: Frontend - Invoice List View](#us-40-frontend---invoice-list-view)
- [Phase 4C: View Invoice Detail](#phase-4c-view-invoice-detail)
  - [US-41: Invoice Service - Get by ID](#us-41-invoice-service---get-by-id)
  - [US-42: Invoice Controller - Get by ID](#us-42-invoice-controller---get-by-id)
  - [US-43: Frontend - Invoice Detail View](#us-43-frontend---invoice-detail-view)
- [Phase 4D: Update Invoice](#phase-4d-update-invoice)
  - [US-44: Invoice Service - Update](#us-44-invoice-service---update)
  - [US-45: Invoice Controller - Update](#us-45-invoice-controller---update)
  - [US-46: Frontend - Invoice Form (Edit)](#us-46-frontend---invoice-form-edit)
- [Phase 4E: Mark Invoice as Sent](#phase-4e-mark-invoice-as-sent)
  - [US-47: Invoice Service - Mark as Sent](#us-47-invoice-service---mark-as-sent)
  - [US-48: Invoice Controller - Mark as Sent](#us-48-invoice-controller---mark-as-sent)
  - [US-49: Frontend - Send Invoice Button](#us-49-frontend---send-invoice-button)
- [Phase 4F: Delete Invoice](#phase-4f-delete-invoice)
  - [US-50: Invoice Service - Delete](#us-50-invoice-service---delete)
  - [US-51: Invoice Controller - Delete](#us-51-invoice-controller---delete)
  - [US-52: Frontend - Delete Invoice](#us-52-frontend---delete-invoice)
- [Completion Checklist](#completion-checklist)

---

## Overview

This phase implements complete Invoice CRUD functionality with status management and cascading updates to customer invoice counts. Invoices follow a one-way status progression: DRAFT → SENT → PAID.

**Goal:** Users can create, list, view, update, and delete invoices. Invoices can be marked as SENT, which locks them from further user edits and updates customer statistics.

**Duration Estimate:** 3-4 days

---

## Architecture Context

### Domain-Driven Design Patterns

**Invoice Entity Responsibilities:**
- Validates business rules (invoice number uniqueness, status transitions)
- Manages lifecycle: beforeCreate/create/afterCreate, beforeUpdate/update/afterUpdate, etc.
- Publishes domain events for state changes that other domains need to know about
- Enforces immutability rules (customerId cannot change, SENT invoices cannot be user-edited)

**Status Lifecycle:**
```
DRAFT ──────────────> SENT ──────────────> PAID
         ^                     ^
         |                     |
   user edits allowed    user edits blocked
   can be deleted        cannot be deleted
                         payments can be recorded
```

**Key Patterns:**
- **Optimistic Locking:** Version field prevents concurrent modification conflicts
- **Soft Deletes:** isDeleted flag, preserves audit trail
- **Audit Trail:** createdBy, lastModifiedBy via UserContext
- **User vs System Updates:** isSystemUpdate flag distinguishes user edits from cascades
- **Invoice Number Generation:** Auto-generated unique format: INV-{YYYY}-{sequential 5-digit}
- **Denormalized customerName:** Copied from Customer.companyName, updated via cascade when customer changes

### Transaction Flow

**Create Invoice Flow:**
```
1. Controller receives CreateInvoiceRequest
2. Controller sets UserContext
3. Controller calls InvoiceService.createInvoice()
4. Service loads Customer entity (validation)
5. Service creates Invoice entity
6. Invoice.beforeCreate() validates business rules
7. Invoice.create() sets all fields, generates invoiceNumber if not provided
8. Service saves Invoice (queued, not committed)
9. Invoice.afterCreate() publishes InvoiceCreatedEvent
10. ApplicationEventPublisher dispatches event (synchronous, within transaction)
11. CustomerService listens for InvoiceCreatedEvent and increments draftInvoiceCount
12. Transaction commits (or rolls back if any step fails)
13. Service returns InvoiceResponse
```

**Key Architecture Note: Domain Events vs Direct Coupling**
- Invoices do NOT directly call CustomerService methods
- Instead, Invoice publishes domain events (InvoiceCreatedEvent, InvoiceStatusChangedEvent, etc.)
- CustomerService subscribes to these events via @EventListener
- This maintains domain boundary separation and prevents circular dependencies

**Mark as Sent Flow:**
```
1. Controller receives POST /api/invoices/:id/send
2. Controller calls InvoiceService.markInvoiceAsSent()
3. Service loads Invoice entity
4. Invoice.beforeSend() validates (status=DRAFT, total>0)
5. Invoice.send() sets status=SENT, invoiceDate=now
6. Service saves Invoice
7. Invoice.afterSend() publishes InvoiceStatusChangedEvent (DRAFT → SENT)
8. CustomerService listens for InvoiceStatusChangedEvent and updates:
   - Decrement draftInvoiceCount
   - Increment sentInvoiceCount
   - Add invoice.total to totalOutstanding
9. Transaction commits
```

**Event-Driven Architecture for Cross-Domain Updates:**
- Customer domain publishes CustomerNameChangedEvent when companyName changes
- InvoiceService subscribes to CustomerNameChangedEvent via @EventListener
- InvoiceService updates denormalized customerName field on all invoices for that customer
- System updates use @Transactional(propagation = MANDATORY) to participate in parent transaction
- Events are published synchronously within the same transaction for consistency

---

## Testing & Documentation Requirements

All features in this phase must include comprehensive testing and API documentation:

### OpenAPI Documentation
- **Required for ALL endpoints**: Every REST API endpoint must have complete OpenAPI 3.0 annotations
- Use Spring `@Operation`, `@ApiResponse`, `@Schema` annotations
- Document all request/response models with descriptions and examples
- Include error responses (400, 404, 409, etc.)
- Example:
```java
@Operation(summary = "Create a new invoice",
           description = "Creates a new invoice in DRAFT status for the specified customer")
@ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "Invoice created successfully",
                 content = @Content(schema = @Schema(implementation = InvoiceResponse.class))),
    @ApiResponse(responseCode = "400", description = "Invalid request data"),
    @ApiResponse(responseCode = "404", description = "Customer not found")
})
@PostMapping
public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(@Valid @RequestBody CreateInvoiceRequest request)
```

### Backend Testing
- **Unit Tests**: Service layer, entity lifecycle methods, mappers
- **Integration Tests**: Full transaction flows with in-memory database
- **Minimum Coverage**: 80% code coverage for service layer

### Frontend Testing

#### 1. ViewModel Tests (Vitest)
- **Required for ALL ViewModels**
- Test state management, form validation, mutation handling
- Mock API calls using MSW (Mock Service Worker)
- Test error handling and loading states
- Example coverage:
  - Initial state
  - Form field updates
  - Validation logic
  - Successful API calls
  - API error handling
  - Edge cases (optimistic locking, etc.)

#### 2. React Component Tests (Vitest + React Testing Library)
- **Required for ALL Views and reusable components**
- Test rendering, user interactions, conditional logic
- Use `@testing-library/react` and `@testing-library/user-event`
- Test accessibility (ARIA labels, keyboard navigation)
- Example coverage:
  - Component renders correctly
  - User can interact with form fields
  - Buttons are enabled/disabled appropriately
  - Error messages display correctly
  - Loading states work properly

#### 3. End-to-End Tests (Playwright)
- **Required for ALL user stories**
- Test complete user workflows from browser perspective
- Run against development server
- Test happy paths and critical error scenarios
- Example flows:
  - Create invoice flow: Navigate to form → Fill fields → Submit → Verify detail page
  - List invoices flow: Navigate to list → Verify invoices display → Search/filter
  - Update invoice flow: Navigate to detail → Click edit → Modify → Submit → Verify changes
  - Delete invoice flow: Navigate to detail → Click delete → Confirm → Verify redirect

### Testing Structure Example

```
Backend:
src/test/java/.../invoice/
  ├── InvoiceServiceTest.java          // Unit tests
  ├── InvoiceControllerIntegrationTest.java
  └── InvoiceMapperTest.java

Frontend:
src/__tests__/
  ├── viewmodels/
  │   └── invoices/
  │       ├── InvoiceFormViewModel.test.ts
  │       ├── InvoiceListViewModel.test.ts
  │       └── InvoiceDetailViewModel.test.ts
  ├── views/
  │   └── invoices/
  │       ├── InvoiceFormView.test.tsx
  │       ├── InvoiceListView.test.tsx
  │       └── InvoiceDetailView.test.tsx
  └── e2e/
      └── invoices/
          ├── create-invoice.spec.ts
          ├── list-invoices.spec.ts
          ├── update-invoice.spec.ts
          └── delete-invoice.spec.ts
```

---

## Data Model Reference

### Invoice Entity Fields

| Field | Type | Description | Notes |
|-------|------|-------------|-------|
| `id` | UUID | Primary identifier | Inherited from BaseEntity |
| `customerId` | UUID | Foreign key to Customer | **Immutable after creation** |
| `invoiceNumber` | String | Unique invoice number | Auto-generated if not provided: INV-{YYYY}-{nnnnn} |
| `notes` | String | Invoice notes/terms | Optional, user-editable while DRAFT |

### Invoice Read-Only Fields

| Field | Type | Description | Computed/System-Managed |
|-------|------|-------------|-------------------------|
| `invoiceDate` | Instant | UTC timestamp when sent | Set when marked as SENT |
| `status` | Enum | DRAFT, SENT, or PAID | Transitions via specific operations |
| `customerName` | String | Customer company name | Denormalized, updated via cascade |
| `total` | BigDecimal | Sum of line items | Recalculated by LineItem operations |
| `amountPaid` | BigDecimal | Sum of payments | Recalculated by Payment operations |

### Invoice Calculated Fields (Not Stored)

| Field | Type | Calculation | Returned By |
|-------|------|-------------|-------------|
| `balance` | BigDecimal | total - amountPaid | GetInvoiceById, ListInvoices |

### Database Indexes

```sql
-- Unique invoice number for non-deleted invoices
CREATE UNIQUE INDEX idx_invoices_invoice_number_active
    ON invoices(invoice_number)
    WHERE is_deleted = FALSE;

-- Filter by status
CREATE INDEX idx_invoices_status
    ON invoices(status)
    WHERE is_deleted = FALSE;

-- Filter by customer
CREATE INDEX idx_invoices_customer_id
    ON invoices(customer_id)
    WHERE is_deleted = FALSE;

-- Sort by invoice date
CREATE INDEX idx_invoices_invoice_date
    ON invoices(invoice_date)
    WHERE is_deleted = FALSE;
```

### Validation Rules

**Create:**
- customerId: required, must reference existing customer
- invoiceNumber: optional, if provided must be unique, if not provided auto-generate
- notes: optional

**Update:**
- status must be DRAFT (unless isSystemUpdate=true)
- customerId: cannot be changed
- invoiceNumber: optional, if provided must be unique
- notes: optional

**Mark as Sent:**
- Current status must be DRAFT
- total must be > 0

**Delete:**
- status must be DRAFT or PAID (SENT invoices cannot be deleted)

---

## Phase 4A: Create Invoice

### US-33: Invoice Entity & Database

**As a developer, I need Invoice entity and database table so that invoices can be stored**

**Acceptance Criteria:**
- Flyway migration creates invoices table with all fields and indexes
- Invoice entity extends BaseEntity
- InvoiceStatus enum defined (DRAFT, SENT, PAID)
- beforeCreate/create/afterCreate methods implemented
- Invoice number auto-generation logic works
- Repository with basic CRUD methods
- Unit tests verify entity behavior

**Implementation Scaffolding:**

```sql
-- V3__create_invoices_table.sql
CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id),
    invoice_number VARCHAR(50) NOT NULL,
    notes TEXT,

    -- Read-only fields
    invoice_date TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    customer_name VARCHAR(255) NOT NULL,
    total DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    amount_paid DECIMAL(19, 2) NOT NULL DEFAULT 0.00,

    -- BaseEntity fields (audit, version, soft delete)
    created_at TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    last_modified_by UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

-- Create indexes as specified above
```

```java
// InvoiceStatus.java
public enum InvoiceStatus {
    DRAFT,
    SENT,
    PAID
}

// Invoice.java
@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Read-only fields
    private Instant invoiceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    // === CREATE OPERATION ===

    public void beforeCreate(CreateInvoiceRequest request,
                            Customer customer,
                            InvoiceRepository invoiceRepository) {
        // Validate customerId exists (already loaded)
        if (customer == null) {
            throw new NotFoundException("Customer not found");
        }

        // Validate invoice number uniqueness if provided
        if (request.getInvoiceNumber() != null) {
            if (invoiceRepository.existsByInvoiceNumberAndIsDeletedFalse(request.getInvoiceNumber())) {
                throw new ValidationException("Invoice number already exists");
            }
        }
    }

    public void create(CreateInvoiceRequest request, Customer customer) {
        this.customerId = request.getCustomerId();
        this.notes = request.getNotes();

        // Generate invoice number if not provided
        if (request.getInvoiceNumber() != null) {
            this.invoiceNumber = request.getInvoiceNumber();
        } else {
            this.invoiceNumber = generateInvoiceNumber();
        }

        // Set read-only fields
        this.status = InvoiceStatus.DRAFT;
        this.customerName = customer.getCompanyName();
        this.total = BigDecimal.ZERO;
        this.amountPaid = BigDecimal.ZERO;
    }

    public void afterCreate(ApplicationEventPublisher eventPublisher) {
        // Publish domain event: Invoice created
        eventPublisher.publishEvent(new InvoiceCreatedEvent(
            this.id,
            this.customerId,
            InvoiceStatus.DRAFT
        ));
    }

    private String generateInvoiceNumber() {
        // Format: INV-{YYYY}-{sequential 5-digit}
        // Implementation: Query for max invoice number for current year, increment
        // Example: INV-2025-00001, INV-2025-00002, etc.
    }

    // Getters/Setters...
}

// InvoiceRepository.java
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByIdAndIsDeletedFalse(UUID id);
    boolean existsByInvoiceNumberAndIsDeletedFalse(String invoiceNumber);
    List<Invoice> findAllByIsDeletedFalseOrderByInvoiceDateDesc();
    List<Invoice> findAllByCustomerIdAndIsDeletedFalse(UUID customerId);
}
```

**Testing Approach:**
```java
@Test
void testCreateInvoiceWithGeneratedNumber() {
    // Given: customer exists, no invoice number provided
    // When: invoice.create()
    // Then: invoice number generated in format INV-YYYY-NNNNN
}

@Test
void testCreateInvoiceWithProvidedNumber() {
    // Given: custom invoice number provided
    // When: invoice.create()
    // Then: uses provided invoice number
}

@Test
void testBeforeCreateRejectsNonExistentCustomer() {
    // Given: invalid customerId
    // When: invoice.beforeCreate()
    // Then: throws NotFoundException
}

@Test
void testBeforeCreateRejectsDuplicateInvoiceNumber() {
    // Given: invoice number already exists
    // When: invoice.beforeCreate()
    // Then: throws ValidationException
}
```

---

### US-34: Invoice DTOs & Mapper

**As a developer, I need Invoice DTOs and mapper so that API boundaries are clean**

**Acceptance Criteria:**
- CreateInvoiceRequest with validation
- UpdateInvoiceRequest with validation
- InvoiceResponse with all fields including calculated balance
- InvoiceListItemResponse with subset of fields
- InvoiceMapper with toResponse() and toListItem() methods
- Unit tests verify mapping

**Implementation Scaffolding:**

```java
// CreateInvoiceRequest.java
public class CreateInvoiceRequest {
    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    private String invoiceNumber; // Optional, will auto-generate if not provided

    private String notes;

    // Getters/Setters
}

// UpdateInvoiceRequest.java
public class UpdateInvoiceRequest {
    @NotNull(message = "Invoice ID is required")
    private UUID id;

    @NotNull(message = "Version is required")
    private Long version;

    // Note: customerId NOT included (immutable)

    private String invoiceNumber;
    private String notes;

    // Getters/Setters
}

// InvoiceResponse.java
public class InvoiceResponse {
    // BaseEntity fields
    private UUID id;
    private Instant createdAt;
    private UUID createdBy;
    private Instant lastModifiedAt;
    private UUID lastModifiedBy;
    private Long version;

    // Invoice fields
    private UUID customerId;
    private String invoiceNumber;
    private String notes;

    // Read-only fields
    private Instant invoiceDate;
    private InvoiceStatus status;
    private String customerName;
    private BigDecimal total;
    private BigDecimal amountPaid;

    // Calculated field (not stored in DB)
    private BigDecimal balance; // total - amountPaid

    // Getters/Setters
}

// InvoiceListItemResponse.java
public class InvoiceListItemResponse {
    private UUID id;
    private String invoiceNumber;
    private InvoiceStatus status;
    private String customerName;
    private BigDecimal total;
    private BigDecimal amountPaid;
    private BigDecimal balance; // calculated

    // Getters/Setters
}

// InvoiceMapper.java
@Component
public class InvoiceMapper {

    public InvoiceResponse toResponse(Invoice entity) {
        InvoiceResponse dto = new InvoiceResponse();

        // Map all fields
        // ...

        // Calculate balance
        dto.setBalance(entity.getTotal().subtract(entity.getAmountPaid()));

        return dto;
    }

    public InvoiceListItemResponse toListItem(Invoice entity) {
        InvoiceListItemResponse dto = new InvoiceListItemResponse();

        // Map subset of fields
        // ...

        // Calculate balance
        dto.setBalance(entity.getTotal().subtract(entity.getAmountPaid()));

        return dto;
    }
}
```

**Testing Approach:**
```java
@Test
void testToResponseCalculatesBalance() {
    // Given: invoice with total=100, amountPaid=30
    // When: mapper.toResponse()
    // Then: balance = 70
}

@Test
void testToListItemIncludesEssentialFields() {
    // Verify mapper includes only list-specific fields
}
```

---

### US-35: Invoice Service - Create

**As a developer, I need InvoiceService.createInvoice() so that business logic is orchestrated**

**Acceptance Criteria:**
- createInvoice(CreateInvoiceRequest) method with @Transactional
- Follows beforeCreate → create → save → afterCreate flow
- Loads Customer entity for validation and data population
- Passes CustomerService to afterCreate for cascade
- Unit tests with mocked dependencies
- Integration test verifies full flow including customer cascade

**Implementation Scaffolding:**

```java
@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private InvoiceMapper invoiceMapper;

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        // Load customer (validates existence)
        Customer customer = customerRepository.findByIdAndIsDeletedFalse(request.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        // Create invoice
        Invoice invoice = new Invoice();
        invoice.beforeCreate(request, customer, invoiceRepository);
        invoice.create(request, customer);
        invoiceRepository.save(invoice);
        invoice.afterCreate(eventPublisher);  // Publishes InvoiceCreatedEvent

        return invoiceMapper.toResponse(invoice);
    }

    // Event listener for customer name changes
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onCustomerNameChanged(CustomerNameChangedEvent event) {
        // Update denormalized customerName on all invoices for this customer
        List<Invoice> invoices = invoiceRepository.findAllByCustomerIdAndIsDeletedFalse(event.getCustomerId());

        for (Invoice invoice : invoices) {
            invoice.setCustomerName(event.getNewCompanyName());
            invoiceRepository.save(invoice);
        }
    }
}

// CustomerService.java (event listener for invoice events)
@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    // Event listener for invoice created
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onInvoiceCreated(InvoiceCreatedEvent event) {
        Customer customer = customerRepository.findByIdAndIsDeletedFalse(event.getCustomerId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        customer.setDraftInvoiceCount(customer.getDraftInvoiceCount() + 1);
        customerRepository.save(customer);
    }
}
```

**Testing Approach:**
```java
@Test
void testCreateInvoiceSuccess() {
    // Mock customer repository returns customer
    // Mock invoice repository save
    // Verify invoice.afterCreate() calls customerService.incrementDraftInvoiceCount()
}

@Test
void testCreateInvoiceCustomerNotFound() {
    // Mock customer repository returns empty
    // Expect NotFoundException
}

@SpringBootTest
@Transactional
@Test
void testCreateInvoiceIntegration() {
    // Create customer
    // Create invoice
    // Verify invoice created with generated number
    // Verify customer.draftInvoiceCount incremented
}
```

---

### US-36: Invoice Controller - Create

**As a developer, I need POST /api/invoices endpoint so that invoices can be created via REST API**

**Acceptance Criteria:**
- POST /api/invoices endpoint with complete OpenAPI documentation
- Returns 201 Created on success
- Returns ApiResponse<InvoiceResponse> wrapper
- Integration test with MockMvc
- **OpenAPI documentation complete with examples**

**Implementation Scaffolding:**

```java
@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = "Invoice management operations")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @Operation(
        summary = "Create a new invoice",
        description = "Creates a new invoice in DRAFT status for the specified customer. Invoice number can be provided or will be auto-generated in format INV-YYYY-#####."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Invoice created successfully",
            content = @Content(schema = @Schema(implementation = InvoiceResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data (validation errors)"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Customer not found"
        )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request) {

        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice created successfully", response));
    }
}
```

**Testing Approach:**
```java
@Test
void testCreateInvoiceSuccess() {
    // POST /api/invoices with valid customerId and notes
    // Expect 201 Created
    // Verify invoice created with generated number
    // Verify status = DRAFT, total = 0, amountPaid = 0
}

@Test
void testCreateInvoiceCustomerNotFound() {
    // POST with invalid customerId
    // Expect 404 Not Found
}

@Test
void testCreateInvoiceWithCustomNumber() {
    // POST with custom invoiceNumber
    // Verify uses provided number
}
```

---

### US-37: Frontend - Invoice Form (Create)

**As a user, I need a form to create invoices so that I can add new invoices**

**Acceptance Criteria:**
- Form with customer dropdown, optional invoice number, notes
- Customer dropdown populated from customer list API
- Invoice number field optional (shows placeholder about auto-generation)
- Submits to API and shows loading state
- Navigates to invoice detail on success
- Shows error messages on failure
- **ViewModel tests for InvoiceFormViewModel**
- **Component tests for InvoiceFormView**
- **E2E test for create invoice flow**

**Implementation Scaffolding:**

```typescript
// models/Invoice.ts
export interface Invoice {
  id: string;
  customerId: string;
  invoiceNumber: string;
  notes: string | null;
  invoiceDate: string | null;
  status: 'DRAFT' | 'SENT' | 'PAID';
  customerName: string;
  total: string;
  amountPaid: string;
  balance: string; // calculated
  createdAt: string;
  createdBy: string;
  lastModifiedAt: string;
  lastModifiedBy: string;
  version: number;
}

export interface InvoiceListItem {
  id: string;
  invoiceNumber: string;
  status: 'DRAFT' | 'SENT' | 'PAID';
  customerName: string;
  total: string;
  amountPaid: string;
  balance: string;
}

export interface CreateInvoiceRequest {
  customerId: string;
  invoiceNumber?: string;
  notes?: string;
}

// api/invoiceApi.ts
export const invoiceApi = {
  create: async (request: CreateInvoiceRequest): Promise<Invoice> => {
    const response = await apiClient.post<ApiResponse<Invoice>>('/invoices', request);
    return response.data.data;
  },
};

// viewmodels/invoices/InvoiceFormViewModel.ts
export const InvoiceFormViewModel = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Form fields
  const [customerId, setCustomerId] = useState('');
  const [invoiceNumber, setInvoiceNumber] = useState('');
  const [notes, setNotes] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Load customers for dropdown
  const { data: customers } = useQuery({
    queryKey: ['customers', 'list'],
    queryFn: customerApi.listAll,
  });

  // Create mutation
  const createMutation = useMutation({
    mutationFn: invoiceApi.create,
    onSuccess: (invoice) => {
      queryClient.invalidateQueries({ queryKey: ['invoices', 'list'] });
      navigate(`/invoices/${invoice.id}`);
    },
    onError: (error: any) => {
      setErrors({ submit: error.response?.data?.message || 'Failed to create invoice' });
    },
  });

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!customerId) {
      newErrors.customerId = 'Customer is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const formData: CreateInvoiceRequest = {
      customerId,
      invoiceNumber: invoiceNumber || undefined,
      notes: notes || undefined,
    };

    createMutation.mutate(formData);
  };

  return {
    customerId, setCustomerId,
    invoiceNumber, setInvoiceNumber,
    notes, setNotes,
    customers,
    errors,
    handleSubmit,
    handleCancel: () => navigate('/invoices'),
    isSubmitting: createMutation.isPending,
  };
};

// views/invoices/InvoiceFormView.tsx
export const InvoiceFormView: React.FC = () => {
  const vm = InvoiceFormViewModel();

  return (
    <div className="max-w-2xl mx-auto p-6">
      <h1 className="text-3xl font-bold mb-6">Create Invoice</h1>

      <form onSubmit={vm.handleSubmit} className="space-y-6">
        {/* Customer Dropdown */}
        <div>
          <label className="block text-sm font-medium mb-2">
            Customer <span className="text-red-500">*</span>
          </label>
          <select
            value={vm.customerId}
            onChange={(e) => vm.setCustomerId(e.target.value)}
            className={`w-full border rounded px-3 py-2 ${
              vm.errors.customerId ? 'border-red-500' : 'border-gray-300'
            }`}
          >
            <option value="">Select a customer...</option>
            {vm.customers?.map((customer) => (
              <option key={customer.id} value={customer.id}>
                {customer.companyName}
              </option>
            ))}
          </select>
          {vm.errors.customerId && (
            <p className="text-red-500 text-sm mt-1">{vm.errors.customerId}</p>
          )}
        </div>

        {/* Invoice Number (Optional) */}
        <div>
          <label className="block text-sm font-medium mb-2">Invoice Number</label>
          <input
            type="text"
            value={vm.invoiceNumber}
            onChange={(e) => vm.setInvoiceNumber(e.target.value)}
            placeholder="Leave blank to auto-generate (INV-YYYY-#####)"
            className="w-full border border-gray-300 rounded px-3 py-2"
          />
          <p className="text-gray-500 text-sm mt-1">
            Optional. If not provided, will be auto-generated.
          </p>
        </div>

        {/* Notes */}
        <div>
          <label className="block text-sm font-medium mb-2">Notes</label>
          <textarea
            value={vm.notes}
            onChange={(e) => vm.setNotes(e.target.value)}
            rows={4}
            className="w-full border border-gray-300 rounded px-3 py-2"
            placeholder="Payment terms, shipping instructions, etc."
          />
        </div>

        {/* Submit Error */}
        {vm.errors.submit && (
          <div className="bg-red-50 border border-red-300 rounded p-3">
            <p className="text-red-700 text-sm">{vm.errors.submit}</p>
          </div>
        )}

        {/* Actions */}
        <div className="flex gap-3">
          <button
            type="submit"
            disabled={vm.isSubmitting}
            className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {vm.isSubmitting ? 'Creating...' : 'Create Invoice'}
          </button>
          <button
            type="button"
            onClick={vm.handleCancel}
            className="bg-gray-300 text-gray-700 px-6 py-2 rounded hover:bg-gray-400"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
};
```

**Testing Approach:**

**ViewModel Tests** (`InvoiceFormViewModel.test.ts`):
```typescript
describe('InvoiceFormViewModel', () => {
  it('should initialize with empty form fields', () => {
    const vm = InvoiceFormViewModel();
    expect(vm.customerId).toBe('');
    expect(vm.invoiceNumber).toBe('');
    expect(vm.notes).toBe('');
  });

  it('should validate required customer field', () => {
    const vm = InvoiceFormViewModel();
    const isValid = vm.validate();
    expect(isValid).toBe(false);
    expect(vm.errors.customerId).toBe('Customer is required');
  });

  it('should call API and navigate on successful submit', async () => {
    // Mock API response
    server.use(
      http.post('/api/invoices', () => {
        return HttpResponse.json({ data: mockInvoice }, { status: 201 });
      })
    );

    const vm = InvoiceFormViewModel();
    vm.setCustomerId('customer-123');
    await vm.handleSubmit(mockEvent);

    expect(mockNavigate).toHaveBeenCalledWith('/invoices/invoice-123');
  });

  it('should display error on API failure', async () => {
    server.use(
      http.post('/api/invoices', () => {
        return HttpResponse.json({ message: 'Customer not found' }, { status: 404 });
      })
    );

    const vm = InvoiceFormViewModel();
    await vm.handleSubmit(mockEvent);

    expect(vm.errors.submit).toBe('Customer not found');
  });
});
```

**Component Tests** (`InvoiceFormView.test.tsx`):
```typescript
describe('InvoiceFormView', () => {
  it('should render form with all fields', () => {
    render(<InvoiceFormView />);

    expect(screen.getByLabelText(/customer/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/invoice number/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/notes/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create invoice/i })).toBeInTheDocument();
  });

  it('should populate customer dropdown from API', async () => {
    render(<InvoiceFormView />);

    await waitFor(() => {
      expect(screen.getByRole('option', { name: /acme corp/i })).toBeInTheDocument();
    });
  });

  it('should submit form when filled correctly', async () => {
    const user = userEvent.setup();
    render(<InvoiceFormView />);

    await user.selectOptions(screen.getByLabelText(/customer/i), 'customer-123');
    await user.type(screen.getByLabelText(/notes/i), 'Test invoice');
    await user.click(screen.getByRole('button', { name: /create invoice/i }));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/invoices/invoice-123');
    });
  });

  it('should show validation errors', async () => {
    const user = userEvent.setup();
    render(<InvoiceFormView />);

    await user.click(screen.getByRole('button', { name: /create invoice/i }));

    expect(await screen.findByText(/customer is required/i)).toBeInTheDocument();
  });
});
```

**E2E Test** (`create-invoice.spec.ts`):
```typescript
test.describe('Create Invoice', () => {
  test('should create invoice with auto-generated number', async ({ page }) => {
    await page.goto('/invoices/new');

    // Fill form
    await page.selectOption('[name="customerId"]', 'customer-123');
    await page.fill('[name="notes"]', 'Payment terms: Net 30');

    // Submit
    await page.click('button:has-text("Create Invoice")');

    // Verify redirect to detail page
    await expect(page).toHaveURL(/\/invoices\/[a-f0-9-]+$/);
    await expect(page.locator('h1')).toContainText(/INV-\d{4}-\d{5}/);
    await expect(page.locator('text=Payment terms: Net 30')).toBeVisible();
  });

  test('should create invoice with custom number', async ({ page }) => {
    await page.goto('/invoices/new');

    await page.selectOption('[name="customerId"]', 'customer-123');
    await page.fill('[name="invoiceNumber"]', 'CUSTOM-001');
    await page.click('button:has-text("Create Invoice")');

    await expect(page.locator('h1')).toContainText('CUSTOM-001');
  });

  test('should show error for duplicate invoice number', async ({ page }) => {
    await page.goto('/invoices/new');

    await page.selectOption('[name="customerId"]', 'customer-123');
    await page.fill('[name="invoiceNumber"]', 'INV-2025-00001'); // Already exists
    await page.click('button:has-text("Create Invoice")');

    await expect(page.locator('.error')).toContainText(/invoice number already exists/i);
  });
});
```

---

## Phase 4B: List Invoices

### US-38: Invoice Service - List

**As a developer, I need InvoiceService.listAllInvoices() so that invoices can be retrieved**

**Acceptance Criteria:**
- listAllInvoices() returns List<InvoiceListItemResponse>
- Sorted by invoice date descending (most recent first)
- Excludes soft-deleted invoices
- Optional filter by customerId
- Integration test verifies sorting and filtering

**Implementation Scaffolding:**

```java
// InvoiceService.java
@Transactional(readOnly = true)
public List<InvoiceListItemResponse> listAllInvoices() {
    return invoiceRepository.findAllByIsDeletedFalseOrderByInvoiceDateDesc()
            .stream()
            .map(invoiceMapper::toListItem)
            .collect(Collectors.toList());
}

@Transactional(readOnly = true)
public List<InvoiceListItemResponse> listInvoicesByCustomer(UUID customerId) {
    return invoiceRepository.findAllByCustomerIdAndIsDeletedFalse(customerId)
            .stream()
            .map(invoiceMapper::toListItem)
            .collect(Collectors.toList());
}
```

**Testing:** Similar to Customer list tests

---

### US-39: Invoice Controller - List

**As a developer, I need GET /api/invoices endpoint so that invoices can be listed via REST API**

**Implementation Scaffolding:**

```java
@GetMapping
public ResponseEntity<ApiResponse<List<InvoiceListItemResponse>>> listAllInvoices(
        @RequestParam(required = false) UUID customerId) {

    List<InvoiceListItemResponse> response = customerId != null
            ? invoiceService.listInvoicesByCustomer(customerId)
            : invoiceService.listAllInvoices();

    return ResponseEntity.ok(ApiResponse.success(response));
}
```

---

### US-40: Frontend - Invoice List View

**As a user, I need to see a list of invoices so that I can view all invoices**

**Acceptance Criteria:**
- Table showing invoice number, customer name, status, total, balance
- Status badges with color coding (DRAFT=gray, SENT=blue, PAID=green)
- Sorted by most recent first
- Click row navigates to detail view
- "Create Invoice" button
- Optional filter by customer (if accessed from customer detail)

**Implementation Scaffolding:**

```typescript
// viewmodels/invoices/InvoiceListViewModel.ts
export const InvoiceListViewModel = (customerId?: string) => {
  const navigate = useNavigate();

  const { data: invoices, isLoading, isError } = useQuery({
    queryKey: ['invoices', 'list', customerId],
    queryFn: () => customerId
      ? invoiceApi.listByCustomer(customerId)
      : invoiceApi.listAll(),
  });

  const handleCreateNew = () => navigate('/invoices/new');
  const handleRowClick = (invoiceId: string) => navigate(`/invoices/${invoiceId}`);

  return {
    invoices,
    isLoading,
    isError,
    handleCreateNew,
    handleRowClick,
  };
};

// views/invoices/InvoiceListView.tsx
// Similar structure to CustomerListView
// - Table with columns: Invoice #, Customer, Status, Total, Balance
// - Status badge component with color coding
// - Click row to navigate to detail
```

---

## Phase 4C: View Invoice Detail

### US-41: Invoice Service - Get by ID

**Implementation Scaffolding:**

```java
@Transactional(readOnly = true)
public InvoiceResponse getInvoiceById(UUID id) {
    Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(id)
            .orElseThrow(() -> new NotFoundException("Invoice not found"));

    return invoiceMapper.toResponse(invoice);
}
```

---

### US-42: Invoice Controller - Get by ID

**Implementation Scaffolding:**

```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(@PathVariable UUID id) {
    InvoiceResponse response = invoiceService.getInvoiceById(id);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

---

### US-43: Frontend - Invoice Detail View

**As a user, I need to view invoice details so that I can see all invoice information**

**Acceptance Criteria:**
- Shows all invoice fields (number, customer, status, dates, notes)
- Shows total, amount paid, balance
- Shows "Edit" button (only if status=DRAFT)
- Shows "Send Invoice" button (only if status=DRAFT and total > 0)
- Shows "Delete" button (only if status=DRAFT or PAID)
- Shows placeholder for line items list (to be implemented in Phase 5)
- Shows placeholder for payments list (to be implemented in Phase 6)

**Implementation Scaffolding:**

```typescript
// viewmodels/invoices/InvoiceDetailViewModel.ts
export const InvoiceDetailViewModel = (invoiceId: string) => {
  const navigate = useNavigate();

  const { data: invoice, isLoading, isError } = useQuery({
    queryKey: ['invoices', invoiceId],
    queryFn: () => invoiceApi.getById(invoiceId),
  });

  // Action availability based on status
  const canEdit = invoice?.status === 'DRAFT';
  const canSend = invoice?.status === 'DRAFT' && parseFloat(invoice.total) > 0;
  const canDelete = invoice?.status === 'DRAFT' || invoice?.status === 'PAID';

  const handleEdit = () => navigate(`/invoices/${invoiceId}/edit`);
  const handleSend = () => { /* Will implement in Phase 4E */ };
  const handleDelete = () => { /* Will implement in Phase 4F */ };

  return {
    invoice,
    isLoading,
    isError,
    canEdit,
    canSend,
    canDelete,
    handleEdit,
    handleSend,
    handleDelete,
  };
};

// views/invoices/InvoiceDetailView.tsx
// Layout:
// - Header with invoice number and status badge
// - Customer info section (name, click to view customer)
// - Invoice details (dates, notes)
// - Financial summary (total, amount paid, balance)
// - Action buttons (Edit, Send, Delete) - conditional based on status
// - Line items section (placeholder: "No line items yet")
// - Payments section (placeholder: "No payments yet")
```

---

## Phase 4D: Update Invoice

### US-44: Invoice Service - Update

**As a developer, I need InvoiceService.updateInvoice() so that invoices can be modified**

**Acceptance Criteria:**
- updateInvoice(UpdateInvoiceRequest, isSystemUpdate) method
- Validates status=DRAFT (unless isSystemUpdate=true)
- Does NOT allow changing customerId
- Validates invoice number uniqueness if changed
- Handles optimistic locking with version
- Supports system updates for cascade operations (customerName changes)

**Implementation Scaffolding:**

```java
// Invoice.java
public void beforeUpdate(UpdateInvoiceRequest request,
                         InvoiceRepository invoiceRepository,
                         boolean isSystemUpdate) {
    // If user update, must be DRAFT
    if (!isSystemUpdate && this.status != InvoiceStatus.DRAFT) {
        throw new ValidationException("Cannot edit invoice with status " + this.status);
    }

    // Validate invoice number uniqueness if changed
    if (request.getInvoiceNumber() != null &&
        !request.getInvoiceNumber().equals(this.invoiceNumber)) {
        if (invoiceRepository.existsByInvoiceNumberAndIsDeletedFalse(request.getInvoiceNumber())) {
            throw new ValidationException("Invoice number already exists");
        }
    }
}

public void update(UpdateInvoiceRequest request) {
    // Update editable fields
    if (request.getInvoiceNumber() != null) {
        this.invoiceNumber = request.getInvoiceNumber();
    }
    if (request.getNotes() != null) {
        this.notes = request.getNotes();
    }

    // Note: customerId is NOT editable
}

public void afterUpdate(ApplicationEventPublisher eventPublisher) {
    // No domain events needed for invoice updates
    // Customer name changes are handled via CustomerNameChangedEvent from Customer domain
}

// InvoiceService.java
@Transactional
public InvoiceResponse updateInvoice(UpdateInvoiceRequest request, boolean isSystemUpdate) {
    Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(request.getId())
            .orElseThrow(() -> new NotFoundException("Invoice not found"));

    // Optimistic locking check
    if (!invoice.getVersion().equals(request.getVersion())) {
        throw new OptimisticLockException("Invoice was modified by another user");
    }

    invoice.beforeUpdate(request, invoiceRepository, isSystemUpdate);
    invoice.update(request);
    invoiceRepository.save(invoice);
    invoice.afterUpdate(eventPublisher);

    return invoiceMapper.toResponse(invoice);
}

// Public method for user updates
@Transactional
public InvoiceResponse updateInvoice(UpdateInvoiceRequest request) {
    return updateInvoice(request, false);
}

// Note: Customer name updates are handled via CustomerNameChangedEvent
// See onCustomerNameChanged() event listener above
```

**Testing Approach:**
```java
@Test
void testUpdateInvoiceDraft() {
    // Given: invoice with status DRAFT
    // When: updateInvoice with new notes
    // Then: updates successfully
}

@Test
void testUpdateInvoiceSentRejectsUserEdit() {
    // Given: invoice with status SENT
    // When: updateInvoice with isSystemUpdate=false
    // Then: throws ValidationException
}

@Test
void testUpdateInvoiceSentAllowsSystemUpdate() {
    // Given: invoice with status SENT
    // When: updateInvoice with isSystemUpdate=true
    // Then: updates successfully (for cascade scenarios)
}

@Test
void testUpdateInvoiceOptimisticLockConflict() {
    // Given: invoice modified by another user (version mismatch)
    // When: updateInvoice
    // Then: throws OptimisticLockException
}
```

---

### US-45: Invoice Controller - Update

**Implementation Scaffolding:**

```java
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<InvoiceResponse>> updateInvoice(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateInvoiceRequest request) {

    // Ensure ID in path matches ID in body
    if (!id.equals(request.getId())) {
        throw new ValidationException("ID mismatch");
    }

    InvoiceResponse response = invoiceService.updateInvoice(request);
    return ResponseEntity.ok(ApiResponse.success("Invoice updated successfully", response));
}
```

---

### US-46: Frontend - Invoice Form (Edit)

**As a user, I need to edit draft invoices so that I can make changes before sending**

**Acceptance Criteria:**
- Same form as create, pre-populated with existing data
- Customer dropdown disabled (cannot change customer)
- Shows validation errors if trying to edit non-DRAFT invoice
- Handles optimistic locking errors gracefully

**Implementation Scaffolding:**

```typescript
// viewmodels/invoices/InvoiceFormViewModel.ts
// Extend to support edit mode
export const InvoiceFormViewModel = () => {
  const { id } = useParams<{ id: string }>();
  const isEditMode = !!id;

  // Fetch existing invoice if editing
  const { data: existingInvoice } = useQuery({
    queryKey: ['invoices', id],
    queryFn: () => invoiceApi.getById(id!),
    enabled: isEditMode,
  });

  // Load form with existing data
  useEffect(() => {
    if (existingInvoice) {
      setCustomerId(existingInvoice.customerId);
      setInvoiceNumber(existingInvoice.invoiceNumber);
      setNotes(existingInvoice.notes || '');
    }
  }, [existingInvoice]);

  // Update mutation
  const updateMutation = useMutation({
    mutationFn: invoiceApi.update,
    onSuccess: (invoice) => {
      queryClient.invalidateQueries({ queryKey: ['invoices', id] });
      navigate(`/invoices/${invoice.id}`);
    },
    onError: (error: any) => {
      if (error.response?.status === 409) {
        setErrors({ submit: 'Invoice was modified by another user. Please refresh and try again.' });
      } else {
        setErrors({ submit: error.response?.data?.message || 'Failed to update invoice' });
      }
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    if (isEditMode && existingInvoice) {
      const updateData: UpdateInvoiceRequest = {
        id: existingInvoice.id,
        version: existingInvoice.version,
        invoiceNumber,
        notes,
      };
      updateMutation.mutate(updateData);
    } else {
      // Create logic...
    }
  };

  return {
    // ... existing return
    isEditMode,
    isCustomerDisabled: isEditMode, // Cannot change customer in edit mode
  };
};

// views/invoices/InvoiceFormView.tsx
// Update customer dropdown:
<select
  value={vm.customerId}
  onChange={(e) => vm.setCustomerId(e.target.value)}
  disabled={vm.isCustomerDisabled}
  className={...}
>
  {/* options */}
</select>
```

---

## Phase 4E: Mark Invoice as Sent

### US-47: Invoice Service - Mark as Sent

**As a developer, I need InvoiceService.markInvoiceAsSent() so that invoice status can transition**

**Acceptance Criteria:**
- markInvoiceAsSent(invoiceId, version) method
- Validates current status is DRAFT
- Validates total > 0
- Sets status=SENT, invoiceDate=now
- Cascades to Customer: decrement draftInvoiceCount, increment sentInvoiceCount, add to totalOutstanding

**Implementation Scaffolding:**

```java
// Invoice.java
public void beforeSend() {
    if (this.status != InvoiceStatus.DRAFT) {
        throw new ValidationException("Only DRAFT invoices can be sent");
    }

    if (this.total.compareTo(BigDecimal.ZERO) <= 0) {
        throw new ValidationException("Cannot send invoice with total of $0");
    }
}

public void send() {
    this.status = InvoiceStatus.SENT;
    this.invoiceDate = Instant.now();
}

public void afterSend(ApplicationEventPublisher eventPublisher) {
    // Publish domain event: Invoice status changed DRAFT → SENT
    eventPublisher.publishEvent(new InvoiceStatusChangedEvent(
        this.id,
        this.customerId,
        InvoiceStatus.DRAFT,  // oldStatus
        InvoiceStatus.SENT,   // newStatus
        this.total
    ));
}

// CustomerService.java - Event listener for invoice status changes
@EventListener
@Transactional(propagation = Propagation.MANDATORY)
public void onInvoiceStatusChanged(InvoiceStatusChangedEvent event) {
    Customer customer = customerRepository.findByIdAndIsDeletedFalse(event.getCustomerId())
            .orElseThrow(() -> new NotFoundException("Customer not found"));

    // Handle DRAFT → SENT transition
    if (event.getOldStatus() == InvoiceStatus.DRAFT && event.getNewStatus() == InvoiceStatus.SENT) {
        customer.setDraftInvoiceCount(customer.getDraftInvoiceCount() - 1);
        customer.setSentInvoiceCount(customer.getSentInvoiceCount() + 1);
        customer.setTotalOutstanding(customer.getTotalOutstanding().add(event.getInvoiceTotal()));
    }

    // Handle SENT → PAID transition (will be implemented in Phase 6)
    // ...

    customerRepository.save(customer);
}

// InvoiceService.java
@Transactional
public InvoiceResponse markInvoiceAsSent(UUID invoiceId, Long version) {
    Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(invoiceId)
            .orElseThrow(() -> new NotFoundException("Invoice not found"));

    // Optimistic locking
    if (!invoice.getVersion().equals(version)) {
        throw new OptimisticLockException("Invoice was modified by another user");
    }

    invoice.beforeSend();
    invoice.send();
    invoiceRepository.save(invoice);
    invoice.afterSend(eventPublisher);  // Publishes InvoiceStatusChangedEvent

    return invoiceMapper.toResponse(invoice);
}
```

**Testing Approach:**
```java
@Test
void testMarkInvoiceAsSentSuccess() {
    // Given: DRAFT invoice with total > 0
    // When: markInvoiceAsSent()
    // Then: status=SENT, invoiceDate set, customer counts updated
}

@Test
void testMarkInvoiceAsSentRejectsIfNotDraft() {
    // Given: invoice already SENT
    // When: markInvoiceAsSent()
    // Then: throws ValidationException
}

@Test
void testMarkInvoiceAsSentRejectsIfTotalZero() {
    // Given: DRAFT invoice with total=0
    // When: markInvoiceAsSent()
    // Then: throws ValidationException
}
```

---

### US-48: Invoice Controller - Mark as Sent

**Implementation Scaffolding:**

```java
@PostMapping("/{id}/send")
public ResponseEntity<ApiResponse<InvoiceResponse>> markInvoiceAsSent(
        @PathVariable UUID id,
        @RequestParam Long version) {

    InvoiceResponse response = invoiceService.markInvoiceAsSent(id, version);
    return ResponseEntity.ok(ApiResponse.success("Invoice sent successfully", response));
}
```

---

### US-49: Frontend - Send Invoice Button

**As a user, I need to send invoices so that they can be marked as sent and locked**

**Acceptance Criteria:**
- "Send Invoice" button on detail view (only visible if status=DRAFT and total > 0)
- Confirmation dialog before sending
- Shows success message after sending
- Updates invoice detail view to show new status
- Disables edit/delete buttons after sending

**Implementation Scaffolding:**

```typescript
// api/invoiceApi.ts
export const invoiceApi = {
  // ... existing methods

  markAsSent: async (invoiceId: string, version: number): Promise<Invoice> => {
    const response = await apiClient.post<ApiResponse<Invoice>>(
      `/invoices/${invoiceId}/send?version=${version}`
    );
    return response.data.data;
  },
};

// viewmodels/invoices/InvoiceDetailViewModel.ts
const sendMutation = useMutation({
  mutationFn: ({ id, version }: { id: string; version: number }) =>
    invoiceApi.markAsSent(id, version),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['invoices', invoiceId] });
    // Show success message
  },
  onError: (error: any) => {
    // Show error message
  },
});

const handleSend = () => {
  if (!invoice) return;

  // Show confirmation dialog
  if (window.confirm('Are you sure you want to send this invoice? It cannot be edited after sending.')) {
    sendMutation.mutate({ id: invoice.id, version: invoice.version });
  }
};

// views/invoices/InvoiceDetailView.tsx
{vm.canSend && (
  <button
    onClick={vm.handleSend}
    className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
  >
    Send Invoice
  </button>
)}
```

---

## Phase 4F: Delete Invoice

### US-50: Invoice Service - Delete

**As a developer, I need InvoiceService.deleteInvoice() so that invoices can be deleted**

**Acceptance Criteria:**
- deleteInvoice(invoiceId, version) method
- Validates status is DRAFT or PAID (cannot delete SENT invoices)
- Soft deletes invoice
- Cascade deletes line items and payments
- Decrements customer draft/paid invoice count based on status

**Implementation Scaffolding:**

```java
// Invoice.java
public void beforeDelete() {
    if (this.status == InvoiceStatus.SENT) {
        throw new ValidationException("Cannot delete SENT invoices");
    }
}

public void delete() {
    this.markAsDeleted(); // Soft delete from BaseEntity
}

public void afterDelete(ApplicationEventPublisher eventPublisher) {
    // Publish domain event: Invoice deleted
    eventPublisher.publishEvent(new InvoiceDeletedEvent(
        this.id,
        this.customerId,
        this.status
    ));
}

// CustomerService.java - Event listener for invoice deletion
@EventListener
@Transactional(propagation = Propagation.MANDATORY)
public void onInvoiceDeleted(InvoiceDeletedEvent event) {
    Customer customer = customerRepository.findByIdAndIsDeletedFalse(event.getCustomerId())
            .orElseThrow(() -> new NotFoundException("Customer not found"));

    // Update customer counts based on invoice status
    if (event.getInvoiceStatus() == InvoiceStatus.DRAFT) {
        customer.setDraftInvoiceCount(customer.getDraftInvoiceCount() - 1);
    } else if (event.getInvoiceStatus() == InvoiceStatus.PAID) {
        customer.setPaidInvoiceCount(customer.getPaidInvoiceCount() - 1);
    }

    customerRepository.save(customer);
}

// LineItemService.java - Event listener for invoice deletion
@EventListener
@Transactional(propagation = Propagation.MANDATORY)
public void onInvoiceDeleted(InvoiceDeletedEvent event) {
    // Cascade delete all line items for this invoice
    List<LineItem> lineItems = lineItemRepository.findAllByInvoiceIdAndIsDeletedFalseOrderByCreatedAtAsc(event.getInvoiceId());

    for (LineItem lineItem : lineItems) {
        lineItem.delete();
        lineItemRepository.save(lineItem);
    }
}

// PaymentService.java - Event listener for invoice deletion
@EventListener
@Transactional(propagation = Propagation.MANDATORY)
public void onInvoiceDeleted(InvoiceDeletedEvent event) {
    // Cascade delete all payments for this invoice
    List<Payment> payments = paymentRepository.findAllByInvoiceIdAndIsDeletedFalseOrderByPaymentDateAsc(event.getInvoiceId());

    for (Payment payment : payments) {
        payment.delete();
        paymentRepository.save(payment);
    }
}

// InvoiceService.java
@Transactional
public void deleteInvoice(UUID invoiceId, Long version) {
    Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(invoiceId)
            .orElseThrow(() -> new NotFoundException("Invoice not found"));

    // Optimistic locking
    if (!invoice.getVersion().equals(version)) {
        throw new OptimisticLockException("Invoice was modified by another user");
    }

    invoice.beforeDelete();
    invoice.delete();
    invoiceRepository.save(invoice);
    invoice.afterDelete(eventPublisher);  // Publishes InvoiceDeletedEvent
}
```

**Testing Approach:**
```java
@Test
void testDeleteDraftInvoice() {
    // Given: DRAFT invoice
    // When: deleteInvoice()
    // Then: soft deleted, customer draftInvoiceCount decremented
}

@Test
void testDeletePaidInvoice() {
    // Given: PAID invoice
    // When: deleteInvoice()
    // Then: soft deleted, customer paidInvoiceCount decremented
}

@Test
void testDeleteSentInvoiceRejected() {
    // Given: SENT invoice
    // When: deleteInvoice()
    // Then: throws ValidationException
}
```

---

### US-51: Invoice Controller - Delete

**Implementation Scaffolding:**

```java
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> deleteInvoice(
        @PathVariable UUID id,
        @RequestParam Long version) {

    invoiceService.deleteInvoice(id, version);
    return ResponseEntity.ok(ApiResponse.success("Invoice deleted successfully", null));
}
```

---

### US-52: Frontend - Delete Invoice

**As a user, I need to delete invoices so that I can remove unwanted invoices**

**Acceptance Criteria:**
- "Delete" button on detail view (only visible if status=DRAFT or PAID)
- Confirmation dialog before deleting
- Navigates to invoice list after successful delete
- Shows error if invoice is SENT

**Implementation Scaffolding:**

```typescript
// api/invoiceApi.ts
export const invoiceApi = {
  // ... existing methods

  delete: async (invoiceId: string, version: number): Promise<void> => {
    await apiClient.delete(`/invoices/${invoiceId}?version=${version}`);
  },
};

// viewmodels/invoices/InvoiceDetailViewModel.ts
const deleteMutation = useMutation({
  mutationFn: ({ id, version }: { id: string; version: number }) =>
    invoiceApi.delete(id, version),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ['invoices', 'list'] });
    navigate('/invoices');
  },
  onError: (error: any) => {
    // Show error message
  },
});

const handleDelete = () => {
  if (!invoice) return;

  if (window.confirm('Are you sure you want to delete this invoice? This cannot be undone.')) {
    deleteMutation.mutate({ id: invoice.id, version: invoice.version });
  }
};

// views/invoices/InvoiceDetailView.tsx
{vm.canDelete && (
  <button
    onClick={vm.handleDelete}
    className="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700"
  >
    Delete Invoice
  </button>
)}
```

---

## Completion Checklist

**Backend:**
- [ ] Invoice entity with full lifecycle methods implemented
- [ ] InvoiceStatus enum defined
- [ ] Invoices table created with Flyway migration
- [ ] All database indexes created
- [ ] InvoiceRepository with query methods
- [ ] All Invoice DTOs created (Create, Update, Response, ListItem)
- [ ] InvoiceMapper with toResponse() and toListItem()
- [ ] InvoiceService with all CRUD methods
- [ ] InvoiceService event listeners implemented (CustomerNameChangedEvent, LineItemChangedEvent, PaymentRecordedEvent)
- [ ] Invoice domain events implemented (InvoiceCreatedEvent, InvoiceStatusChangedEvent, InvoiceDeletedEvent, InvoiceCustomerNameChangedEvent)
- [ ] InvoiceController with all endpoints
- [ ] **OpenAPI documentation complete for all endpoints**
- [ ] Unit tests pass for all layers (service, entity, mapper)
- [ ] Integration tests pass for all operations
- [ ] **Code coverage ≥ 80% for service layer**

**Frontend:**
- [ ] Invoice TypeScript models defined
- [ ] invoiceApi with all methods
- [ ] InvoiceFormView (create mode) working
- [ ] InvoiceFormView (edit mode) working with customer dropdown disabled
- [ ] InvoiceListView with status badges
- [ ] InvoiceDetailView with conditional action buttons
- [ ] Send invoice confirmation dialog working
- [ ] Delete invoice confirmation dialog working
- [ ] Optimistic locking errors handled gracefully
- [ ] All routes configured in App.tsx
- [ ] **ViewModel tests complete (InvoiceFormViewModel, InvoiceListViewModel, InvoiceDetailViewModel)**
- [ ] **Component tests complete (InvoiceFormView, InvoiceListView, InvoiceDetailView)**
- [ ] **E2E tests complete (create, list, view, update, send, delete flows)**

**Documentation & Testing:**
- [ ] OpenAPI spec available at /swagger-ui/index.html
- [ ] All API endpoints documented with examples
- [ ] Error responses documented
- [ ] Frontend test coverage ≥ 70%
- [ ] E2E tests run in CI/CD pipeline
- [ ] Test documentation updated

**Integration:**
- [ ] Can create invoice via UI with auto-generated number
- [ ] Can create invoice with custom number
- [ ] Can list invoices sorted by date
- [ ] Can view invoice detail
- [ ] Can edit draft invoice (not sent/paid)
- [ ] Can mark invoice as sent (only if total > 0)
- [ ] Edit button hidden when invoice is SENT or PAID
- [ ] Can delete draft or paid invoice (not sent)
- [ ] Customer draft/sent/paid counts update correctly via domain events
- [ ] Soft deletes work, audit trail preserved
- [ ] **E2E tests verify complete user workflows**

**Next Phase:**
Phase 5: LineItem CRUD - Adding line items to invoices with automatic total recalculation
