# Phase 6: Payment Recording (Full Stack)

## Table of Contents
- [Overview](#overview)
- [Architecture Context](#architecture-context)
- [Data Model Reference](#data-model-reference)
- [Phase 6A: Record Payment](#phase-6a-record-payment)
  - [US-67: Payment Entity & Database](#us-67-payment-entity--database)
  - [US-68: Payment DTOs & Mapper](#us-68-payment-dtos--mapper)
  - [US-69: Payment Service - Record](#us-69-payment-service---record)
  - [US-70: Payment Controller - Record](#us-70-payment-controller---record)
  - [US-71: Frontend - Record Payment Form](#us-71-frontend---record-payment-form)
- [Phase 6B: List Payments](#phase-6b-list-payments)
  - [US-72: Payment Service - List](#us-72-payment-service---list)
  - [US-73: Payment Controller - List](#us-73-payment-controller---list)
  - [US-74: Frontend - Payments Table on Invoice Detail](#us-74-frontend---payments-table-on-invoice-detail)
- [Phase 6C: View Payment Detail](#phase-6c-view-payment-detail)
  - [US-75: Payment Service - Get by ID](#us-75-payment-service---get-by-id)
  - [US-76: Payment Controller - Get by ID](#us-76-payment-controller---get-by-id)
  - [US-77: Frontend - Payment Detail View](#us-77-frontend---payment-detail-view)
- [Completion Checklist](#completion-checklist)

---

## Overview

This phase implements payment recording functionality with automatic invoice status transitions and customer statistics updates. When payments are recorded, the system automatically calculates amountPaid and transitions the invoice to PAID status when fully paid.

**Goal:** Users can record payments against invoices. The system automatically tracks payment totals and marks invoices as PAID when the balance reaches zero.

**Duration Estimate:** 2-3 days

**Important Notes:**
- **NO user-facing DELETE endpoint** - Payments cannot be deleted by users
- Payments can only be cascade-deleted when the parent invoice is deleted
- Payment.beforeDelete/delete/afterDelete exist for cascade scenarios only
- PaymentService.deleteAllForInvoice() uses isSystemUpdate=true for cascades

---

## Architecture Context

### Domain-Driven Design Patterns

**Payment Entity Responsibilities:**
- Validates business rules (payment date not before invoice date, amount > 0)
- Publishes domain events (PaymentRecordedEvent) to trigger invoice amountPaid recalculation
- Invoice status transition to PAID handled by InvoiceService listening to events
- Subscribes to InvoiceCustomerNameChangedEvent to update denormalized customerName field

**Status Transition Logic (Event-Driven):**
```
When Payment is recorded:
1. Payment publishes PaymentRecordedEvent
2. InvoiceService listens and recalculates Invoice.amountPaid (sum of all payment amounts)
3. InvoiceService checks if Invoice.amountPaid >= Invoice.total
4. If fully paid:
   - InvoiceService sets Invoice.status = PAID
   - InvoiceService publishes InvoiceStatusChangedEvent (SENT → PAID)
5. CustomerService listens to InvoiceStatusChangedEvent and updates:
   * Decrement sentInvoiceCount
   * Increment paidInvoiceCount
   * Subtract invoice.total from totalOutstanding
6. All updates happen within same transaction
```

**Key Patterns:**
- **Event-Driven Calculation:** Payment publishes PaymentRecordedEvent, InvoiceService subscribes and recalculates amountPaid
- **Event-Driven Status Transition:** Invoice publishes InvoiceStatusChangedEvent when transitioning to PAID
- **Soft Deletes:** isDeleted flag, preserves audit trail
- **No User Deletes:** Payments cannot be deleted by users (only cascade delete via InvoiceDeletedEvent)
- **Audit Trail:** createdBy, lastModifiedBy via UserContext
- **Denormalized customerName:** Updated via subscription to InvoiceCustomerNameChangedEvent

### Transaction Flow

**Record Payment Flow (Event-Driven):**
```
1. Controller receives RecordPaymentRequest
2. Controller calls PaymentService.recordPayment()
3. Service loads Invoice entity (validation)
4. Payment.beforeCreate() validates:
   - Invoice exists and is not deleted
   - Invoice status is SENT or PAID
   - Payment date is not before invoice date
   - Amount > 0
5. Payment.create() sets all fields
6. Service saves Payment
7. Payment.afterCreate() publishes PaymentRecordedEvent
8. InvoiceService listens for PaymentRecordedEvent:
   a. Recalculates Invoice.amountPaid (query sum of all payments)
   b. Updates Invoice.amountPaid
   c. Checks if Invoice.amountPaid >= Invoice.total
   d. If fully paid:
      - Sets Invoice.status = PAID
      - Publishes InvoiceStatusChangedEvent (SENT → PAID)
9. CustomerService listens for InvoiceStatusChangedEvent (SENT → PAID):
   - Decrements sentInvoiceCount
   - Increments paidInvoiceCount
   - Subtracts invoice.total from totalOutstanding
10. Transaction commits
```

**Cascade Delete Flow (Event-Driven):**
```
1. Invoice publishes InvoiceDeletedEvent
2. PaymentService listens for InvoiceDeletedEvent
3. PaymentService loads all payments for invoice
4. For each payment:
   - Payment.beforeDelete(isSystemUpdate=true) - no validation needed
   - Payment.delete() - soft delete
   - Save payment
5. Note: Don't publish PaymentRecordedEvent since invoice is being deleted
6. All updates happen within same transaction
```

---

## Testing & Documentation Requirements

All features in this phase must include comprehensive testing and API documentation:

### OpenAPI Documentation
- Complete OpenAPI 3.0 annotations for all Payment endpoints
- Document POST /api/payments (record payment)
- Document GET /api/payments/{id} (view payment detail)
- Document GET /api/invoices/{invoiceId}/payments (list payments for invoice)
- **Note**: No DELETE endpoint (payments cannot be deleted by users)

### Backend Testing
- Unit tests for Payment entity lifecycle methods
- Integration tests for amountPaid recalculation and status transitions
- Test event publishing (PaymentRecordedEvent)
- Test automatic SENT → PAID transition
- Test customer statistics updates via events
- Minimum 80% code coverage for PaymentService

### Frontend Testing

**ViewModel Tests:**
- `RecordPaymentViewModel.test.ts` - Form state, validation, balance calculation
- `PaymentDetailViewModel.test.ts` - Data loading, navigation
- Test payment date validation (not before invoice date)
- Test amount validation (must be > 0)

**Component Tests:**
- `RecordPaymentForm.test.tsx` - Form rendering, payment method dropdown, date picker
- `PaymentsTable.test.tsx` - Table rendering, running total display
- `PaymentDetailView.test.tsx` - Detail display, navigation to invoice
- Test conditional rendering (record payment button only for SENT/PAID invoices)

**E2E Tests:**
- `record-payment-full.spec.ts` - Record payment equal to invoice total, verify PAID status
- `record-payment-partial.spec.ts` - Record partial payment, verify status stays SENT
- `record-payment-overpayment.spec.ts` - Record overpayment, verify PAID status
- `view-payment-history.spec.ts` - View payment history on invoice detail
- Test validation errors (date before invoice date, zero amount)

---

## Data Model Reference

### Payment Entity Fields

| Field | Type | Description | Notes |
|-------|------|-------------|-------|
| `id` | UUID | Primary identifier | Inherited from BaseEntity |
| `invoiceId` | UUID | Foreign key to Invoice | Required, immutable |
| `paymentDate` | Instant | UTC timestamp of payment | Required |
| `amount` | BigDecimal | Payment amount | Required, must be > 0 |
| `paymentMethod` | Enum | Payment method | CASH, CHECK, CREDIT_CARD, BANK_TRANSFER, OTHER |
| `referenceNumber` | String | Check #, transaction ID, etc. | Optional |
| `notes` | String | Payment notes | Optional |

### Payment Read-Only Fields

| Field | Type | Description | Computed/System-Managed |
|-------|------|-------------|-------------------------|
| `customerName` | String | Customer company name | Denormalized from Invoice, updated via cascade |

### PaymentMethod Enum

```java
public enum PaymentMethod {
    CASH,
    CHECK,
    CREDIT_CARD,
    BANK_TRANSFER,
    OTHER
}
```

### Database Indexes

```sql
-- Index for filtering by invoice
CREATE INDEX idx_payments_invoice_id
    ON payments(invoice_id)
    WHERE is_deleted = FALSE;
```

### Validation Rules

**Record Payment:**
- invoiceId: required, must reference existing invoice
- Invoice status must be SENT or PAID
- paymentDate: required, cannot be before invoice.invoiceDate
- amount: required, must be > 0
- paymentMethod: required

**Note:** Overpayments are allowed (system does not prevent amountPaid > total)

**Delete:**
- Only via system cascade (no user-facing delete operation)

---

## Phase 6A: Record Payment

### US-67: Payment Entity & Database

**As a developer, I need Payment entity and database table so that payments can be stored**

**Acceptance Criteria:**
- Flyway migration creates payments table
- Payment entity extends BaseEntity
- PaymentMethod enum defined
- beforeCreate/create/afterCreate methods implemented
- Repository with basic CRUD methods
- Unit tests verify entity behavior

**Implementation Scaffolding:**

```sql
-- V5__create_payments_table.sql
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    payment_date TIMESTAMP NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    reference_number VARCHAR(255),
    notes TEXT,

    -- Read-only fields
    customer_name VARCHAR(255) NOT NULL,

    -- BaseEntity fields
    created_at TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    last_modified_by UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

CREATE INDEX idx_payments_invoice_id
    ON payments(invoice_id)
    WHERE is_deleted = FALSE;
```

```java
// PaymentMethod.java
public enum PaymentMethod {
    CASH,
    CHECK,
    CREDIT_CARD,
    BANK_TRANSFER,
    OTHER
}

// Payment.java
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Column(nullable = false)
    private UUID invoiceId;

    @Column(nullable = false)
    private Instant paymentDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    private String referenceNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Read-only field
    @Column(nullable = false)
    private String customerName;

    // === RECORD PAYMENT OPERATION ===

    public void beforeCreate(RecordPaymentRequest request,
                            Invoice invoice) {
        // Validate invoice exists (already loaded)
        if (invoice == null) {
            throw new NotFoundException("Invoice not found");
        }

        // Validate invoice status is SENT or PAID
        if (invoice.getStatus() != InvoiceStatus.SENT && invoice.getStatus() != InvoiceStatus.PAID) {
            throw new ValidationException("Payments can only be recorded for SENT or PAID invoices");
        }

        // Validate payment date not before invoice date
        if (request.getPaymentDate() != null && invoice.getInvoiceDate() != null) {
            if (request.getPaymentDate().isBefore(invoice.getInvoiceDate())) {
                throw new ValidationException("Payment date cannot be before invoice date");
            }
        }

        // Validate amount > 0
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Payment amount must be greater than 0");
        }

        // Validate payment method provided
        if (request.getPaymentMethod() == null) {
            throw new ValidationException("Payment method is required");
        }
    }

    public void create(RecordPaymentRequest request, Invoice invoice) {
        this.invoiceId = request.getInvoiceId();
        this.paymentDate = request.getPaymentDate();
        this.amount = request.getAmount();
        this.paymentMethod = request.getPaymentMethod();
        this.referenceNumber = request.getReferenceNumber();
        this.notes = request.getNotes();

        // Set read-only field
        this.customerName = invoice.getCustomerName();
    }

    public void afterCreate(ApplicationEventPublisher eventPublisher) {
        // Publish domain event: Payment recorded
        eventPublisher.publishEvent(new PaymentRecordedEvent(
            this.id,
            this.invoiceId,
            this.amount
        ));
    }

    // === DELETE OPERATION (System Only) ===

    public void beforeDelete(boolean isSystemUpdate) {
        // No validation needed for system deletes (cascade from invoice delete)
        // User deletes not allowed at controller level
    }

    public void delete() {
        this.markAsDeleted(); // Soft delete from BaseEntity
    }

    public void afterDelete() {
        // No cascading needed - only called during invoice delete
        // Invoice.amountPaid recalculation not needed since invoice is being deleted
    }

    // Getters/Setters...
}

// PaymentRepository.java
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdAndIsDeletedFalse(UUID id);
    List<Payment> findAllByInvoiceIdAndIsDeletedFalseOrderByPaymentDateAsc(UUID invoiceId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.invoiceId = :invoiceId AND p.isDeleted = false")
    BigDecimal sumAmountsByInvoiceId(@Param("invoiceId") UUID invoiceId);
}
```

**Testing Approach:**
```java
@Test
void testRecordPaymentSuccess() {
    // Given: SENT invoice, payment with valid date and amount
    // When: payment.create()
    // Then: payment created with all fields set
}

@Test
void testBeforeCreateRejectsDraftInvoice() {
    // Given: invoice with status DRAFT
    // When: payment.beforeCreate()
    // Then: throws ValidationException
}

@Test
void testBeforeCreateRejectsPaymentDateBeforeInvoiceDate() {
    // Given: payment date before invoice date
    // When: payment.beforeCreate()
    // Then: throws ValidationException
}

@Test
void testBeforeCreateRejectsNegativeAmount() {
    // Given: amount = -50
    // When: payment.beforeCreate()
    // Then: throws ValidationException
}
```

---

### US-68: Payment DTOs & Mapper

**As a developer, I need Payment DTOs and mapper so that API boundaries are clean**

**Acceptance Criteria:**
- RecordPaymentRequest with validation
- PaymentResponse with all fields
- PaymentMapper with toResponse() method
- Unit tests verify mapping

**Implementation Scaffolding:**

```java
// RecordPaymentRequest.java
public class RecordPaymentRequest {
    @NotNull(message = "Invoice ID is required")
    private UUID invoiceId;

    @NotNull(message = "Payment date is required")
    private Instant paymentDate;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String referenceNumber;
    private String notes;

    // Getters/Setters
}

// PaymentResponse.java
public class PaymentResponse {
    // BaseEntity fields
    private UUID id;
    private Instant createdAt;
    private UUID createdBy;
    private Instant lastModifiedAt;
    private UUID lastModifiedBy;
    private Long version;

    // Payment fields
    private UUID invoiceId;
    private Instant paymentDate;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String referenceNumber;
    private String notes;

    // Read-only field
    private String customerName;

    // Getters/Setters
}

// PaymentMapper.java
@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment entity) {
        PaymentResponse dto = new PaymentResponse();

        // Map all fields
        dto.setId(entity.getId());
        dto.setCreatedAt(entity.getCreatedAt());
        // ... map all fields

        dto.setCustomerName(entity.getCustomerName());

        return dto;
    }
}
```

---

### US-69: Payment Service - Record

**As a developer, I need PaymentService.recordPayment() so that business logic is orchestrated**

**Acceptance Criteria:**
- recordPayment(RecordPaymentRequest) method with @Transactional
- Loads Invoice entity for validation
- Follows beforeCreate → create → save → afterCreate flow
- Passes InvoiceService to afterCreate
- InvoiceService.recalculateAmountPaid() implemented
- InvoiceService.checkAndTransitionToPaid() implemented
- Unit and integration tests

**Implementation Scaffolding:**

```java
@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PaymentMapper paymentMapper;

    @Transactional
    public PaymentResponse recordPayment(RecordPaymentRequest request) {
        // Load invoice (validates existence and status)
        Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(request.getInvoiceId())
                .orElseThrow(() -> new NotFoundException("Invoice not found"));

        // Record payment
        Payment payment = new Payment();
        payment.beforeCreate(request, invoice);
        payment.create(request, invoice);
        paymentRepository.save(payment);
        payment.afterCreate(eventPublisher);  // Publishes PaymentRecordedEvent

        return paymentMapper.toResponse(payment);
    }

    // Event listener for invoice deletion (cascade delete)
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onInvoiceDeleted(InvoiceDeletedEvent event) {
        List<Payment> payments = paymentRepository.findAllByInvoiceIdAndIsDeletedFalseOrderByPaymentDateAsc(event.getInvoiceId());

        for (Payment payment : payments) {
            payment.beforeDelete(true); // isSystemUpdate=true
            payment.delete();
            paymentRepository.save(payment);
            payment.afterDelete(); // No-op since invoice is being deleted
        }
    }

    // Event listener for customer name changes from Invoice
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onInvoiceCustomerNameChanged(InvoiceCustomerNameChangedEvent event) {
        // Update denormalized customerName on all payments for this invoice
        List<Payment> payments = paymentRepository.findAllByInvoiceIdAndIsDeletedFalseOrderByPaymentDateAsc(event.getInvoiceId());

        for (Payment payment : payments) {
            payment.setCustomerName(event.getNewCustomerName());
            paymentRepository.save(payment);
        }
    }
}

// InvoiceService.java - Event listener for payment recorded
@EventListener
@Transactional(propagation = Propagation.MANDATORY)
public void onPaymentRecorded(PaymentRecordedEvent event) {
    Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(event.getInvoiceId())
            .orElseThrow(() -> new NotFoundException("Invoice not found"));

    // Recalculate amountPaid
    BigDecimal newAmountPaid = paymentRepository.sumAmountsByInvoiceId(event.getInvoiceId());
    if (newAmountPaid == null) {
        newAmountPaid = BigDecimal.ZERO;
    }

    invoice.setAmountPaid(newAmountPaid);
    invoiceRepository.save(invoice);

    // Check if should transition to PAID
    if (invoice.getStatus() == InvoiceStatus.SENT &&
        invoice.getAmountPaid().compareTo(invoice.getTotal()) >= 0) {

        InvoiceStatus oldStatus = invoice.getStatus();
        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        // Publish status change event
        eventPublisher.publishEvent(new InvoiceStatusChangedEvent(
            invoice.getId(),
            invoice.getCustomerId(),
            oldStatus,
            InvoiceStatus.PAID,
            invoice.getTotal()
        ));
    }
}

// CustomerService.java - Already has event listener for InvoiceStatusChangedEvent
// See phase4-invoice.md for the SENT → PAID transition handling
```

**Testing Approach:**
```java
@Test
void testRecordPaymentSuccess() {
    // Mock invoice repository returns SENT invoice
    // Mock payment repository save
    // Verify invoiceService.recalculateAmountPaid() called
    // Verify invoiceService.checkAndTransitionToPaid() called
}

@SpringBootTest
@Transactional
@Test
void testRecordPaymentFullyPaidTransition() {
    // Create customer
    // Create invoice with total=100, mark as SENT
    // Record payment of 100
    // Verify invoice.status = PAID
    // Verify invoice.amountPaid = 100
    // Verify customer.sentInvoiceCount decreased
    // Verify customer.paidInvoiceCount increased
    // Verify customer.totalOutstanding decreased
}

@SpringBootTest
@Transactional
@Test
void testRecordPaymentPartialPayment() {
    // Create invoice with total=100, mark as SENT
    // Record payment of 50
    // Verify invoice.status still SENT
    // Verify invoice.amountPaid = 50
}

@SpringBootTest
@Transactional
@Test
void testRecordPaymentOverpaymentAllowed() {
    // Create invoice with total=100, mark as SENT
    // Record payment of 150
    // Verify invoice.status = PAID (system allows overpayment)
    // Verify invoice.amountPaid = 150
}
```

---

### US-70: Payment Controller - Record

**As a developer, I need POST /api/payments endpoint so that payments can be recorded via REST API**

**Acceptance Criteria:**
- POST /api/payments endpoint
- Returns 201 Created on success
- Returns ApiResponse<PaymentResponse> wrapper
- Integration test with MockMvc

**Implementation Scaffolding:**

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> recordPayment(
            @Valid @RequestBody RecordPaymentRequest request) {

        PaymentResponse response = paymentService.recordPayment(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment recorded successfully", response));
    }
}

// Note: NO DELETE endpoint - payments cannot be deleted by users
```

**Testing Approach:**
```java
@Test
void testRecordPaymentSuccess() {
    // Create invoice, mark as SENT
    // POST /api/payments with valid payment data
    // Expect 201 Created
    // Verify payment recorded
    // Verify invoice.amountPaid updated
}

@Test
void testRecordPaymentInvoiceNotSent() {
    // Create DRAFT invoice
    // POST /api/payments
    // Expect 400 Bad Request
}

@Test
void testRecordPaymentDateBeforeInvoiceDate() {
    // Create invoice sent on 2025-01-15
    // POST payment with date 2025-01-10
    // Expect 400 Bad Request
}
```

---

### US-71: Frontend - Record Payment Form

**As a user, I need to record payments so that I can track received payments**

**Acceptance Criteria:**
- "Record Payment" button on invoice detail (only if status=SENT or PAID)
- Form with payment date, amount, payment method, reference #, notes
- Payment method dropdown (Cash, Check, Credit Card, Bank Transfer, Other)
- Date picker or date input
- Shows current balance due
- Submits to API
- Refreshes invoice detail after recording

**Implementation Scaffolding:**

```typescript
// models/Payment.ts
export interface Payment {
  id: string;
  invoiceId: string;
  paymentDate: string;
  amount: string;
  paymentMethod: 'CASH' | 'CHECK' | 'CREDIT_CARD' | 'BANK_TRANSFER' | 'OTHER';
  referenceNumber: string | null;
  notes: string | null;
  customerName: string;
  createdAt: string;
  createdBy: string;
  lastModifiedAt: string;
  lastModifiedBy: string;
  version: number;
}

export interface RecordPaymentRequest {
  invoiceId: string;
  paymentDate: string; // ISO format
  amount: string;
  paymentMethod: 'CASH' | 'CHECK' | 'CREDIT_CARD' | 'BANK_TRANSFER' | 'OTHER';
  referenceNumber?: string;
  notes?: string;
}

// api/paymentApi.ts
export const paymentApi = {
  record: async (request: RecordPaymentRequest): Promise<Payment> => {
    const response = await apiClient.post<ApiResponse<Payment>>('/payments', request);
    return response.data.data;
  },

  listForInvoice: async (invoiceId: string): Promise<Payment[]> => {
    const response = await apiClient.get<ApiResponse<Payment[]>>(
      `/invoices/${invoiceId}/payments`
    );
    return response.data.data;
  },

  getById: async (paymentId: string): Promise<Payment> => {
    const response = await apiClient.get<ApiResponse<Payment>>(`/payments/${paymentId}`);
    return response.data.data;
  },
};

// viewmodels/payments/RecordPaymentViewModel.ts
export const RecordPaymentViewModel = (invoice: Invoice, onSuccess: () => void) => {
  const [paymentDate, setPaymentDate] = useState(new Date().toISOString().split('T')[0]); // Today
  const [amount, setAmount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<'CASH' | 'CHECK' | 'CREDIT_CARD' | 'BANK_TRANSFER' | 'OTHER'>('CASH');
  const [referenceNumber, setReferenceNumber] = useState('');
  const [notes, setNotes] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Calculate balance due
  const balanceDue = useMemo(() => {
    const total = parseFloat(invoice.total);
    const paid = parseFloat(invoice.amountPaid);
    return (total - paid).toFixed(2);
  }, [invoice]);

  const recordMutation = useMutation({
    mutationFn: paymentApi.record,
    onSuccess: () => {
      onSuccess();
      // Reset form
      setAmount('');
      setReferenceNumber('');
      setNotes('');
    },
    onError: (error: any) => {
      setErrors({ submit: error.response?.data?.message || 'Failed to record payment' });
    },
  });

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!paymentDate) {
      newErrors.paymentDate = 'Payment date is required';
    } else {
      // Check not before invoice date
      const payDate = new Date(paymentDate);
      const invDate = new Date(invoice.invoiceDate);
      if (payDate < invDate) {
        newErrors.paymentDate = 'Payment date cannot be before invoice date';
      }
    }

    if (!amount || parseFloat(amount) <= 0) {
      newErrors.amount = 'Amount must be greater than 0';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const formData: RecordPaymentRequest = {
      invoiceId: invoice.id,
      paymentDate: new Date(paymentDate).toISOString(),
      amount,
      paymentMethod,
      referenceNumber: referenceNumber || undefined,
      notes: notes || undefined,
    };

    recordMutation.mutate(formData);
  };

  return {
    paymentDate, setPaymentDate,
    amount, setAmount,
    paymentMethod, setPaymentMethod,
    referenceNumber, setReferenceNumber,
    notes, setNotes,
    balanceDue,
    errors,
    handleSubmit,
    isSubmitting: recordMutation.isPending,
  };
};

// components/payments/RecordPaymentForm.tsx
export const RecordPaymentForm: React.FC<{
  invoice: Invoice;
  onSuccess: () => void;
  onCancel: () => void;
}> = ({ invoice, onSuccess, onCancel }) => {
  const vm = RecordPaymentViewModel(invoice, onSuccess);

  return (
    <form onSubmit={vm.handleSubmit} className="bg-gray-50 rounded p-6 space-y-4">
      <h3 className="text-xl font-semibold mb-4">Record Payment</h3>

      {/* Balance Due */}
      <div className="bg-blue-50 rounded p-4">
        <p className="text-sm text-gray-700">Balance Due:</p>
        <p className="text-2xl font-bold text-blue-700">${vm.balanceDue}</p>
      </div>

      {/* Payment Date */}
      <div>
        <label className="block text-sm font-medium mb-1">
          Payment Date <span className="text-red-500">*</span>
        </label>
        <input
          type="date"
          value={vm.paymentDate}
          onChange={(e) => vm.setPaymentDate(e.target.value)}
          className={`w-full border rounded px-3 py-2 ${
            vm.errors.paymentDate ? 'border-red-500' : 'border-gray-300'
          }`}
        />
        {vm.errors.paymentDate && (
          <p className="text-red-500 text-sm mt-1">{vm.errors.paymentDate}</p>
        )}
      </div>

      {/* Amount */}
      <div>
        <label className="block text-sm font-medium mb-1">
          Amount <span className="text-red-500">*</span>
        </label>
        <input
          type="number"
          step="0.01"
          value={vm.amount}
          onChange={(e) => vm.setAmount(e.target.value)}
          placeholder="0.00"
          className={`w-full border rounded px-3 py-2 ${
            vm.errors.amount ? 'border-red-500' : 'border-gray-300'
          }`}
        />
        {vm.errors.amount && (
          <p className="text-red-500 text-sm mt-1">{vm.errors.amount}</p>
        )}
      </div>

      {/* Payment Method */}
      <div>
        <label className="block text-sm font-medium mb-1">
          Payment Method <span className="text-red-500">*</span>
        </label>
        <select
          value={vm.paymentMethod}
          onChange={(e) => vm.setPaymentMethod(e.target.value as any)}
          className="w-full border border-gray-300 rounded px-3 py-2"
        >
          <option value="CASH">Cash</option>
          <option value="CHECK">Check</option>
          <option value="CREDIT_CARD">Credit Card</option>
          <option value="BANK_TRANSFER">Bank Transfer</option>
          <option value="OTHER">Other</option>
        </select>
      </div>

      {/* Reference Number */}
      <div>
        <label className="block text-sm font-medium mb-1">
          Reference Number
        </label>
        <input
          type="text"
          value={vm.referenceNumber}
          onChange={(e) => vm.setReferenceNumber(e.target.value)}
          placeholder="Check #, Transaction ID, etc."
          className="w-full border border-gray-300 rounded px-3 py-2"
        />
      </div>

      {/* Notes */}
      <div>
        <label className="block text-sm font-medium mb-1">Notes</label>
        <textarea
          value={vm.notes}
          onChange={(e) => vm.setNotes(e.target.value)}
          rows={3}
          className="w-full border border-gray-300 rounded px-3 py-2"
          placeholder="Additional payment details..."
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
          className="bg-green-600 text-white px-6 py-2 rounded hover:bg-green-700 disabled:opacity-50"
        >
          {vm.isSubmitting ? 'Recording...' : 'Record Payment'}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="bg-gray-300 text-gray-700 px-6 py-2 rounded hover:bg-gray-400"
        >
          Cancel
        </button>
      </div>
    </form>
  );
};
```

---

## Phase 6B: List Payments

### US-72: Payment Service - List

**As a developer, I need PaymentService.listPaymentsForInvoice() so that payments can be retrieved**

**Implementation Scaffolding:**

```java
@Transactional(readOnly = true)
public List<PaymentResponse> listPaymentsForInvoice(UUID invoiceId) {
    return paymentRepository.findAllByInvoiceIdAndIsDeletedFalseOrderByPaymentDateAsc(invoiceId)
            .stream()
            .map(paymentMapper::toResponse)
            .collect(Collectors.toList());
}

@Transactional(readOnly = true)
public PaymentResponse getPaymentById(UUID paymentId) {
    Payment payment = paymentRepository.findByIdAndIsDeletedFalse(paymentId)
            .orElseThrow(() -> new NotFoundException("Payment not found"));

    return paymentMapper.toResponse(payment);
}
```

---

### US-73: Payment Controller - List

**Implementation Scaffolding:**

```java
// Note: Using nested route under invoices for list
@RestController
@RequestMapping("/api/invoices/{invoiceId}/payments")
public class InvoicePaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> listPaymentsForInvoice(
            @PathVariable UUID invoiceId) {

        List<PaymentResponse> response = paymentService.listPaymentsForInvoice(invoiceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

// Also add getById to PaymentController
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(@PathVariable UUID id) {
    PaymentResponse response = paymentService.getPaymentById(id);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

---

### US-74: Frontend - Payments Table on Invoice Detail

**As a user, I need to see all payments on an invoice so that I can track payment history**

**Acceptance Criteria:**
- Table showing payment date, amount, method, reference #
- Displayed on invoice detail page below line items
- Shows "No payments yet" if empty
- Shows running total of payments
- Click row navigates to payment detail view
- "Record Payment" button (only if status=SENT or PAID)

**Implementation Scaffolding:**

```typescript
// Update InvoiceDetailViewModel to include payments
export const InvoiceDetailViewModel = (invoiceId: string) => {
  // ... existing invoice query

  // Query payments
  const { data: payments, refetch: refetchPayments } = useQuery({
    queryKey: ['payments', 'invoice', invoiceId],
    queryFn: () => paymentApi.listForInvoice(invoiceId),
  });

  const [showRecordPayment, setShowRecordPayment] = useState(false);

  const handlePaymentRecorded = () => {
    refetchPayments();
    queryClient.invalidateQueries({ queryKey: ['invoices', invoiceId] });
    setShowRecordPayment(false);
  };

  const handleRecordPayment = () => setShowRecordPayment(true);

  return {
    // ... existing
    payments,
    showRecordPayment,
    handleRecordPayment,
    handlePaymentRecorded,
    handleCancelRecordPayment: () => setShowRecordPayment(false),
  };
};

// components/payments/PaymentsTable.tsx
export const PaymentsTable: React.FC<{
  payments: Payment[];
  onRowClick: (id: string) => void;
}> = ({ payments, onRowClick }) => {
  if (!payments || payments.length === 0) {
    return (
      <div className="bg-gray-50 rounded p-6 text-center">
        <p className="text-gray-600">No payments recorded yet</p>
      </div>
    );
  }

  // Calculate total
  const totalPaid = payments.reduce(
    (sum, payment) => sum + parseFloat(payment.amount),
    0
  );

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <table className="w-full">
        <thead className="bg-gray-50 border-b">
          <tr>
            <th className="text-left p-4 font-medium text-gray-700">Date</th>
            <th className="text-right p-4 font-medium text-gray-700">Amount</th>
            <th className="text-left p-4 font-medium text-gray-700">Method</th>
            <th className="text-left p-4 font-medium text-gray-700">Reference</th>
          </tr>
        </thead>
        <tbody>
          {payments.map((payment) => (
            <tr
              key={payment.id}
              onClick={() => onRowClick(payment.id)}
              className="border-b hover:bg-gray-50 cursor-pointer"
            >
              <td className="p-4">
                {new Date(payment.paymentDate).toLocaleDateString()}
              </td>
              <td className="p-4 text-right font-mono font-semibold">
                ${parseFloat(payment.amount).toFixed(2)}
              </td>
              <td className="p-4">
                {payment.paymentMethod.replace('_', ' ')}
              </td>
              <td className="p-4 text-gray-600">
                {payment.referenceNumber || <span className="text-gray-400">—</span>}
              </td>
            </tr>
          ))}
        </tbody>
        <tfoot className="bg-gray-50 border-t">
          <tr>
            <td className="p-4 font-semibold">Total Paid:</td>
            <td className="p-4 text-right font-mono font-bold text-lg">
              ${totalPaid.toFixed(2)}
            </td>
            <td colSpan={2}></td>
          </tr>
        </tfoot>
      </table>
    </div>
  );
};
```

---

## Phase 6C: View Payment Detail

### US-75: Payment Service - Get by ID

**Already implemented in US-72**

---

### US-76: Payment Controller - Get by ID

**Already implemented in US-73**

---

### US-77: Frontend - Payment Detail View

**As a user, I need to view payment details so that I can see full payment information**

**Acceptance Criteria:**
- Shows all payment fields (date, amount, method, reference, notes)
- Shows associated invoice information
- Link back to invoice
- No edit/delete buttons (payments cannot be modified)

**Implementation Scaffolding:**

```typescript
// viewmodels/payments/PaymentDetailViewModel.ts
export const PaymentDetailViewModel = (paymentId: string) => {
  const navigate = useNavigate();

  const { data: payment, isLoading, isError } = useQuery({
    queryKey: ['payments', paymentId],
    queryFn: () => paymentApi.getById(paymentId),
  });

  const handleViewInvoice = () => {
    if (payment) {
      navigate(`/invoices/${payment.invoiceId}`);
    }
  };

  return {
    payment,
    isLoading,
    isError,
    handleViewInvoice,
  };
};

// views/payments/PaymentDetailView.tsx
export const PaymentDetailView: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const vm = PaymentDetailViewModel(id!);

  if (vm.isLoading) {
    return <div className="p-6">Loading payment...</div>;
  }

  if (vm.isError || !vm.payment) {
    return <div className="p-6 text-red-600">Payment not found</div>;
  }

  return (
    <div className="max-w-3xl mx-auto p-6">
      <h1 className="text-3xl font-bold mb-6">Payment Details</h1>

      <div className="bg-white rounded-lg shadow p-6 space-y-6">
        {/* Payment Date */}
        <div>
          <label className="text-sm text-gray-600">Payment Date</label>
          <p className="text-lg font-semibold">
            {new Date(vm.payment.paymentDate).toLocaleDateString()}
          </p>
        </div>

        {/* Amount */}
        <div>
          <label className="text-sm text-gray-600">Amount</label>
          <p className="text-2xl font-bold text-green-600">
            ${parseFloat(vm.payment.amount).toFixed(2)}
          </p>
        </div>

        {/* Payment Method */}
        <div>
          <label className="text-sm text-gray-600">Payment Method</label>
          <p className="text-lg">{vm.payment.paymentMethod.replace('_', ' ')}</p>
        </div>

        {/* Reference Number */}
        {vm.payment.referenceNumber && (
          <div>
            <label className="text-sm text-gray-600">Reference Number</label>
            <p className="text-lg font-mono">{vm.payment.referenceNumber}</p>
          </div>
        )}

        {/* Notes */}
        {vm.payment.notes && (
          <div>
            <label className="text-sm text-gray-600">Notes</label>
            <p className="text-lg">{vm.payment.notes}</p>
          </div>
        )}

        {/* Customer Name */}
        <div>
          <label className="text-sm text-gray-600">Customer</label>
          <p className="text-lg">{vm.payment.customerName}</p>
        </div>

        {/* Audit Info */}
        <div className="border-t pt-4 space-y-2">
          <p className="text-sm text-gray-600">
            Recorded: {new Date(vm.payment.createdAt).toLocaleString()}
          </p>
          <p className="text-sm text-gray-600">
            Last Modified: {new Date(vm.payment.lastModifiedAt).toLocaleString()}
          </p>
        </div>

        {/* Actions */}
        <div className="flex gap-3 border-t pt-4">
          <button
            onClick={vm.handleViewInvoice}
            className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700"
          >
            View Invoice
          </button>
        </div>
      </div>
    </div>
  );
};
```

---

## Completion Checklist

**Backend:**
- [ ] Payment entity with beforeCreate/create/afterCreate implemented
- [ ] Payment entity with beforeDelete/delete/afterDelete for cascades
- [ ] Payment domain event implemented (PaymentRecordedEvent)
- [ ] PaymentMethod enum defined
- [ ] payments table created with Flyway migration
- [ ] PaymentRepository with query methods including sumAmounts
- [ ] All Payment DTOs created (RecordPayment, Response)
- [ ] PaymentMapper with toResponse()
- [ ] PaymentService.recordPayment() implemented
- [ ] PaymentService event listeners implemented (InvoiceDeletedEvent, InvoiceCustomerNameChangedEvent)
- [ ] InvoiceService event listener for PaymentRecordedEvent
- [ ] InvoiceService handles SENT → PAID transition via event
- [ ] CustomerService handles InvoiceStatusChangedEvent for SENT → PAID
- [ ] PaymentController with record and getById (NO DELETE endpoint)
- [ ] InvoicePaymentController for list payments
- [ ] **OpenAPI documentation complete for all endpoints**
- [ ] Unit tests pass for all layers
- [ ] Integration tests pass for status transition via events
- [ ] **Code coverage ≥ 80% for service layer**

**Frontend:**
- [ ] Payment TypeScript models defined
- [ ] paymentApi with record, list, getById methods
- [ ] RecordPaymentForm component working
- [ ] PaymentsTable component on invoice detail
- [ ] PaymentDetailView showing all payment info
- [ ] Invoice detail page shows payments section
- [ ] Invoice total and status update after payment recorded
- [ ] "Record Payment" button only shows for SENT/PAID invoices
- [ ] All routes configured in App.tsx
- [ ] **ViewModel tests complete (RecordPaymentViewModel, PaymentDetailViewModel)**
- [ ] **Component tests complete (RecordPaymentForm, PaymentsTable, PaymentDetailView)**
- [ ] **E2E tests complete (full payment, partial payment, overpayment flows)**

**Documentation & Testing:**
- [ ] OpenAPI documentation includes payment method enum
- [ ] Error responses documented (date validation, amount validation)
- [ ] Frontend test coverage ≥ 70%
- [ ] E2E tests verify automatic PAID status transition

**Integration:**
- [ ] Can record payment for SENT invoice
- [ ] Cannot record payment for DRAFT invoice
- [ ] Invoice.amountPaid recalculates via PaymentRecordedEvent
- [ ] Invoice transitions to PAID automatically when fully paid (via event)
- [ ] Customer counts update via InvoiceStatusChangedEvent
- [ ] Partial payments work correctly (status stays SENT)
- [ ] Overpayments allowed (status becomes PAID)
- [ ] Payment date validation (not before invoice date)
- [ ] Payments cascade deleted via InvoiceDeletedEvent
- [ ] Customer name updates propagate via events
- [ ] Payments cannot be deleted by users (no delete button/endpoint)
- [ ] Audit trail preserved
- [ ] **E2E tests verify complete payment workflows**

**Application Complete:**
- [ ] All CRUD operations working for all entities
- [ ] Event-driven architecture working (no circular dependencies)
- [ ] Status transitions working (DRAFT → SENT → PAID) via events
- [ ] Domain events published and subscribed correctly
- [ ] All validation rules enforced
- [ ] Optimistic locking working
- [ ] Soft deletes working everywhere
- [ ] Audit trail complete
- [ ] **OpenAPI documentation complete for entire application**
- [ ] **All unit tests passing (backend ≥ 80% coverage)**
- [ ] **All integration tests passing**
- [ ] **All frontend tests passing (≥ 70% coverage)**
- [ ] **All E2E tests passing in CI/CD**
- [ ] **E2E test: Complete workflow from customer creation to invoice paid**
