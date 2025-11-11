package com.invoiceme.application.seeddata;

import com.invoiceme.application.customer.CustomerService;
import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.application.customer.dto.CustomerResponse;
import com.invoiceme.application.invoice.InvoiceService;
import com.invoiceme.application.invoice.dto.CreateInvoiceRequest;
import com.invoiceme.application.invoice.dto.InvoiceResponse;
import com.invoiceme.application.lineitem.LineItemService;
import com.invoiceme.application.lineitem.dto.CreateLineItemRequest;
import com.invoiceme.application.payment.PaymentService;
import com.invoiceme.application.payment.dto.RecordPaymentRequest;
import com.invoiceme.domain.payment.PaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for generating seed data for testing and demo purposes.
 */
@Service
public class SeedDataService {

    private static final Logger logger = LoggerFactory.getLogger(SeedDataService.class);

    @Autowired
    private CustomerService customerService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private LineItemService lineItemService;

    @Autowired
    private PaymentService paymentService;

    private static final String[] FIRST_NAMES = {
        "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
        "William", "Barbara", "David", "Elizabeth", "Richard", "Susan", "Joseph", "Jessica",
        "Thomas", "Sarah", "Christopher", "Karen", "Daniel", "Nancy", "Matthew", "Lisa"
    };

    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
        "Taylor", "Moore", "Jackson", "Martin", "Lee", "Thompson", "White", "Harris"
    };

    private static final String[] COMPANY_TYPES = {
        "LLC", "Inc", "Corp", "Co", "Group", "Partners", "Associates", "Enterprises"
    };

    private static final String[] COMPANY_PREFIXES = {
        "Advanced", "Global", "Premier", "Elite", "Supreme", "Dynamic", "Innovative", "Strategic",
        "Precision", "Summit", "Apex", "Prime", "United", "National", "American", "Metro"
    };

    private static final String[] COMPANY_SUFFIXES = {
        "Solutions", "Services", "Industries", "Technologies", "Consulting", "Systems",
        "Manufacturing", "Logistics", "Construction", "Development", "Marketing", "Distribution"
    };

    private static final String[] STREET_NAMES = {
        "Main", "Oak", "Maple", "Cedar", "Elm", "Washington", "Lake", "Hill",
        "Park", "River", "Pine", "Sunset", "First", "Second", "Broadway", "Market"
    };

    private static final String[] STREET_TYPES = {
        "St", "Ave", "Blvd", "Dr", "Ln", "Rd", "Way", "Ct"
    };

    private static final String[] CITIES = {
        "New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia",
        "San Antonio", "San Diego", "Dallas", "San Jose", "Austin", "Jacksonville",
        "San Francisco", "Seattle", "Denver", "Boston", "Portland", "Miami"
    };

    private static final String[] STATES = {
        "NY", "CA", "TX", "FL", "IL", "PA", "OH", "GA", "NC", "MI",
        "WA", "AZ", "MA", "CO", "OR", "NV", "VA", "TN", "MN", "WI"
    };

    private static final String[] LINE_ITEM_DESCRIPTIONS = {
        "Professional Services", "Consulting Services", "Software Development", "Project Management",
        "Design Services", "Marketing Services", "IT Support", "Training Services",
        "Maintenance Contract", "Installation Services", "Custom Development", "Technical Support",
        "Data Analysis", "Research Services", "Content Creation", "SEO Optimization"
    };

    private static final PaymentMethod[] PAYMENT_METHODS = PaymentMethod.values();

    /**
     * Generate a batch of customers with invoices, line items, and payments.
     * @param batchSize Number of customers to create
     * @return Result containing success count and any errors
     */
    @Transactional
    public SeedDataResult generateBatch(int batchSize) {
        logger.info("Starting seed data generation for {} customers", batchSize);

        SeedDataResult result = new SeedDataResult();
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < batchSize; i++) {
            try {
                generateCustomerWithData();
                successCount++;
                logger.debug("Successfully created customer {}/{}", i + 1, batchSize);
            } catch (Exception e) {
                String errorMsg = String.format("Failed to create customer %d/%d: %s - %s",
                    i + 1, batchSize, e.getClass().getSimpleName(), e.getMessage());
                logger.error(errorMsg, e);
                errors.add(errorMsg);
            }
        }

        result.setSuccessCount(successCount);
        result.setErrorCount(errors.size());
        result.setErrors(errors);

        logger.info("Seed data generation complete: {} succeeded, {} failed", successCount, errors.size());
        return result;
    }

    /**
     * Generate a single customer with complete data including invoices, line items, and payments.
     */
    private void generateCustomerWithData() {
        // Create customer
        CustomerResponse customer = createRandomCustomer();
        logger.debug("Created customer: {}", customer.getCompanyName());

        // Determine number of invoices (5-10 paid, 0-2 sent, 0-2 draft)
        int paidCount = randomInt(5, 10);
        int sentCount = randomInt(0, 2);
        int draftCount = randomInt(0, 2);

        // Create paid invoices (oldest)
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        for (int i = 0; i < paidCount; i++) {
            createPaidInvoice(customer, sixMonthsAgo);
        }

        // Create sent invoices (more recent)
        for (int i = 0; i < sentCount; i++) {
            createSentInvoice(customer);
        }

        // Create draft invoices (most recent)
        for (int i = 0; i < draftCount; i++) {
            createDraftInvoice(customer);
        }
    }

    private CustomerResponse createRandomCustomer() {
        CreateCustomerRequest request = new CreateCustomerRequest();

        // Generate company name
        String companyName = String.format("%s %s %s",
            randomElement(COMPANY_PREFIXES),
            randomElement(COMPANY_SUFFIXES),
            randomElement(COMPANY_TYPES));
        request.setCompanyName(companyName);

        // Contact name
        request.setContactFirstName(randomElement(FIRST_NAMES));
        request.setContactLastName(randomElement(LAST_NAMES));

        // Email
        String emailPrefix = companyName.toLowerCase()
            .replaceAll("[^a-z0-9]", "")
            .substring(0, Math.min(10, companyName.length()));
        request.setEmail(emailPrefix + "@example.com");

        // Phone
        request.setPhone(String.format("(%03d) %03d-%04d",
            randomInt(200, 999),
            randomInt(200, 999),
            randomInt(1000, 9999)));

        // Address
        request.setAddressLine1(String.format("%d %s %s",
            randomInt(100, 9999),
            randomElement(STREET_NAMES),
            randomElement(STREET_TYPES)));

        if (randomBoolean(0.3)) {
            request.setAddressLine2(String.format("Suite %d", randomInt(100, 999)));
        }

        request.setCity(randomElement(CITIES));
        request.setState(randomElement(STATES));
        request.setZipCode(String.format("%05d", randomInt(10000, 99999)));
        request.setCountry("USA");

        return customerService.createCustomer(request);
    }

    private void createPaidInvoice(CustomerResponse customer, LocalDate startDate) {
        try {
            // Create invoice
            CreateInvoiceRequest invoiceReq = new CreateInvoiceRequest();
            invoiceReq.setCustomerId(customer.getId());

            InvoiceResponse invoice = invoiceService.createInvoice(invoiceReq);
            logger.debug("Created draft invoice: {}", invoice.getInvoiceNumber());

            // Add 1-5 line items
            int lineItemCount = randomInt(1, 5);
            BigDecimal total = BigDecimal.ZERO;

            for (int i = 0; i < lineItemCount; i++) {
                CreateLineItemRequest lineItemReq = new CreateLineItemRequest();
                lineItemReq.setInvoiceId(invoice.getId());
                lineItemReq.setDescription(randomElement(LINE_ITEM_DESCRIPTIONS));

                BigDecimal quantity = BigDecimal.valueOf(randomInt(1, 20));
                lineItemReq.setQuantity(quantity);

                BigDecimal unitPrice = randomPrice(50, 500);
                lineItemReq.setUnitPrice(unitPrice);

                lineItemService.createLineItem(lineItemReq);
                total = total.add(unitPrice.multiply(quantity));
            }

            // Mark as sent
            InvoiceResponse sentInvoice = invoiceService.markInvoiceAsSent(invoice.getId());
            logger.debug("Marked invoice as sent: {}", sentInvoice.getInvoiceNumber());

            // Create payments (1-3 payments totaling the invoice amount)
            int paymentCount = randomInt(1, 3);
            BigDecimal remaining = total;

            LocalDate invoiceDate = sentInvoice.getInvoiceDate()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

            for (int i = 0; i < paymentCount; i++) {
                RecordPaymentRequest paymentReq = new RecordPaymentRequest();
                paymentReq.setInvoiceId(sentInvoice.getId());

                // Last payment gets the remaining amount, others get random portion
                BigDecimal paymentAmount;
                if (i == paymentCount - 1) {
                    paymentAmount = remaining;
                } else {
                    // Pay 20-80% of remaining
                    BigDecimal percentage = BigDecimal.valueOf(randomInt(20, 80))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    paymentAmount = remaining.multiply(percentage)
                        .setScale(2, RoundingMode.HALF_UP);
                }

                paymentReq.setAmount(paymentAmount);
                paymentReq.setPaymentMethod(randomElement(PAYMENT_METHODS));

                // Payment date 1-30 days after invoice date
                LocalDate paymentDate = invoiceDate.plusDays(randomInt(1, 30));
                paymentReq.setPaymentDate(paymentDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

                if (randomBoolean(0.3)) {
                    paymentReq.setReferenceNumber(String.format("REF-%08d", randomInt(10000000, 99999999)));
                }

                paymentService.recordPayment(paymentReq);
                remaining = remaining.subtract(paymentAmount);

                logger.debug("Recorded payment {} of {}: ${}", i + 1, paymentCount, paymentAmount);
            }

        } catch (Exception e) {
            logger.error("Error creating paid invoice for customer {}: {}",
                customer.getCompanyName(), e.getMessage());
            throw new RuntimeException("Failed to create paid invoice: " + e.getMessage(), e);
        }
    }

    private void createSentInvoice(CustomerResponse customer) {
        try {
            CreateInvoiceRequest invoiceReq = new CreateInvoiceRequest();
            invoiceReq.setCustomerId(customer.getId());

            InvoiceResponse invoice = invoiceService.createInvoice(invoiceReq);

            // Add 1-5 line items
            int lineItemCount = randomInt(1, 5);
            for (int i = 0; i < lineItemCount; i++) {
                CreateLineItemRequest lineItemReq = new CreateLineItemRequest();
                lineItemReq.setInvoiceId(invoice.getId());
                lineItemReq.setDescription(randomElement(LINE_ITEM_DESCRIPTIONS));
                lineItemReq.setQuantity(BigDecimal.valueOf(randomInt(1, 20)));
                lineItemReq.setUnitPrice(randomPrice(50, 500));

                lineItemService.createLineItem(lineItemReq);
            }

            // Mark as sent
            invoiceService.markInvoiceAsSent(invoice.getId());
            logger.debug("Created sent invoice: {}", invoice.getInvoiceNumber());

        } catch (Exception e) {
            logger.error("Error creating sent invoice for customer {}: {}",
                customer.getCompanyName(), e.getMessage());
            throw new RuntimeException("Failed to create sent invoice: " + e.getMessage(), e);
        }
    }

    private void createDraftInvoice(CustomerResponse customer) {
        try {
            CreateInvoiceRequest invoiceReq = new CreateInvoiceRequest();
            invoiceReq.setCustomerId(customer.getId());

            InvoiceResponse invoice = invoiceService.createInvoice(invoiceReq);

            // Add 1-3 line items
            int lineItemCount = randomInt(1, 3);
            for (int i = 0; i < lineItemCount; i++) {
                CreateLineItemRequest lineItemReq = new CreateLineItemRequest();
                lineItemReq.setInvoiceId(invoice.getId());
                lineItemReq.setDescription(randomElement(LINE_ITEM_DESCRIPTIONS));
                lineItemReq.setQuantity(BigDecimal.valueOf(randomInt(1, 20)));
                lineItemReq.setUnitPrice(randomPrice(50, 500));

                lineItemService.createLineItem(lineItemReq);
            }

            logger.debug("Created draft invoice: {}", invoice.getInvoiceNumber());

        } catch (Exception e) {
            logger.error("Error creating draft invoice for customer {}: {}",
                customer.getCompanyName(), e.getMessage());
            throw new RuntimeException("Failed to create draft invoice: " + e.getMessage(), e);
        }
    }

    // Utility methods

    private <T> T randomElement(T[] array) {
        return array[ThreadLocalRandom.current().nextInt(array.length)];
    }

    private int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private boolean randomBoolean(double probability) {
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    private BigDecimal randomPrice(int min, int max) {
        double price = ThreadLocalRandom.current().nextDouble(min, max);
        return BigDecimal.valueOf(price).setScale(2, RoundingMode.HALF_UP);
    }
}
