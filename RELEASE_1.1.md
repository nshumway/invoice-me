# Release 1.1 Planning Document

**Release Date:** TBD
**Status:** Planning
**Priority:** High

---

## Overview

Release 1.1 focuses on critical bug fixes discovered in production and user experience improvements for the authentication flow and overall application styling. This release addresses payment calculation issues, improves first-time user experience, and establishes consistent UI/UX patterns across the application.

---

## User Stories

### User Story 1: Fix Customer Outstanding Balance Calculation

**Priority:** Critical
**Estimated Effort:** 4-6 hours
**Status:** Ready for Development

#### Problem Statement
When a customer has an invoice and makes a partial payment, the customer's `totalOutstanding` field is not updated to reflect the partial payment. The outstanding balance only updates when an invoice transitions between statuses (DRAFT → SENT or SENT → PAID), not when payments are recorded against SENT invoices.

**Current Behavior:**
1. Invoice for $60 is sent → Customer `totalOutstanding` = $60 ✓
2. First payment of $30 recorded → Customer `totalOutstanding` = $60 ❌ (should be $30)
3. Second payment of $30 recorded → Invoice transitions to PAID → Customer `totalOutstanding` = $0 ✓

**Root Cause:**
The customer statistics update logic in `InvoiceEventHandler.onInvoiceStatusChanged()` uses a **transaction-based model** (add full invoice total on SENT, subtract full invoice total on PAID) rather than an **incremental model** that adjusts as payments come in.

**Location:** `/src/main/java/com/invoiceme/application/customer/InvoiceEventHandler.java:98-117`

#### Acceptance Criteria
- [ ] Customer `totalOutstanding` accurately reflects unpaid invoice balances in real-time
- [ ] Partial payments immediately reduce the customer's outstanding balance
- [ ] Customer list view shows correct outstanding amounts
- [ ] Customer detail view shows correct outstanding amounts
- [ ] All existing tests pass
- [ ] New integration tests verify partial payment scenarios

#### Technical Implementation Plan

**Approach:** Change from status-transition-based updates to payment-event-based updates.

**Changes Required:**

1. **Add new event listener in InvoiceEventHandler:**
   ```java
   @EventListener
   @Transactional(propagation = Propagation.MANDATORY)
   @Retryable(...)
   public void onPaymentRecorded(PaymentRecordedEvent event) {
       // Load invoice to get current balance
       Invoice invoice = invoiceRepository.findById(event.getInvoiceId());
       Customer customer = customerRepository.findById(invoice.getCustomerId());

       // Calculate new outstanding for this invoice: total - amountPaid
       BigDecimal invoiceBalance = invoice.getTotal().subtract(invoice.getAmountPaid());

       // Calculate previous balance (before this payment)
       BigDecimal previousInvoiceBalance = invoiceBalance.add(event.getAmount());

       // Adjust customer total: subtract the payment amount
       customer.setTotalOutstanding(
           customer.getTotalOutstanding().subtract(event.getAmount())
       );

       customerRepository.save(customer);
   }
   ```

2. **Modify existing status change handler:**
   - Keep DRAFT → SENT logic (add full invoice total)
   - **Remove** SENT → PAID logic that subtracts invoice total
   - When SENT → PAID, the outstanding should already be zero from payments
   - Add validation: log warning if outstanding != 0 when transitioning to PAID

3. **Add database migration (if needed):**
   - Create script to recalculate all customer `totalOutstanding` values based on current invoice balances
   - Run this as part of the deployment to fix any existing inconsistencies

4. **Testing:**
   - Add integration test: Create invoice, send it, make partial payments, verify customer balance updates
   - Add test: Multiple invoices for same customer with various payment states
   - Add test: Full payment in one transaction still works correctly
   - Add test: Edge case - payment recorded but invoice stays SENT (balance should still update)

#### Files to Modify
- `src/main/java/com/invoiceme/application/customer/InvoiceEventHandler.java`
- `src/test/java/com/invoiceme/application/customer/InvoiceEventHandlerTest.java`
- `src/test/java/com/invoiceme/integration/PaymentIntegrationTest.java` (new)

#### Risks & Mitigation
- **Risk:** Race conditions with concurrent payments
  - **Mitigation:** Already using `@Retryable` with optimistic locking
- **Risk:** Existing customers may have incorrect `totalOutstanding` values
  - **Mitigation:** Include data migration script to recalculate from scratch

---

### User Story 2: Fix Invoice Status Not Transitioning to PAID After Multiple Payments

**Priority:** Critical
**Estimated Effort:** TBD (Under Investigation)
**Status:** Investigation in Progress

#### Problem Statement
When an invoice is paid in multiple payments (e.g., $30 + $30 = $60), the invoice status does not automatically transition from SENT to PAID after the second payment, even though the full amount has been received.

**Current Behavior:**
1. Invoice for $60 is sent with status = SENT
2. First payment of $30 recorded → Status stays SENT ✓ (expected, not fully paid)
3. Second payment of $30 recorded → Status stays SENT ❌ (should transition to PAID)

**Expected Behavior:**
- Single payment of $60 → Status transitions to PAID ✓ (works correctly)
- Two payments of $30 each → Status should transition to PAID after second payment ❌ (fails)

#### Root Cause (Hypothesis - Under Investigation)
The automatic status transition logic in `InvoiceService.onPaymentRecorded()` relies on a database query to sum all payments. There may be a **transaction flush timing issue** where the newly saved payment is not visible to the JPQL query that runs immediately after in the same transaction.

**Location:** `/src/main/java/com/invoiceme/application/invoice/InvoiceService.java:298-350`

**Key Code Sequence:**
```java
// PaymentService.recordPayment() (line 88-89)
paymentRepository.save(payment);           // Save to JPA session
payment.afterCreate(eventPublisher);       // Publish event synchronously

// InvoiceService.onPaymentRecorded() (line 309)
BigDecimal newAmountPaid = paymentRepository.sumAmountsByInvoiceId(invoiceId); // JPQL query
```

**Hypothesis:**
The JPQL query at line 309 may not see the payment that was just saved in the same transaction because:
1. The payment is saved to the JPA persistence context but not yet flushed
2. The event is published synchronously (same transaction, no commit yet)
3. The `@Query` JPQL executes directly against the database
4. The database doesn't yet have the new payment record
5. The sum query returns only previous payments, not the current one

#### Acceptance Criteria
- [ ] Invoice transitions to PAID status when `amountPaid >= total`, regardless of number of payments
- [ ] Works correctly for 1 payment, 2 payments, 3+ payments
- [ ] Payment recording is atomic (no partial state)
- [ ] All existing tests pass
- [ ] New tests verify multi-payment scenarios

#### Investigation Tasks
- [ ] Add debug logging to trace payment saves and query results
- [ ] Test with explicit `entityManager.flush()` before the query
- [ ] Check JPA/Hibernate session state during event handling
- [ ] Verify transaction boundaries and propagation settings
- [ ] Test alternative: Use JPA entity navigation instead of native query
- [ ] Test alternative: Defer status check to after transaction commit
- [ ] Reproduce issue in integration test with multiple payments

#### Potential Solutions (To Be Validated)

**Option A: Force Flush Before Query**
```java
// In InvoiceService.onPaymentRecorded()
entityManager.flush(); // Force JPA to sync to database
BigDecimal newAmountPaid = paymentRepository.sumAmountsByInvoiceId(invoiceId);
```

**Option B: Use Entity Navigation Instead of Query**
```java
// Load all payment entities (from JPA session, not DB)
List<Payment> payments = paymentRepository.findAllByInvoiceId(invoiceId);
BigDecimal newAmountPaid = payments.stream()
    .map(Payment::getAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

**Option C: Check Status After Transaction Commit**
- Move status transition logic to a new `@TransactionalEventListener(phase = AFTER_COMMIT)`
- Would require publishing a different event or using Spring's transaction synchronization

**Option D: Maintain Denormalized amountPaid in Real-Time**
- Update invoice.amountPaid directly in PaymentService instead of via event
- Event listener only handles status transition check

#### Files to Investigate
- `src/main/java/com/invoiceme/application/invoice/InvoiceService.java:298-350`
- `src/main/java/com/invoiceme/application/payment/PaymentService.java:56-99`
- `src/main/java/com/invoiceme/infrastructure/persistence/PaymentRepository.java:31-32`
- `src/test/java/com/invoiceme/integration/PaymentIntegrationTest.java`

#### Next Steps
1. Create comprehensive integration test that reproduces the issue
2. Add detailed logging to trace query results and JPA session state
3. Test each potential solution in isolation
4. Select solution based on performance, maintainability, and architectural fit
5. Update technical implementation plan once root cause is confirmed

---

### User Story 3: Add Loading Indicator for Authentication Cold Start

**Priority:** High
**Estimated Effort:** 2-3 hours
**Status:** Ready for Development

#### Problem Statement
The application is deployed on Render's free tier, which has a "cold start" delay of 30-60 seconds when the backend service spins up after inactivity. When users attempt to log in or sign up during a cold start, they experience a long delay with no feedback, leading to confusion about whether the application is working.

**Current User Experience:**
1. User navigates to login page
2. User enters credentials and clicks "Log In"
3. Button shows "Logging In..." spinner
4. **30-60 second delay with no additional feedback**
5. User doesn't know if the app is broken, their connection failed, or if they should wait

#### Acceptance Criteria
- [ ] Login and signup forms display informative message about potential delay
- [ ] Message appears **before** user attempts to log in/sign up (proactive, not reactive)
- [ ] Message is visible but not intrusive
- [ ] Message clearly explains the delay is due to free hosting and is normal
- [ ] Message provides approximate wait time expectation (30-60 seconds)
- [ ] Message only appears in production environment (not in local development)
- [ ] UI remains accessible and professional

#### Technical Implementation Plan

**Approach:** Add an informational alert/banner to the login and signup views that explains the potential cold start delay.

**Design Recommendations:**

1. **Visual Style:**
   - Use info/warning color scheme (blue or yellow, not red error)
   - Small, unobtrusive banner below the form title
   - Icon: Info icon (ⓘ) or clock icon
   - Dismissible or always visible (recommend always visible)

2. **Message Content:**
   ```
   ⓘ First login may take 30-60 seconds while the server starts up.
   This is normal for free hosting services.
   ```

   Alternative (more concise):
   ```
   ⓘ Please be patient: First request may take up to 60 seconds due to
   server startup on our free hosting tier.
   ```

3. **Placement Options:**
   - **Option A (Recommended):** Below the "Log In" / "Sign Up" title, above the form
   - **Option B:** Above the submit button, below the form fields
   - **Option C:** As a small footnote below the submit button

4. **Environment Detection:**
   ```typescript
   // In a config or utils file
   export const isProduction = import.meta.env.PROD;
   export const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';
   export const showColdStartWarning = API_BASE_URL.includes('render.com');
   ```

**Example Implementation:**

```tsx
// LoginView.tsx
export const LoginView: React.FC = () => {
  const vm = LoginViewModel();

  return (
    <div className="min-h-screen bg-gray-900 dark:bg-gray-950 flex items-center justify-center p-6">
      <div className="max-w-md w-full bg-gray-800 dark:bg-gray-900 rounded-lg shadow-xl border border-gray-700 p-8">
        <h1 className="text-3xl font-bold text-center mb-2 text-gray-100">Log In</h1>

        {/* NEW: Cold start warning */}
        {import.meta.env.PROD && (
          <div className="mb-6 bg-blue-900/30 border border-blue-700/50 rounded-lg p-3 flex items-start gap-2">
            <svg className="w-5 h-5 text-blue-400 mt-0.5 flex-shrink-0" /* info icon SVG */>
              {/* Icon path */}
            </svg>
            <p className="text-sm text-blue-200">
              <strong>Please note:</strong> First login may take 30-60 seconds
              while the server starts up. This is normal for free hosting.
            </p>
          </div>
        )}

        <form onSubmit={vm.handleSubmit} className="space-y-4">
          {/* Rest of form */}
        </form>
      </div>
    </div>
  );
};
```

**Additional Enhancement (Optional):**
- After clicking submit, if request takes >5 seconds, show additional message:
  ```
  "Still loading... The server is waking up. Thanks for your patience!"
  ```
- Use a timeout to detect slow requests and provide progressive feedback

#### Files to Modify
- `invoice-me-frontend/src/views/auth/LoginView.tsx`
- `invoice-me-frontend/src/views/auth/SignupView.tsx`
- `invoice-me-frontend/src/config/environment.ts` (new - for environment detection)
- `invoice-me-frontend/src/viewmodels/auth/LoginViewModel.ts` (optional - for progressive feedback)
- `invoice-me-frontend/src/viewmodels/auth/SignupViewModel.ts` (optional - for progressive feedback)

#### Testing Checklist
- [ ] Message appears on login page in production build
- [ ] Message appears on signup page in production build
- [ ] Message does NOT appear in local development
- [ ] Message does not interfere with form validation errors
- [ ] Message is readable and appropriately styled with dark theme
- [ ] Message does not cause layout shift
- [ ] Mobile responsive (message wraps properly on small screens)

#### Design Review Questions
1. Should the message be dismissible? (Recommendation: No - users may forget and be confused on subsequent visits)
2. Should we add a "Learn more" link explaining free tier limitations?
3. Should we show a different message during the actual wait vs. before submission?
4. Do we want to track cold start frequency/duration for potential tier upgrade decision?

---

### User Story 4: Improve CSS and UI Consistency Across Application

**Priority:** High
**Estimated Effort:** 32-48 hours (4-6 days)
**Status:** Ready for Development

#### Problem Statement
Based on a comprehensive frontend audit, the application has significant CSS and UI inconsistencies that negatively impact user experience, accessibility, and maintainability. Issues include missing focus states (WCAG violations), broken mobile layouts, duplicate styling systems, and inconsistent design patterns.

**Impact:**
- **Accessibility:** Keyboard users cannot navigate forms (missing focus indicators)
- **Mobile UX:** Unusable layouts on small screens (4-column grids, overflowing tables)
- **Maintainability:** 3 different status color implementations, hardcoded colors
- **User Experience:** Jarring theme switches between list (dark) and detail (light) views
- **Developer Experience:** No design system, inline styles everywhere

#### Key Issues Identified

##### 1. **Critical - Accessibility Violations (WCAG 2.1)**
- **CustomerFormView:** 12+ inputs missing focus rings
- **RecordPaymentForm:** 4 inputs missing focus states
- **All forms:** Inconsistent focus indicator styles
- **Impact:** Keyboard users cannot see which field is active

##### 2. **Critical - Mobile Layout Failures**
- **CustomerDetailView:** 4-column grid on mobile = 97.5px per column (unreadable)
- **CustomerFormView:** 3-column grid on mobile
- **InvoiceDetailView:** 2-3 column grids with no responsive breakpoints
- **Tables:** All tables (LineItems, Payments, CustomerList) overflow on mobile
- **Impact:** Application is unusable on phones

##### 3. **High - Inconsistent Status Colors**
```typescript
// THREE different implementations found:

// InvoiceListView - Dark backgrounds
DRAFT: "bg-gray-600/50 text-gray-200"
SENT: "bg-blue-600/50 text-blue-200"
PAID: "bg-green-600/50 text-green-200"

// InvoiceDetailView - Light backgrounds
DRAFT: "bg-gray-100 text-gray-800"
SENT: "bg-blue-100 text-blue-800"
PAID: "bg-green-100 text-green-800"

// InvoiceCard - Different light variant
DRAFT: "bg-yellow-100 text-yellow-800"
SENT: "bg-blue-100 text-blue-800"
PAID: "bg-green-100 text-green-800"
```

##### 4. **High - Inconsistent Button Styling**
- **Sizes:** px-3 py-1, px-4 py-2, px-6 py-3 (3 different sizes)
- **Disabled states:** 3 different implementations
- **Colors:** Hardcoded blue, green, red throughout
- **Count:** ~35 buttons with inline styles, no shared component

##### 5. **Medium - Dark/Light Theme Inconsistency**
- **List views:** bg-gray-900 (dark)
- **Detail views:** bg-gray-50 (light)
- **Forms:** Mixed dark/light
- **Impact:** Disorienting navigation between views

##### 6. **Medium - Hardcoded Colors**
```typescript
// Examples found:
border-blue-500    // 15+ occurrences
text-red-400       // 12+ occurrences
bg-green-600       // 8+ occurrences
ring-blue-500      // 8+ focus rings

// Should use theme variables:
border-primary-500
text-error-400
bg-success-600
ring-primary-500
```

##### 7. **Low - Inconsistent Spacing**
- **Padding:** p-3, p-4, p-6, p-8 scattered throughout (68 usages)
- **Gaps:** gap-2, gap-4, gap-6, gap-8 (no standardized scale)
- **Margins:** Inconsistent vertical spacing between sections

#### Acceptance Criteria

**Accessibility:**
- [ ] All form inputs have visible focus indicators (ring-2 ring-primary-500)
- [ ] Focus indicators meet WCAG 2.1 contrast requirements (3:1 minimum)
- [ ] Keyboard navigation works seamlessly across all forms
- [ ] Screen reader labels are present and descriptive

**Mobile Responsiveness:**
- [ ] All grid layouts use responsive breakpoints (sm:, md:, lg:)
- [ ] No layout exceeds viewport width on 320px screens
- [ ] Tables use horizontal scroll or card layout on mobile
- [ ] Forms stack vertically on mobile (single column)
- [ ] Touch targets are minimum 44x44px

**Design Consistency:**
- [ ] Single status badge component used everywhere
- [ ] Single button component with size variants (sm, md, lg)
- [ ] Consistent theme: all views use same background color scheme
- [ ] Centralized color palette in tailwind.config.js
- [ ] Consistent spacing scale (4px base unit)

**Code Quality:**
- [ ] Reusable component library for common elements
- [ ] No hardcoded colors (use theme variables)
- [ ] Consistent Tailwind class usage (no custom CSS)
- [ ] Component documentation for design system

#### Technical Implementation Plan

**Phase 1: Establish Design System Foundation (8 hours)**

1. **Create Tailwind Theme Configuration:**
   ```javascript
   // tailwind.config.js
   module.exports = {
     theme: {
       extend: {
         colors: {
           primary: {
             50: '#eff6ff',
             500: '#3b82f6',
             600: '#2563eb',
             700: '#1d4ed8',
           },
           success: { /* green */ },
           warning: { /* yellow */ },
           error: { /* red */ },
           neutral: { /* gray */ },
         },
         spacing: {
           // Standardize: 4px base (1), 8px (2), 12px (3), 16px (4), etc.
         },
       },
     },
   };
   ```

2. **Create Shared Component Library:**
   ```
   /src/components/shared/
   ├── Button.tsx           // Variants: primary, secondary, danger, sizes: sm, md, lg
   ├── Input.tsx            // Standardized input with focus styles
   ├── Select.tsx           // Standardized select with focus styles
   ├── Badge.tsx            // Status badges with consistent styling
   ├── Card.tsx             // Container component
   └── FormField.tsx        // Label + input + error wrapper
   ```

3. **Create Style Guide Documentation:**
   ```
   /docs/STYLE_GUIDE.md     // Component usage, color palette, spacing scale
   ```

**Phase 2: Fix Critical Accessibility Issues (6 hours)**

1. **Add Focus Indicators to All Forms:**
   - CustomerFormView: 12 inputs
   - InvoiceFormModal: 2 inputs
   - LineItemFormModal: 4 inputs
   - RecordPaymentForm: 4 inputs
   - LoginView, SignupView: 2-4 inputs each

2. **Standardized Focus Style:**
   ```typescript
   className="focus:ring-2 focus:ring-primary-500 focus:border-transparent outline-none"
   ```

3. **Test keyboard navigation flow in all forms**

**Phase 3: Fix Critical Mobile Responsiveness (10 hours)**

1. **Convert Grid Layouts to Responsive:**
   ```tsx
   // Before:
   <div className="grid grid-cols-4 gap-4">

   // After:
   <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
   ```

2. **Make Tables Responsive:**
   - Option A: Horizontal scroll container
   - Option B: Card layout on mobile (recommended for better UX)
   ```tsx
   // Mobile: Card layout
   // Desktop: Table layout
   const isMobile = useMediaQuery('(max-width: 640px)');
   ```

3. **Update Forms to Single Column on Mobile:**
   - CustomerFormView, InvoiceFormModal, etc.

**Phase 4: Standardize Component Styling (12 hours)**

1. **Replace All Buttons with Button Component:**
   - Search for: `<button` (35+ instances)
   - Replace with: `<Button variant="primary" size="md">`
   - Test all button interactions

2. **Replace All Status Badges:**
   - Create `<StatusBadge status={invoice.status} />` component
   - Replace 3 different implementations
   - Ensure consistent appearance

3. **Replace Inline Form Inputs:**
   - Use `<Input>` and `<Select>` components
   - Ensures consistent focus, validation, and error styling

**Phase 5: Theme Consistency (6 hours)**

1. **Standardize Background Colors:**
   - Decision: Keep dark theme or switch to light?
   - **Recommendation:** Full dark theme for consistency
   - Update all views to use: `bg-gray-900`, `bg-gray-800` cards

2. **Remove Hardcoded Colors:**
   - Find: `border-blue-500` → Replace: `border-primary-500`
   - Find: `text-red-400` → Replace: `text-error-400`
   - Find: `bg-green-600` → Replace: `bg-success-600`

**Phase 6: Testing & QA (6 hours)**

1. **Visual Regression Testing:**
   - Screenshot before/after for each view
   - Ensure no visual regressions

2. **Accessibility Testing:**
   - Run axe DevTools audit
   - Test keyboard navigation
   - Test screen reader compatibility

3. **Responsive Testing:**
   - Test on: 320px (iPhone SE), 375px (iPhone), 768px (iPad), 1024px (desktop)
   - Verify all layouts work correctly

4. **Cross-browser Testing:**
   - Chrome, Firefox, Safari
   - Test form interactions

#### Files to Modify (Estimated)

**New Files:**
- `invoice-me-frontend/src/components/shared/Button.tsx`
- `invoice-me-frontend/src/components/shared/Input.tsx`
- `invoice-me-frontend/src/components/shared/Select.tsx`
- `invoice-me-frontend/src/components/shared/Badge.tsx`
- `invoice-me-frontend/src/components/shared/Card.tsx`
- `invoice-me-frontend/src/components/shared/FormField.tsx`
- `invoice-me-frontend/src/hooks/useMediaQuery.ts`
- `invoice-me-frontend/docs/STYLE_GUIDE.md`

**Modified Files (High Priority):**
- `invoice-me-frontend/tailwind.config.js`
- `invoice-me-frontend/src/views/customers/CustomerFormView.tsx`
- `invoice-me-frontend/src/views/customers/CustomerDetailView.tsx`
- `invoice-me-frontend/src/views/customers/CustomerListView.tsx`
- `invoice-me-frontend/src/views/invoices/InvoiceDetailView.tsx`
- `invoice-me-frontend/src/views/invoices/InvoiceListView.tsx`
- `invoice-me-frontend/src/components/invoices/InvoiceCard.tsx`
- `invoice-me-frontend/src/components/payments/RecordPaymentForm.tsx`
- `invoice-me-frontend/src/components/payments/PaymentsTable.tsx`
- `invoice-me-frontend/src/components/lineitems/LineItemsTable.tsx`
- `invoice-me-frontend/src/views/auth/LoginView.tsx`
- `invoice-me-frontend/src/views/auth/SignupView.tsx`

**Additional Files (Medium Priority):**
- All remaining views and components (20-30 files)

#### Design Decisions Needed Before Development

1. **Theme Direction:**
   - [ ] Full dark theme (recommended for consistency)
   - [ ] Full light theme
   - [ ] Context-aware theme switcher

2. **Mobile Table Strategy:**
   - [ ] Horizontal scroll with shadow indicators
   - [ ] Card layout transformation (recommended)
   - [ ] Simplified table with show/hide columns

3. **Component Library Approach:**
   - [ ] Build custom components (recommended - matches current Tailwind usage)
   - [ ] Adopt existing library (shadcn/ui, HeadlessUI, etc.)

4. **Button Size Standard:**
   - [ ] sm: px-3 py-1.5, md: px-4 py-2, lg: px-6 py-3
   - [ ] sm: px-4 py-2, md: px-6 py-3, lg: px-8 py-4

5. **Status Badge Style:**
   - [ ] Dark backgrounds with opacity (current list view)
   - [ ] Light backgrounds (current detail view)
   - [ ] Border-only with colored text

6. **Focus Ring Style:**
   - [ ] Blue (current - `ring-blue-500`)
   - [ ] Primary theme color (matches brand)
   - [ ] Always visible vs. keyboard-only (`:focus-visible`)

#### Success Metrics
- [ ] Zero WCAG accessibility violations (Level AA)
- [ ] 100% mobile usability score (all screens < 768px)
- [ ] Single source of truth for each UI pattern (buttons, badges, inputs)
- [ ] Zero hardcoded colors in component files
- [ ] Design system documentation complete
- [ ] All views use shared component library
- [ ] Consistent 4px spacing scale throughout

#### Risks & Mitigation
- **Risk:** Breaking existing functionality while refactoring
  - **Mitigation:** Comprehensive testing, visual regression tests, incremental rollout
- **Risk:** Component library overhead slows development
  - **Mitigation:** Start with high-traffic components, expand iteratively
- **Risk:** Design decisions cause scope creep
  - **Mitigation:** Limit to documented issues, defer nice-to-haves to 1.2

---

## Release Checklist

### Pre-Development
- [ ] All user stories reviewed and approved by stakeholders
- [ ] Design decisions finalized for User Story 4 (CSS improvements)
- [ ] Root cause confirmed for User Story 2 (invoice PAID status)
- [ ] Technical approach approved for all user stories
- [ ] Dev environment set up with latest master branch

### Development
- [ ] User Story 1: Customer outstanding balance fix implemented
- [ ] User Story 1: Integration tests pass
- [ ] User Story 2: Root cause identified and validated
- [ ] User Story 2: Invoice PAID status fix implemented
- [ ] User Story 2: Multi-payment scenarios tested
- [ ] User Story 3: Cold start warning implemented (login & signup)
- [ ] User Story 3: Environment detection working correctly
- [ ] User Story 4: Design system components created
- [ ] User Story 4: Accessibility issues fixed (focus states)
- [ ] User Story 4: Mobile layouts fixed (responsive grids)
- [ ] User Story 4: Status badge component standardized
- [ ] User Story 4: Button component standardized
- [ ] User Story 4: Theme consistency achieved
- [ ] User Story 4: Style guide documentation complete
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Code review completed

### Testing (Staging Environment)
- [ ] User Story 1: Verify partial payments update customer balance correctly
- [ ] User Story 1: Verify multiple invoices per customer work correctly
- [ ] User Story 2: Verify single payment still transitions to PAID
- [ ] User Story 2: Verify multiple payments transition to PAID
- [ ] User Story 2: Verify edge cases (overpayment, exact payment)
- [ ] User Story 3: Verify cold start warning appears in production build
- [ ] User Story 3: Verify warning does NOT appear in local dev
- [ ] User Story 3: User test - get feedback on message clarity
- [ ] User Story 4: Run axe accessibility audit (zero violations)
- [ ] User Story 4: Test keyboard navigation on all forms
- [ ] User Story 4: Test on mobile devices (320px, 375px, 768px)
- [ ] User Story 4: Visual regression testing (before/after screenshots)
- [ ] User Story 4: Cross-browser testing (Chrome, Firefox, Safari)
- [ ] Regression testing: All existing features still work
- [ ] Performance testing: No degradation in load times

### Documentation
- [ ] CHANGELOG.md updated with all changes
- [ ] README.md updated if needed
- [ ] Style guide published (User Story 4)
- [ ] API documentation updated if endpoints changed
- [ ] Database migration scripts documented
- [ ] Deployment notes prepared

### Deployment
- [ ] Database backup taken
- [ ] Backend deployed to staging
- [ ] Frontend deployed to staging
- [ ] Smoke tests pass on staging
- [ ] Backend deployed to production
- [ ] Frontend deployed to production
- [ ] Production smoke tests pass
- [ ] Rollback plan tested and ready

### Post-Deployment
- [ ] Monitor application logs for errors (24 hours)
- [ ] Monitor customer outstanding balance calculations
- [ ] Monitor invoice status transitions
- [ ] User feedback collection on cold start warning
- [ ] User feedback collection on UI improvements
- [ ] Performance metrics reviewed
- [ ] Create tickets for any issues discovered
- [ ] Retrospective meeting scheduled

---

## Dependencies

**External:**
- None (all changes are internal)

**Internal:**
- Requires Spring Boot event system (existing)
- Requires JPA/Hibernate (existing)
- Requires React Query for frontend cache invalidation (existing)
- Requires Tailwind CSS (existing)

---

## Rollback Plan

### User Story 1 (Customer Balance)
- Revert `InvoiceEventHandler` changes
- Run data migration script to recalculate customer balances from scratch
- Redeploy previous version

### User Story 2 (Invoice Status)
- Revert `InvoiceService.onPaymentRecorded()` changes
- Manually update any invoices stuck in SENT status (if needed)
- Redeploy previous version

### User Story 3 (Cold Start Warning)
- Frontend-only change, no data impact
- Redeploy previous frontend build

### User Story 4 (CSS Improvements)
- Frontend-only change, no data impact
- Redeploy previous frontend build
- Visual changes only, no logic impact

---

## Notes

- User Story 2 requires confirmation of root cause before finalizing implementation plan
- User Story 4 requires design decisions before development begins (see "Design Decisions Needed")
- Consider splitting User Story 4 into multiple smaller releases (1.1, 1.2) if timeline is tight
- All changes are backward compatible (no breaking API changes)
- No database schema changes required (only data recalculation for User Story 1)

---

## Questions for Stakeholders

1. **Priority Trade-off:** If timeline is tight, which user stories are must-haves vs. nice-to-haves?
   - Recommendation: Stories 1 & 2 are critical (money calculation bugs), 3 & 4 can be deferred

2. **User Story 4 Scope:** Should we tackle all CSS improvements in 1.1, or split into multiple releases?
   - Recommendation: 1.1 = Critical accessibility + mobile fixes, 1.2 = Design system + polish

3. **Testing Strategy:** Do we need external QA, or is internal testing sufficient?

4. **Rollout Strategy:** Big bang release, or phased rollout (backend first, then frontend)?

5. **User Communication:** Do we need to notify users about the fixes/improvements?

---

**Document Version:** 1.0
**Last Updated:** 2025-11-10
**Author:** Development Team
**Reviewers:** [To be assigned]
