# Phase 5: LineItem CRUD (Full Stack)

## Table of Contents
- [Overview](#overview)
- [Architecture Context](#architecture-context)
- [Data Model Reference](#data-model-reference)
- [Phase 5A: Create LineItem](#phase-5a-create-lineitem)
  - [US-53: LineItem Entity & Database](#us-53-lineitem-entity--database)
  - [US-54: LineItem DTOs & Mapper](#us-54-lineitem-dtos--mapper)
  - [US-55: LineItem Service - Create](#us-55-lineitem-service---create)
  - [US-56: LineItem Controller - Create](#us-56-lineitem-controller---create)
  - [US-57: Frontend - Add LineItem to Invoice](#us-57-frontend---add-lineitem-to-invoice)
- [Phase 5B: List LineItems](#phase-5b-list-lineitems)
  - [US-58: LineItem Service - List](#us-58-lineitem-service---list)
  - [US-59: LineItem Controller - List](#us-59-lineitem-controller---list)
  - [US-60: Frontend - LineItems Table on Invoice Detail](#us-60-frontend---lineitems-table-on-invoice-detail)
- [Phase 5C: Update LineItem](#phase-5c-update-lineitem)
  - [US-61: LineItem Service - Update](#us-61-lineitem-service---update)
  - [US-62: LineItem Controller - Update](#us-62-lineitem-controller---update)
  - [US-63: Frontend - Edit LineItem Inline](#us-63-frontend---edit-lineitem-inline)
- [Phase 5D: Delete LineItem](#phase-5d-delete-lineitem)
  - [US-64: LineItem Service - Delete](#us-64-lineitem-service---delete)
  - [US-65: LineItem Controller - Delete](#us-65-lineitem-controller---delete)
  - [US-66: Frontend - Delete LineItem](#us-66-frontend---delete-lineitem)
- [Completion Checklist](#completion-checklist)

---

## Overview

This phase implements complete LineItem CRUD functionality with automatic invoice total recalculation. Line items can only be added/modified/deleted when the parent invoice is in DRAFT status.

**Goal:** Users can add, edit, and remove line items from invoices. The invoice total automatically updates based on line item changes.

**Duration Estimate:** 2-3 days

---

## Architecture Context

### Domain-Driven Design Patterns

**LineItem Entity Responsibilities:**
- Calculates lineTotal (quantity × unitPrice) on create/update
- Validates business rules (positive quantity, positive unitPrice)
- Publishes domain events (LineItemChangedEvent) to trigger invoice total recalculation
- Subscribes to CustomerNameChangedEvent to update denormalized customerName field

**Key Patterns:**
- **Automatic Calculation:** lineTotal = quantity × unitPrice
- **Event-Driven Recalculation:** LineItem publishes LineItemChangedEvent, InvoiceService subscribes and recalculates total
- **Status Validation:** LineItems can only be modified when Invoice.status = DRAFT
- **Soft Deletes:** isDeleted flag, preserves audit trail
- **Audit Trail:** createdBy, lastModifiedBy via UserContext
- **Denormalized customerName:** Updated via subscription to CustomerNameChangedEvent

### Transaction Flow

**Create LineItem Flow:**
```
1. Controller receives CreateLineItemRequest
2. Controller calls LineItemService.createLineItem()
3. Service loads Invoice entity (validation)
4. LineItem.beforeCreate() validates invoice is DRAFT
5. LineItem.create() sets fields, calculates lineTotal
6. Service saves LineItem
7. LineItem.afterCreate() publishes LineItemChangedEvent
8. InvoiceService listens for LineItemChangedEvent and recalculates:
   - Queries all line items, sums lineTotals
   - Updates Invoice.total
9. Transaction commits
```

**Invoice Total Recalculation (Event-Driven):**
```
LineItem publishes LineItemChangedEvent containing:
- invoiceId
- changeType (CREATED, UPDATED, DELETED)

InvoiceService subscribes with @EventListener and:
SELECT SUM(line_total)
FROM line_items
WHERE invoice_id = ? AND is_deleted = FALSE

Updates Invoice.total
```

**Event-Driven Architecture for CustomerName Updates:**
```
When Customer.companyName changes:
1. Customer publishes CustomerNameChangedEvent
2. InvoiceService listens and updates all invoices for that customer
3. InvoiceService publishes InvoiceCustomerNameChangedEvent for each invoice
4. LineItemService listens and updates all line items for affected invoices
5. PaymentService listens and updates all payments for affected invoices
6. All updates happen within same transaction
```

---

## Data Model Reference

### LineItem Entity Fields

| Field | Type | Description | Notes |
|-------|------|-------------|-------|
| `id` | UUID | Primary identifier | Inherited from BaseEntity |
| `invoiceId` | UUID | Foreign key to Invoice | Required, immutable |
| `description` | String | Product/service description | Required |
| `quantity` | BigDecimal | Quantity | Required, must be > 0 |
| `unitPrice` | BigDecimal | Price per unit | Required, must be > 0 |

### LineItem Read-Only Fields

| Field | Type | Description | Computed/System-Managed |
|-------|------|-------------|-------------------------|
| `customerName` | String | Customer company name | Denormalized from Invoice, updated via cascade |
| `lineTotal` | BigDecimal | quantity × unitPrice | Calculated on create/update |

### Database Indexes

```sql
-- Index for filtering by invoice
CREATE INDEX idx_line_items_invoice_id
    ON line_items(invoice_id)
    WHERE is_deleted = FALSE;
```

### Validation Rules

**Create:**
- invoiceId: required, must reference existing invoice
- Invoice status must be DRAFT
- description: required, not blank
- quantity: required, must be > 0
- unitPrice: required, must be > 0

**Update:**
- Invoice status must be DRAFT (unless isSystemUpdate=true)
- description: required, not blank
- quantity: required, must be > 0
- unitPrice: required, must be > 0

**Delete:**
- Invoice status must be DRAFT

---

## Phase 5A: Create LineItem

### US-53: LineItem Entity & Database

**As a developer, I need LineItem entity and database table so that line items can be stored**

**Acceptance Criteria:**
- Flyway migration creates line_items table
- LineItem entity extends BaseEntity
- beforeCreate/create/afterCreate methods implemented
- lineTotal calculation logic works
- Repository with basic CRUD methods
- Unit tests verify entity behavior

**Implementation Scaffolding:**

```sql
-- V4__create_line_items_table.sql
CREATE TABLE line_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    description TEXT NOT NULL,
    quantity DECIMAL(19, 4) NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,

    -- Read-only fields
    customer_name VARCHAR(255) NOT NULL,
    line_total DECIMAL(19, 2) NOT NULL,

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

CREATE INDEX idx_line_items_invoice_id
    ON line_items(invoice_id)
    WHERE is_deleted = FALSE;
```

```java
// LineItem.java
@Entity
@Table(name = "line_items")
public class LineItem extends BaseEntity {

    @Column(nullable = false)
    private UUID invoiceId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    // Read-only fields
    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    // === CREATE OPERATION ===

    public void beforeCreate(CreateLineItemRequest request,
                            Invoice invoice) {
        // Validate invoice exists (already loaded)
        if (invoice == null) {
            throw new NotFoundException("Invoice not found");
        }

        // Validate invoice is DRAFT
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ValidationException("Can only add line items to DRAFT invoices");
        }

        // Validate quantity > 0
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Quantity must be greater than 0");
        }

        // Validate unitPrice > 0
        if (request.getUnitPrice() == null || request.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Unit price must be greater than 0");
        }

        // Validate description not blank
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new ValidationException("Description is required");
        }
    }

    public void create(CreateLineItemRequest request, Invoice invoice) {
        this.invoiceId = request.getInvoiceId();
        this.description = request.getDescription();
        this.quantity = request.getQuantity();
        this.unitPrice = request.getUnitPrice();

        // Set read-only fields
        this.customerName = invoice.getCustomerName();
        this.lineTotal = calculateLineTotal(this.quantity, this.unitPrice);
    }

    public void afterCreate(ApplicationEventPublisher eventPublisher) {
        // Publish domain event: Line item changed
        eventPublisher.publishEvent(new LineItemChangedEvent(
            this.invoiceId,
            this.id,
            LineItemChangeType.CREATED
        ));
    }

    private BigDecimal calculateLineTotal(BigDecimal quantity, BigDecimal unitPrice) {
        return quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    // Getters/Setters...
}

// LineItemRepository.java
@Repository
public interface LineItemRepository extends JpaRepository<LineItem, UUID> {
    Optional<LineItem> findByIdAndIsDeletedFalse(UUID id);
    List<LineItem> findAllByInvoiceIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID invoiceId);

    @Query("SELECT SUM(l.lineTotal) FROM LineItem l WHERE l.invoiceId = :invoiceId AND l.isDeleted = false")
    BigDecimal sumLineTotalsByInvoiceId(@Param("invoiceId") UUID invoiceId);
}
```

**Testing Approach:**
```java
@Test
void testCreateLineItemCalculatesTotal() {
    // Given: quantity=5, unitPrice=10.50
    // When: lineItem.create()
    // Then: lineTotal = 52.50
}

@Test
void testBeforeCreateRejectsNonDraftInvoice() {
    // Given: invoice with status SENT
    // When: lineItem.beforeCreate()
    // Then: throws ValidationException
}

@Test
void testBeforeCreateRejectsNegativeQuantity() {
    // Given: quantity = -5
    // When: lineItem.beforeCreate()
    // Then: throws ValidationException
}
```

---

### US-54: LineItem DTOs & Mapper

**As a developer, I need LineItem DTOs and mapper so that API boundaries are clean**

**Acceptance Criteria:**
- CreateLineItemRequest with validation
- UpdateLineItemRequest with validation
- LineItemResponse with all fields
- LineItemMapper with toResponse() method
- Unit tests verify mapping

**Implementation Scaffolding:**

```java
// CreateLineItemRequest.java
public class CreateLineItemRequest {
    @NotNull(message = "Invoice ID is required")
    private UUID invoiceId;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    private BigDecimal unitPrice;

    // Getters/Setters
}

// UpdateLineItemRequest.java
public class UpdateLineItemRequest {
    @NotNull(message = "LineItem ID is required")
    private UUID id;

    @NotNull(message = "Version is required")
    private Long version;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
    private BigDecimal quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    private BigDecimal unitPrice;

    // Getters/Setters
}

// LineItemResponse.java
public class LineItemResponse {
    // BaseEntity fields
    private UUID id;
    private Instant createdAt;
    private UUID createdBy;
    private Instant lastModifiedAt;
    private UUID lastModifiedBy;
    private Long version;

    // LineItem fields
    private UUID invoiceId;
    private String description;
    private BigDecimal quantity;
    private BigDecimal unitPrice;

    // Read-only fields
    private String customerName;
    private BigDecimal lineTotal;

    // Getters/Setters
}

// LineItemMapper.java
@Component
public class LineItemMapper {

    public LineItemResponse toResponse(LineItem entity) {
        LineItemResponse dto = new LineItemResponse();

        // Map all fields
        dto.setId(entity.getId());
        dto.setCreatedAt(entity.getCreatedAt());
        // ... map all fields

        dto.setLineTotal(entity.getLineTotal());

        return dto;
    }
}
```

---

### US-55: LineItem Service - Create

**As a developer, I need LineItemService.createLineItem() so that business logic is orchestrated**

**Acceptance Criteria:**
- createLineItem(CreateLineItemRequest) method with @Transactional
- Loads Invoice entity for validation
- Follows beforeCreate → create → save → afterCreate flow
- Passes InvoiceService to afterCreate for total recalculation
- Unit and integration tests

**Implementation Scaffolding:**

```java
@Service
public class LineItemService {

    @Autowired
    private LineItemRepository lineItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private LineItemMapper lineItemMapper;

    @Transactional
    public LineItemResponse createLineItem(CreateLineItemRequest request) {
        // Load invoice (validates existence and status)
        Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(request.getInvoiceId())
                .orElseThrow(() -> new NotFoundException("Invoice not found"));

        // Create line item
        LineItem lineItem = new LineItem();
        lineItem.beforeCreate(request, invoice);
        lineItem.create(request, invoice);
        lineItemRepository.save(lineItem);
        lineItem.afterCreate(eventPublisher);  // Publishes LineItemChangedEvent

        return lineItemMapper.toResponse(lineItem);
    }

    // Event listener for customer name changes from Invoice
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onInvoiceCustomerNameChanged(InvoiceCustomerNameChangedEvent event) {
        // Update denormalized customerName on all line items for this invoice
        List<LineItem> lineItems = lineItemRepository.findAllByInvoiceIdAndIsDeletedFalseOrderByCreatedAtAsc(event.getInvoiceId());

        for (LineItem lineItem : lineItems) {
            lineItem.setCustomerName(event.getNewCustomerName());
            lineItemRepository.save(lineItem);
        }
    }
}

// InvoiceService.java - Event listener for line item changes
@EventListener
@Transactional(propagation = Propagation.MANDATORY)
public void onLineItemChanged(LineItemChangedEvent event) {
    Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(event.getInvoiceId())
            .orElseThrow(() -> new NotFoundException("Invoice not found"));

    // Query sum of all line items
    BigDecimal newTotal = lineItemRepository.sumLineTotalsByInvoiceId(event.getInvoiceId());
    if (newTotal == null) {
        newTotal = BigDecimal.ZERO;
    }

    invoice.setTotal(newTotal);
    invoiceRepository.save(invoice);
}

// InvoiceService.java - Publish event when customerName changes (for downstream updates)
@EventListener
@Transactional(propagation = Propagation.MANDATORY)
public void onCustomerNameChanged(CustomerNameChangedEvent event) {
    // Update denormalized customerName on all invoices for this customer
    List<Invoice> invoices = invoiceRepository.findAllByCustomerIdAndIsDeletedFalse(event.getCustomerId());

    for (Invoice invoice : invoices) {
        invoice.setCustomerName(event.getNewCompanyName());
        invoiceRepository.save(invoice);

        // Publish event for downstream entities (LineItems, Payments)
        eventPublisher.publishEvent(new InvoiceCustomerNameChangedEvent(
            invoice.getId(),
            event.getNewCompanyName()
        ));
    }
}
```

**Testing Approach:**
```java
@Test
void testCreateLineItemSuccess() {
    // Mock invoice repository returns DRAFT invoice
    // Mock line item repository save
    // Verify invoiceService.recalculateInvoiceTotal() called
}

@SpringBootTest
@Transactional
@Test
void testCreateLineItemIntegration() {
    // Create customer
    // Create invoice (total = 0)
    // Create line item (quantity=2, unitPrice=50)
    // Verify line item created with lineTotal=100
    // Verify invoice.total = 100
}
```

---

### US-56: LineItem Controller - Create

**As a developer, I need POST /api/invoices/:invoiceId/line-items endpoint**

**Acceptance Criteria:**
- POST /api/invoices/:invoiceId/line-items endpoint
- Returns 201 Created on success
- Returns ApiResponse<LineItemResponse> wrapper
- Integration test with MockMvc

**Implementation Scaffolding:**

```java
@RestController
@RequestMapping("/api/invoices/{invoiceId}/line-items")
public class LineItemController {

    @Autowired
    private LineItemService lineItemService;

    @PostMapping
    public ResponseEntity<ApiResponse<LineItemResponse>> createLineItem(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody CreateLineItemRequest request) {

        // Ensure invoiceId in path matches request
        if (!invoiceId.equals(request.getInvoiceId())) {
            throw new ValidationException("Invoice ID mismatch");
        }

        LineItemResponse response = lineItemService.createLineItem(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Line item added successfully", response));
    }
}
```

**Testing Approach:**
```java
@Test
void testCreateLineItemSuccess() {
    // Create invoice
    // POST /api/invoices/{id}/line-items with description, quantity, unitPrice
    // Expect 201 Created
    // Verify line item created
    // Verify invoice total updated
}

@Test
void testCreateLineItemInvoiceNotDraft() {
    // Create invoice, mark as SENT
    // POST /api/invoices/{id}/line-items
    // Expect 400 Bad Request
}
```

---

### US-57: Frontend - Add LineItem to Invoice

**As a user, I need to add line items to invoices so that I can itemize charges**

**Acceptance Criteria:**
- "Add Line Item" button on invoice detail (only if status=DRAFT)
- Inline form or modal to add line item
- Fields: description, quantity, unit price
- Auto-calculates and shows line total
- Submits to API
- Refreshes line items list and invoice total after creation

**Implementation Scaffolding:**

```typescript
// models/LineItem.ts
export interface LineItem {
  id: string;
  invoiceId: string;
  description: string;
  quantity: string; // BigDecimal as string
  unitPrice: string;
  customerName: string;
  lineTotal: string;
  createdAt: string;
  createdBy: string;
  lastModifiedAt: string;
  lastModifiedBy: string;
  version: number;
}

export interface CreateLineItemRequest {
  invoiceId: string;
  description: string;
  quantity: string;
  unitPrice: string;
}

// api/lineItemApi.ts
export const lineItemApi = {
  create: async (request: CreateLineItemRequest): Promise<LineItem> => {
    const response = await apiClient.post<ApiResponse<LineItem>>(
      `/invoices/${request.invoiceId}/line-items`,
      request
    );
    return response.data.data;
  },

  listForInvoice: async (invoiceId: string): Promise<LineItem[]> => {
    const response = await apiClient.get<ApiResponse<LineItem[]>>(
      `/invoices/${invoiceId}/line-items`
    );
    return response.data.data;
  },
};

// viewmodels/invoices/AddLineItemViewModel.ts
export const AddLineItemViewModel = (invoiceId: string, onSuccess: () => void) => {
  const [description, setDescription] = useState('');
  const [quantity, setQuantity] = useState('');
  const [unitPrice, setUnitPrice] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Calculate line total for preview
  const lineTotal = useMemo(() => {
    const qty = parseFloat(quantity);
    const price = parseFloat(unitPrice);
    if (!isNaN(qty) && !isNaN(price)) {
      return (qty * price).toFixed(2);
    }
    return '0.00';
  }, [quantity, unitPrice]);

  const createMutation = useMutation({
    mutationFn: lineItemApi.create,
    onSuccess: () => {
      onSuccess();
      // Reset form
      setDescription('');
      setQuantity('');
      setUnitPrice('');
    },
    onError: (error: any) => {
      setErrors({ submit: error.response?.data?.message || 'Failed to add line item' });
    },
  });

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!description.trim()) {
      newErrors.description = 'Description is required';
    }

    if (!quantity || parseFloat(quantity) <= 0) {
      newErrors.quantity = 'Quantity must be greater than 0';
    }

    if (!unitPrice || parseFloat(unitPrice) <= 0) {
      newErrors.unitPrice = 'Unit price must be greater than 0';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validate()) return;

    const formData: CreateLineItemRequest = {
      invoiceId,
      description,
      quantity,
      unitPrice,
    };

    createMutation.mutate(formData);
  };

  return {
    description, setDescription,
    quantity, setQuantity,
    unitPrice, setUnitPrice,
    lineTotal,
    errors,
    handleSubmit,
    isSubmitting: createMutation.isPending,
  };
};

// components/invoices/AddLineItemForm.tsx
export const AddLineItemForm: React.FC<{ invoiceId: string; onSuccess: () => void }> = ({
  invoiceId,
  onSuccess,
}) => {
  const vm = AddLineItemViewModel(invoiceId, onSuccess);

  return (
    <form onSubmit={vm.handleSubmit} className="bg-gray-50 rounded p-4 space-y-4">
      <h3 className="font-semibold">Add Line Item</h3>

      <div className="grid grid-cols-3 gap-4">
        {/* Description */}
        <div className="col-span-3 sm:col-span-1">
          <label className="block text-sm font-medium mb-1">Description</label>
          <input
            type="text"
            value={vm.description}
            onChange={(e) => vm.setDescription(e.target.value)}
            placeholder="Product or service"
            className={`w-full border rounded px-3 py-2 ${
              vm.errors.description ? 'border-red-500' : 'border-gray-300'
            }`}
          />
          {vm.errors.description && (
            <p className="text-red-500 text-sm mt-1">{vm.errors.description}</p>
          )}
        </div>

        {/* Quantity */}
        <div>
          <label className="block text-sm font-medium mb-1">Quantity</label>
          <input
            type="number"
            step="0.01"
            value={vm.quantity}
            onChange={(e) => vm.setQuantity(e.target.value)}
            className={`w-full border rounded px-3 py-2 ${
              vm.errors.quantity ? 'border-red-500' : 'border-gray-300'
            }`}
          />
          {vm.errors.quantity && (
            <p className="text-red-500 text-sm mt-1">{vm.errors.quantity}</p>
          )}
        </div>

        {/* Unit Price */}
        <div>
          <label className="block text-sm font-medium mb-1">Unit Price</label>
          <input
            type="number"
            step="0.01"
            value={vm.unitPrice}
            onChange={(e) => vm.setUnitPrice(e.target.value)}
            placeholder="0.00"
            className={`w-full border rounded px-3 py-2 ${
              vm.errors.unitPrice ? 'border-red-500' : 'border-gray-300'
            }`}
          />
          {vm.errors.unitPrice && (
            <p className="text-red-500 text-sm mt-1">{vm.errors.unitPrice}</p>
          )}
        </div>
      </div>

      {/* Line Total Preview */}
      <div className="bg-blue-50 rounded p-3">
        <span className="text-sm font-medium">Line Total: </span>
        <span className="text-lg font-bold text-blue-700">${vm.lineTotal}</span>
      </div>

      {/* Submit Error */}
      {vm.errors.submit && (
        <div className="bg-red-50 border border-red-300 rounded p-3">
          <p className="text-red-700 text-sm">{vm.errors.submit}</p>
        </div>
      )}

      {/* Actions */}
      <button
        type="submit"
        disabled={vm.isSubmitting}
        className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
      >
        {vm.isSubmitting ? 'Adding...' : 'Add Line Item'}
      </button>
    </form>
  );
};
```

---

## Phase 5B: List LineItems

### US-58: LineItem Service - List

**As a developer, I need LineItemService.listLineItemsForInvoice() so that line items can be retrieved**

**Implementation Scaffolding:**

```java
@Transactional(readOnly = true)
public List<LineItemResponse> listLineItemsForInvoice(UUID invoiceId) {
    return lineItemRepository.findAllByInvoiceIdAndIsDeletedFalseOrderByCreatedAtAsc(invoiceId)
            .stream()
            .map(lineItemMapper::toResponse)
            .collect(Collectors.toList());
}
```

---

### US-59: LineItem Controller - List

**Implementation Scaffolding:**

```java
@GetMapping
public ResponseEntity<ApiResponse<List<LineItemResponse>>> listLineItemsForInvoice(
        @PathVariable UUID invoiceId) {

    List<LineItemResponse> response = lineItemService.listLineItemsForInvoice(invoiceId);
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

---

### US-60: Frontend - LineItems Table on Invoice Detail

**As a user, I need to see all line items on an invoice so that I can review charges**

**Acceptance Criteria:**
- Table showing description, quantity, unit price, line total
- Displayed on invoice detail page
- Shows "No line items yet" if empty
- Shows running total at bottom
- Edit/Delete buttons per row (only if invoice status=DRAFT)

**Implementation Scaffolding:**

```typescript
// Update InvoiceDetailViewModel to include line items
export const InvoiceDetailViewModel = (invoiceId: string) => {
  // ... existing invoice query

  // Query line items
  const { data: lineItems, refetch: refetchLineItems } = useQuery({
    queryKey: ['lineItems', 'invoice', invoiceId],
    queryFn: () => lineItemApi.listForInvoice(invoiceId),
  });

  const handleLineItemAdded = () => {
    refetchLineItems();
    // Also refetch invoice to get updated total
    queryClient.invalidateQueries({ queryKey: ['invoices', invoiceId] });
  };

  return {
    // ... existing
    lineItems,
    handleLineItemAdded,
  };
};

// components/invoices/LineItemsTable.tsx
export const LineItemsTable: React.FC<{
  lineItems: LineItem[];
  canEdit: boolean;
  onEdit: (id: string) => void;
  onDelete: (id: string) => void;
}> = ({ lineItems, canEdit, onEdit, onDelete }) => {
  if (!lineItems || lineItems.length === 0) {
    return (
      <div className="bg-gray-50 rounded p-6 text-center">
        <p className="text-gray-600">No line items yet</p>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow overflow-hidden">
      <table className="w-full">
        <thead className="bg-gray-50 border-b">
          <tr>
            <th className="text-left p-4 font-medium text-gray-700">Description</th>
            <th className="text-right p-4 font-medium text-gray-700">Quantity</th>
            <th className="text-right p-4 font-medium text-gray-700">Unit Price</th>
            <th className="text-right p-4 font-medium text-gray-700">Line Total</th>
            {canEdit && <th className="text-right p-4 font-medium text-gray-700">Actions</th>}
          </tr>
        </thead>
        <tbody>
          {lineItems.map((item) => (
            <tr key={item.id} className="border-b">
              <td className="p-4">{item.description}</td>
              <td className="p-4 text-right font-mono">{parseFloat(item.quantity).toFixed(2)}</td>
              <td className="p-4 text-right font-mono">${parseFloat(item.unitPrice).toFixed(2)}</td>
              <td className="p-4 text-right font-mono font-semibold">
                ${parseFloat(item.lineTotal).toFixed(2)}
              </td>
              {canEdit && (
                <td className="p-4 text-right space-x-2">
                  <button
                    onClick={() => onEdit(item.id)}
                    className="text-blue-600 hover:underline"
                  >
                    Edit
                  </button>
                  <button
                    onClick={() => onDelete(item.id)}
                    className="text-red-600 hover:underline"
                  >
                    Delete
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
```

---

## Phase 5C: Update LineItem

### US-61: LineItem Service - Update

**As a developer, I need LineItemService.updateLineItem() so that line items can be modified**

**Acceptance Criteria:**
- updateLineItem(UpdateLineItemRequest, isSystemUpdate) method
- Validates invoice status=DRAFT (unless isSystemUpdate=true)
- Recalculates lineTotal
- Triggers invoice total recalculation
- Handles optimistic locking

**Implementation Scaffolding:**

```java
// LineItem.java
public void beforeUpdate(UpdateLineItemRequest request,
                        Invoice invoice,
                        boolean isSystemUpdate) {
    // If user update, must be DRAFT
    if (!isSystemUpdate && invoice.getStatus() != InvoiceStatus.DRAFT) {
        throw new ValidationException("Can only edit line items on DRAFT invoices");
    }

    // Validate quantity > 0
    if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
        throw new ValidationException("Quantity must be greater than 0");
    }

    // Validate unitPrice > 0
    if (request.getUnitPrice() == null || request.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
        throw new ValidationException("Unit price must be greater than 0");
    }

    // Validate description not blank
    if (request.getDescription() == null || request.getDescription().isBlank()) {
        throw new ValidationException("Description is required");
    }
}

public void update(UpdateLineItemRequest request) {
    this.description = request.getDescription();
    this.quantity = request.getQuantity();
    this.unitPrice = request.getUnitPrice();

    // Recalculate lineTotal
    this.lineTotal = calculateLineTotal(this.quantity, this.unitPrice);
}

public void afterUpdate(ApplicationEventPublisher eventPublisher) {
    // Publish domain event: Line item changed
    eventPublisher.publishEvent(new LineItemChangedEvent(
        this.invoiceId,
        this.id,
        LineItemChangeType.UPDATED
    ));
}

// LineItemService.java
@Transactional
public LineItemResponse updateLineItem(UpdateLineItemRequest request, boolean isSystemUpdate) {
    LineItem lineItem = lineItemRepository.findByIdAndIsDeletedFalse(request.getId())
            .orElseThrow(() -> new NotFoundException("Line item not found"));

    // Optimistic locking
    if (!lineItem.getVersion().equals(request.getVersion())) {
        throw new OptimisticLockException("Line item was modified by another user");
    }

    // Load invoice for validation
    Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(lineItem.getInvoiceId())
            .orElseThrow(() -> new NotFoundException("Invoice not found"));

    lineItem.beforeUpdate(request, invoice, isSystemUpdate);
    lineItem.update(request);
    lineItemRepository.save(lineItem);
    lineItem.afterUpdate(eventPublisher);  // Publishes LineItemChangedEvent

    return lineItemMapper.toResponse(lineItem);
}

// Public method for user updates
@Transactional
public LineItemResponse updateLineItem(UpdateLineItemRequest request) {
    return updateLineItem(request, false);
}

// Note: Customer name updates are handled via InvoiceCustomerNameChangedEvent
// See onInvoiceCustomerNameChanged() event listener above
```

---

### US-62: LineItem Controller - Update

**Implementation Scaffolding:**

```java
@PutMapping("/{lineItemId}")
public ResponseEntity<ApiResponse<LineItemResponse>> updateLineItem(
        @PathVariable UUID invoiceId,
        @PathVariable UUID lineItemId,
        @Valid @RequestBody UpdateLineItemRequest request) {

    // Ensure IDs match
    if (!lineItemId.equals(request.getId())) {
        throw new ValidationException("Line item ID mismatch");
    }

    LineItemResponse response = lineItemService.updateLineItem(request);
    return ResponseEntity.ok(ApiResponse.success("Line item updated successfully", response));
}
```

---

### US-63: Frontend - Edit LineItem Inline

**As a user, I need to edit line items so that I can correct mistakes**

**Acceptance Criteria:**
- Click "Edit" button makes row editable
- Shows input fields for description, quantity, unit price
- Auto-calculates line total preview
- "Save" and "Cancel" buttons
- Updates line item and refreshes invoice total

**Implementation Scaffolding:**

```typescript
// Similar to create, but with edit mode state management
// Can use inline editing or modal approach
// Key: Pass version field for optimistic locking
// After successful update, refetch line items and invoice
```

---

## Phase 5D: Delete LineItem

### US-64: LineItem Service - Delete

**As a developer, I need LineItemService.deleteLineItem() so that line items can be removed**

**Acceptance Criteria:**
- deleteLineItem(lineItemId, version, isSystemUpdate) method
- Validates invoice status=DRAFT (unless isSystemUpdate=true)
- Soft deletes line item
- Triggers invoice total recalculation
- Handles optimistic locking

**Implementation Scaffolding:**

```java
// LineItem.java
public void beforeDelete(Invoice invoice, boolean isSystemUpdate) {
    // If user delete, must be DRAFT
    if (!isSystemUpdate && invoice.getStatus() != InvoiceStatus.DRAFT) {
        throw new ValidationException("Can only delete line items from DRAFT invoices");
    }
}

public void delete() {
    this.markAsDeleted(); // Soft delete from BaseEntity
}

public void afterDelete(ApplicationEventPublisher eventPublisher) {
    // Publish domain event: Line item changed
    eventPublisher.publishEvent(new LineItemChangedEvent(
        this.invoiceId,
        this.id,
        LineItemChangeType.DELETED
    ));
}

// LineItemService.java
@Transactional
public void deleteLineItem(UUID lineItemId, Long version, boolean isSystemUpdate) {
    LineItem lineItem = lineItemRepository.findByIdAndIsDeletedFalse(lineItemId)
            .orElseThrow(() -> new NotFoundException("Line item not found"));

    // Optimistic locking
    if (!lineItem.getVersion().equals(version)) {
        throw new OptimisticLockException("Line item was modified by another user");
    }

    // Load invoice for validation
    Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(lineItem.getInvoiceId())
            .orElseThrow(() -> new NotFoundException("Invoice not found"));

    lineItem.beforeDelete(invoice, isSystemUpdate);
    lineItem.delete();
    lineItemRepository.save(lineItem);
    lineItem.afterDelete(eventPublisher);  // Publishes LineItemChangedEvent
}

// Public method for user deletes
@Transactional
public void deleteLineItem(UUID lineItemId, Long version) {
    deleteLineItem(lineItemId, version, false);
}

// Event listener for invoice deletion (cascade delete)
@EventListener
@Transactional(propagation = Propagation.MANDATORY)
public void onInvoiceDeleted(InvoiceDeletedEvent event) {
    List<LineItem> lineItems = lineItemRepository.findAllByInvoiceIdAndIsDeletedFalseOrderByCreatedAtAsc(event.getInvoiceId());

    for (LineItem lineItem : lineItems) {
        lineItem.beforeDelete(null, true); // isSystemUpdate=true, skip invoice check
        lineItem.delete();
        lineItemRepository.save(lineItem);
        // Note: Don't publish LineItemChangedEvent since invoice is being deleted
    }
}
```

---

### US-65: LineItem Controller - Delete

**Implementation Scaffolding:**

```java
@DeleteMapping("/{lineItemId}")
public ResponseEntity<ApiResponse<Void>> deleteLineItem(
        @PathVariable UUID invoiceId,
        @PathVariable UUID lineItemId,
        @RequestParam Long version) {

    lineItemService.deleteLineItem(lineItemId, version);
    return ResponseEntity.ok(ApiResponse.success("Line item deleted successfully", null));
}
```

---

### US-66: Frontend - Delete LineItem

**As a user, I need to delete line items so that I can remove unwanted charges**

**Acceptance Criteria:**
- "Delete" button on each line item row (only if invoice status=DRAFT)
- Confirmation dialog before deleting
- Deletes line item and refreshes invoice total

**Implementation Scaffolding:**

```typescript
// api/lineItemApi.ts
export const lineItemApi = {
  // ... existing methods

  delete: async (invoiceId: string, lineItemId: string, version: number): Promise<void> => {
    await apiClient.delete(
      `/invoices/${invoiceId}/line-items/${lineItemId}?version=${version}`
    );
  },
};

// In InvoiceDetailViewModel
const deleteMutation = useMutation({
  mutationFn: ({ invoiceId, lineItemId, version }: {
    invoiceId: string;
    lineItemId: string;
    version: number
  }) => lineItemApi.delete(invoiceId, lineItemId, version),
  onSuccess: () => {
    refetchLineItems();
    queryClient.invalidateQueries({ queryKey: ['invoices', invoiceId] });
  },
});

const handleDeleteLineItem = (lineItem: LineItem) => {
  if (window.confirm('Delete this line item?')) {
    deleteMutation.mutate({
      invoiceId: invoiceId,
      lineItemId: lineItem.id,
      version: lineItem.version,
    });
  }
};
```

---

## Completion Checklist

**Backend:**
- [ ] LineItem entity with full lifecycle methods implemented
- [ ] line_items table created with Flyway migration
- [ ] LineItemRepository with query methods including sumLineTotals
- [ ] All LineItem DTOs created
- [ ] LineItemMapper with toResponse()
- [ ] LineItemService with CRUD methods
- [ ] LineItemService cascade methods (updateCustomerName, deleteAllForInvoice)
- [ ] InvoiceService.recalculateInvoiceTotal() implemented
- [ ] LineItemController with all endpoints
- [ ] Unit tests pass for all layers
- [ ] Integration tests pass for total recalculation

**Frontend:**
- [ ] LineItem TypeScript models defined
- [ ] lineItemApi with all methods
- [ ] AddLineItemForm component working
- [ ] LineItemsTable component with edit/delete
- [ ] Invoice detail page shows line items
- [ ] Invoice total updates after line item changes
- [ ] Optimistic locking errors handled

**Integration:**
- [ ] Can add line item to DRAFT invoice
- [ ] Cannot add line item to SENT/PAID invoice
- [ ] Invoice total recalculates after create/update/delete
- [ ] Can edit line item (quantity, price, description)
- [ ] Can delete line item
- [ ] Line items soft deleted when invoice deleted
- [ ] Audit trail preserved

**Next Phase:**
Phase 6: Payment CRUD - Recording payments with automatic invoice status transitions to PAID
