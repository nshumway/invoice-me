# Release 1.2 Planning Document

**Release Date:** TBD
**Status:** Planning
**Priority:** Medium

---

## Overview

Release 1.2 focuses on improving data consistency, removing unnecessary business rule constraints, and enhancing user navigation. This release addresses potential rollup field synchronization issues, removes restrictive payment date validation, and improves cross-entity navigation in the UI.

---

## User Stories

### User Story 1: Replace Incremental Rollup Fields with Query-Based Calculations

**Priority:** High
**Estimated Effort:** 8-12 hours
**Status:** Ready for Development

#### Problem Statement

Currently, `customer.totalOutstanding` is maintained through incremental updates (add/subtract) triggered by domain events. While this works in most cases, this approach is prone to synchronization issues:

1. **Race conditions:** Concurrent invoice/payment operations could cause lost updates despite optimistic locking retries
2. **Event processing failures:** If an event handler fails or is skipped, the rollup becomes permanently out of sync
3. **Data migration issues:** Importing historical data or fixing bugs requires manual recalculation
4. **Debugging difficulty:** When values are wrong, it's hard to trace which event caused the issue

**Current Implementation:**
```java
// InvoiceEventHandler.java - Incremental approach
customer.setTotalOutstanding(customer.getTotalOutstanding().add(invoiceTotal));    // DRAFT→SENT
customer.setTotalOutstanding(customer.getTotalOutstanding().subtract(paymentAmount)); // Payment
```

**Issue:** If any event is missed or fails, `totalOutstanding` becomes incorrect with no self-healing mechanism.

**Location:**
- `/src/main/java/com/invoiceme/application/customer/InvoiceEventHandler.java:107,216`
- `/src/main/java/com/invoiceme/domain/customer/Customer.java`

#### Acceptance Criteria

- [ ] `customer.totalOutstanding` is calculated on-demand from invoice data, not stored incrementally
- [ ] CustomerService has a method to recalculate outstanding balance from invoices
- [ ] Customer API responses include the calculated `totalOutstanding` value
- [ ] Remove event-based increment/decrement logic for `totalOutstanding`
- [ ] All existing tests pass
- [ ] New tests verify calculated values match expected amounts
- [ ] Performance is acceptable (query-based calculation doesn't slow down customer list/detail views)

#### Technical Implementation Plan

**Approach:** Replace stored rollup field with query-based calculation.

**Option A: Database View (Recommended)**
Create a database view that calculates `totalOutstanding` from invoice data:

```sql
CREATE OR REPLACE VIEW customer_statistics AS
SELECT
    c.id as customer_id,
    c.company_name,
    c.email,
    -- Invoice counts (keep as-is, these are fast to calculate)
    COALESCE((SELECT COUNT(*) FROM invoices i
              WHERE i.customer_id = c.id
              AND i.status = 'DRAFT'
              AND i.is_deleted = false), 0) as draft_invoice_count,
    COALESCE((SELECT COUNT(*) FROM invoices i
              WHERE i.customer_id = c.id
              AND i.status = 'SENT'
              AND i.is_deleted = false), 0) as sent_invoice_count,
    COALESCE((SELECT COUNT(*) FROM invoices i
              WHERE i.customer_id = c.id
              AND i.status = 'PAID'
              AND i.is_deleted = false), 0) as paid_invoice_count,
    -- Calculate total outstanding from invoice balances
    COALESCE((SELECT SUM(i.total - i.amount_paid)
              FROM invoices i
              WHERE i.customer_id = c.id
              AND i.status IN ('SENT', 'PAID')
              AND i.is_deleted = false
              AND (i.total - i.amount_paid) > 0), 0) as total_outstanding
FROM customers c
WHERE c.is_deleted = false;
```

**Option B: Repository Query Method (Alternative)**
Add a method to calculate on-the-fly in Java:

```java
// CustomerRepository.java
@Query("""
    SELECT COALESCE(SUM(i.total - i.amountPaid), 0)
    FROM Invoice i
    WHERE i.customerId = :customerId
    AND i.status IN ('SENT', 'PAID')
    AND i.isDeleted = false
    AND (i.total - i.amountPaid) > 0
    """)
BigDecimal calculateTotalOutstanding(@Param("customerId") UUID customerId);

// CustomerService.java
public CustomerResponse getCustomerById(UUID id) {
    Customer customer = customerRepository.findByIdAndIsDeletedFalse(id)
        .orElseThrow(() -> new NotFoundException("Customer not found"));

    BigDecimal totalOutstanding = customerRepository.calculateTotalOutstanding(id);

    return CustomerResponse.builder()
        .id(customer.getId())
        .companyName(customer.getCompanyName())
        .totalOutstanding(totalOutstanding) // Calculated, not stored
        .build();
}
```

**Recommended Approach: Option B (Repository Query)**

Reasons:
- Simpler to implement (no database view migration)
- Works with existing JPA setup
- Easy to add caching later if needed
- Can be tested with existing test infrastructure

**Changes Required:**

1. **Add query method to CustomerRepository:**
   - `BigDecimal calculateTotalOutstanding(UUID customerId)`
   - `Map<UUID, BigDecimal> calculateTotalOutstandingForCustomers(List<UUID> customerIds)` (for list views)

2. **Update CustomerService:**
   - Call `calculateTotalOutstanding()` when building `CustomerResponse`
   - Remove event handler logic for updating `totalOutstanding`

3. **Update Customer entity:**
   - Remove `totalOutstanding` field (no longer stored)
   - Or keep as `@Transient` for backward compatibility during migration

4. **Update InvoiceEventHandler:**
   - Remove lines 107, 216 that increment/decrement `totalOutstanding`
   - Keep event handlers for invoice counts (these are still useful)

5. **Database migration:**
   - Option 1: Drop `total_outstanding` column
   - Option 2: Keep column but stop updating it (safer rollback)

6. **Performance optimization (if needed):**
   - Add caching to `calculateTotalOutstanding()` with 5-minute TTL
   - Invalidate cache on payment/invoice events

**Testing:**

- Add test: Create invoices, verify calculated outstanding matches expected
- Add test: Record payments, verify calculated outstanding decreases correctly
- Add test: Multiple customers with various invoice states
- Add test: Performance test - loading 100 customers with statistics
- Integration test: Full invoice lifecycle, verify outstanding always correct

#### Files to Modify

**Backend:**
- `src/main/java/com/invoiceme/infrastructure/persistence/CustomerRepository.java` (add query methods)
- `src/main/java/com/invoiceme/application/customer/CustomerService.java` (use calculated values)
- `src/main/java/com/invoiceme/application/customer/InvoiceEventHandler.java` (remove increment/decrement logic)
- `src/main/java/com/invoiceme/domain/customer/Customer.java` (remove or mark @Transient)
- `src/main/resources/db/migration/V1_X__remove_total_outstanding_column.sql` (migration)

**Tests:**
- `src/test/java/com/invoiceme/infrastructure/persistence/CustomerRepositoryTest.java` (new test)
- `src/test/java/com/invoiceme/application/customer/CustomerServiceTest.java` (update expectations)
- `src/test/java/com/invoiceme/application/customer/InvoiceEventHandlerTest.java` (update/remove tests)

#### Risks & Mitigation

- **Risk:** Performance degradation on customer list view (N+1 query problem)
  - **Mitigation:** Use batch query `calculateTotalOutstandingForCustomers()` for list views
  - **Mitigation:** Add database index on `invoices(customer_id, status, is_deleted)`
  - **Mitigation:** Add caching layer if needed

- **Risk:** Breaking change if external systems rely on `totalOutstanding` field
  - **Mitigation:** Keep field in API response (just calculated differently)
  - **Mitigation:** Gradual rollout - calculate but don't remove stored field initially

- **Risk:** Increased database load
  - **Mitigation:** Monitor query performance before/after
  - **Mitigation:** Keep option to rollback to stored field approach

---

### User Story 2: Remove Payment Date Validation

**Priority:** Low
**Estimated Effort:** 1-2 hours
**Status:** Ready for Development

#### Problem Statement

Currently, the system prevents recording payments with a `paymentDate` before the invoice's `invoiceDate`. This validation is overly restrictive and doesn't reflect real-world business scenarios:

**Examples where this is problematic:**
1. **Advance payments:** Customer pays before invoice is issued
2. **Backdated invoices:** Invoice is created to document a service already paid for
3. **Data corrections:** Fixing historical records where payment was received first
4. **Deposit scenarios:** Customer pays upfront, invoice is issued later

**Current Behavior:**
```
Invoice Date: 2025-11-01
Payment Date: 2025-10-28
Result: ❌ "Payment date cannot be before invoice date"
```

**Expected Behavior:**
```
Invoice Date: 2025-11-01
Payment Date: 2025-10-28
Result: ✅ Payment recorded successfully
```

**Location:** `/src/main/java/com/invoiceme/domain/payment/Payment.java:73-78`

#### Acceptance Criteria

- [ ] Payment date validation is removed from `Payment.beforeCreate()` method
- [ ] Payments can be recorded with any date (past, present, future)
- [ ] Payment date is still required (not null)
- [ ] All existing tests pass
- [ ] Update any tests that relied on this validation

#### Technical Implementation Plan

**Changes Required:**

1. **Remove validation in Payment.java:**
   ```java
   // DELETE THESE LINES (73-78):
   // Validate payment date not before invoice date
   if (paymentDate != null && invoice.getInvoiceDate() != null) {
       if (paymentDate.isBefore(invoice.getInvoiceDate())) {
           throw new ValidationException("Payment date cannot be before invoice date");
       }
   }
   ```

2. **Update tests:**
   - Remove `PaymentTest.testPaymentDateBeforeInvoiceDateThrowsException()`
   - Or update test to verify payment date CAN be before invoice date

3. **Update API documentation:**
   - Remove note about payment date restrictions in API docs

#### Files to Modify

**Backend:**
- `src/main/java/com/invoiceme/domain/payment/Payment.java` (remove validation)

**Tests:**
- `src/test/java/com/invoiceme/domain/payment/PaymentTest.java` (remove or update test)

#### Risks & Mitigation

- **Risk:** None - this is a pure relaxation of constraints
- **Risk:** Users entering incorrect dates by mistake
  - **Mitigation:** Frontend can show a warning (not an error) if payment date is significantly before invoice date
  - **Mitigation:** Add UI confirmation: "Payment date is before invoice date. Continue?"

---

### User Story 3: Make Customer Name a Link on Invoice Pages

**Priority:** Low
**Estimated Effort:** 2-3 hours
**Status:** Ready for Development

#### Problem Statement

On the Invoice Detail page, the customer name is displayed as plain text. Users cannot quickly navigate to the customer's detail page to see their full information, payment history, or other invoices.

**Current UX:**
```
Invoice Detail Page
─────────────────
Customer: Acme Corporation      <-- Plain text, not clickable
Invoice #: INV-001
Total: $1,500.00
```

**Expected UX:**
```
Invoice Detail Page
─────────────────
Customer: [Acme Corporation]    <-- Clickable link to customer detail
Invoice #: INV-001
Total: $1,500.00
```

When clicked, should navigate to: `/customers/{customerId}`

**Location:**
- `/invoice-me-frontend/src/views/invoices/InvoiceDetailView.tsx`
- Possibly also `/invoice-me-frontend/src/components/InvoiceCard.tsx` (for consistency)

#### Acceptance Criteria

- [ ] Customer name on Invoice Detail page is a clickable link
- [ ] Link navigates to Customer Detail page (`/customers/{customerId}`)
- [ ] Link styling matches design system (uses `Link` component or styled anchor)
- [ ] Hover state shows it's clickable (underline, color change, cursor pointer)
- [ ] Accessible - screen readers announce it as a link
- [ ] Also update Invoice List view if customer name is shown there

#### Technical Implementation Plan

**Changes Required:**

1. **Update InvoiceDetailView.tsx:**
   ```tsx
   // BEFORE:
   <div className="text-gray-300">
     <span className="text-gray-500">Customer:</span> {invoice.customerName}
   </div>

   // AFTER:
   import { Link } from 'react-router-dom';

   <div className="text-gray-300">
     <span className="text-gray-500">Customer:</span>{' '}
     <Link
       to={`/customers/${invoice.customerId}`}
       className="text-primary-400 hover:text-primary-300 hover:underline transition-colors"
     >
       {invoice.customerName}
     </Link>
   </div>
   ```

2. **Ensure Invoice response includes customerId:**
   - Check that `InvoiceResponse.java` includes `customerId` field
   - Check that `Invoice.ts` frontend model includes `customerId`

3. **Update InvoiceCard.tsx (if applicable):**
   - Same link treatment if customer name is shown on invoice cards

4. **Consider adding breadcrumb:**
   ```tsx
   <nav className="text-sm mb-4">
     <Link to="/invoices" className="text-gray-400">Invoices</Link>
     {' > '}
     <Link to={`/customers/${invoice.customerId}`} className="text-primary-400">
       {invoice.customerName}
     </Link>
     {' > '}
     <span className="text-gray-300">{invoice.invoiceNumber}</span>
   </nav>
   ```

#### Files to Modify

**Frontend:**
- `invoice-me-frontend/src/views/invoices/InvoiceDetailView.tsx` (add Link)
- `invoice-me-frontend/src/components/InvoiceCard.tsx` (add Link if applicable)
- `invoice-me-frontend/src/models/Invoice.ts` (verify customerId field exists)

**Backend (verify):**
- `src/main/java/com/invoiceme/application/invoice/dto/InvoiceResponse.java` (verify customerId is included)

#### Testing

- [ ] Manual test: Click customer link on invoice detail page
- [ ] Verify navigation to correct customer detail page
- [ ] Test with multiple invoices for different customers
- [ ] Keyboard navigation works (Tab to link, Enter to follow)
- [ ] Screen reader announces as link

#### Risks & Mitigation

- **Risk:** None - this is a pure UX enhancement
- **Risk:** If `customerId` is not included in `InvoiceResponse`, will need backend change
  - **Mitigation:** Check API response first, add field if needed

---

## Release Checklist

### Pre-Development
- [ ] All user stories reviewed and prioritized
- [ ] Decision made on rollup calculation approach (Option A vs Option B)
- [ ] Verify `customerId` is available in Invoice API responses
- [ ] Dev environment set up with latest master branch

### Development
- [ ] User Story 1: Repository query methods implemented
- [ ] User Story 1: CustomerService updated to use calculated values
- [ ] User Story 1: Event handler increment/decrement logic removed
- [ ] User Story 1: Database migration created (if removing column)
- [ ] User Story 1: Performance tested with realistic data volumes
- [ ] User Story 2: Payment date validation removed
- [ ] User Story 2: Tests updated
- [ ] User Story 3: Customer link added to Invoice Detail page
- [ ] User Story 3: Verified customerId is in API response
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Code review completed

### Testing (Staging Environment)
- [ ] User Story 1: Verify calculated outstanding matches expected values
- [ ] User Story 1: Test with various invoice states (draft, sent, paid, partial payments)
- [ ] User Story 1: Performance test - load customer list with 100+ customers
- [ ] User Story 2: Verify payment can be recorded before invoice date
- [ ] User Story 2: Test advance payment scenario
- [ ] User Story 3: Verify customer link navigates correctly
- [ ] User Story 3: Test keyboard and screen reader navigation
- [ ] Regression testing: All existing features still work
- [ ] Performance testing: No degradation in load times

### Documentation
- [ ] CHANGELOG.md updated with all changes
- [ ] README.md updated if needed
- [ ] API documentation updated (payment date validation removed)
- [ ] Database migration scripts documented (if applicable)

### Deployment
- [ ] Database backup taken
- [ ] Backend deployed to staging
- [ ] Frontend deployed to staging
- [ ] Smoke tests pass on staging
- [ ] Backend deployed to production
- [ ] Frontend deployed to production
- [ ] Production smoke tests pass
- [ ] Monitor for any calculation discrepancies

### Post-Deployment
- [ ] Monitor customer `totalOutstanding` values for correctness
- [ ] Monitor query performance on customer list/detail views
- [ ] Verify payment date flexibility is working as expected
- [ ] User feedback collection on customer link navigation
- [ ] Create tickets for any issues discovered
- [ ] Retrospective meeting scheduled

---

## Dependencies

**External:**
- None (all changes are internal)

**Internal:**
- Requires existing JPA/Hibernate setup
- Requires React Router for navigation (User Story 3)

---

## Rollback Plan

### User Story 1 (Rollup Calculation)
- If performance is unacceptable:
  - Revert to event-based increment/decrement approach
  - Re-enable InvoiceEventHandler updates
  - Run data migration to recalculate all customer balances
- If database migration was applied:
  - Revert migration to restore `total_outstanding` column
  - Restore JPA field in Customer entity

### User Story 2 (Payment Date Validation)
- Simply re-add the validation code in `Payment.java`
- No data migration needed (existing payments are unaffected)

### User Story 3 (Customer Link)
- Revert frontend changes to remove Link components
- No backend changes needed

---

## Performance Considerations

### User Story 1: Query-Based Calculation

**Expected Query Pattern:**
```sql
-- Single customer detail view (1 query):
SELECT COALESCE(SUM(total - amount_paid), 0)
FROM invoices
WHERE customer_id = ?
  AND status IN ('SENT', 'PAID')
  AND is_deleted = false
  AND (total - amount_paid) > 0;

-- Customer list view (N+1 prevention):
SELECT customer_id, COALESCE(SUM(total - amount_paid), 0) as total_outstanding
FROM invoices
WHERE customer_id IN (?, ?, ?, ...)  -- Batch of customer IDs
  AND status IN ('SENT', 'PAID')
  AND is_deleted = false
  AND (total - amount_paid) > 0
GROUP BY customer_id;
```

**Recommended Indexes:**
```sql
CREATE INDEX idx_invoices_customer_status
ON invoices(customer_id, status, is_deleted)
WHERE is_deleted = false;

CREATE INDEX idx_invoices_outstanding
ON invoices(customer_id, (total - amount_paid))
WHERE status IN ('SENT', 'PAID')
  AND is_deleted = false
  AND (total - amount_paid) > 0;
```

**Expected Performance:**
- Single customer query: <10ms (with proper indexes)
- Customer list view (100 customers): <50ms (batch query)
- Acceptable for real-time calculation without caching

**Caching Strategy (if needed):**
- Use Spring Cache abstraction
- Cache key: `customer:{customerId}:totalOutstanding`
- TTL: 5 minutes
- Evict on: Payment recorded, Invoice status changed
