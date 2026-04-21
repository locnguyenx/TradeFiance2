# Cash Management Services Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Cash Management Services module to provide comprehensive cash flow management tools for corporate treasury functions, enabling business customers to efficiently manage their liquidity, payments, and receivables.

**Architecture:** Following modular monolith principles, the Cash Management Services module will depend only on foundation module APIs (Customer, Account, Limits, Charges, Master Data) via their `.api` and `.dto` packages, never accessing internal repositories directly. The module implements all four communication patterns: Direct Interface Calls, Event Publishing, Domain Events, and Command Query Separation.

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Data JPA, Hibernate, PostgreSQL, Kafka for event streaming, Jakarta Validation, SLF4J with Logback

**Specs:**
- BRD: `docs/superpowers/specs/cash-management/cash-management-brd.md`
- BDD: `docs/superpowers/specs/cash-management/cash-management-bdd.md`
- Design: `docs/superpowers/specs/cash-management/cash-management-design.md`

---

## File Structure Overview

### Package Structure
```
src/main/java/com/banking/cashmanagement/
├── domain/
│   ├── entity/           # JPA entities
│   │   ├── PayrollBatch.java
│   │   ├── PayrollRecord.java
│   │   ├── ReceivableInvoice.java
│   │   ├── ReceivablePayment.java
│   │   ├── LiquidityPosition.java
│   │   ├── CashPoolingTransaction.java
│   │   ├── BatchPayment.java
│   │   ├── BatchPaymentInstruction.java
│   │   ├── AutoCollectionRule.java
│   │   ├── AutoCollectionAttempt.java
│   │   └── CashManagementAudit.java
│   ├── enums/            # Enumerations
│   │   ├── PayrollBatchStatus.java
│   │   ├── PayrollFileFormat.java
│   │   ├── InvoiceStatus.java
│   │   ├── PaymentStatus.java
│   │   └── ...
│   └── embeddable/       # Embeddable types
│       └── AuditFields.java
├── repository/           # Data access
│   ├── PayrollBatchRepository.java
│   ├── ReceivableInvoiceRepository.java
│   └── ...
├── service/              # Business logic
│   ├── PayrollService.java
│   ├── ReceivablesService.java
│   ├── LiquidityService.java
│   ├── BatchPaymentService.java
│   ├── AutoCollectionService.java
│   └── ReportingService.java
├── controller/           # REST endpoints
│   ├── PayrollController.java
│   ├── ReceivablesController.java
│   ├── LiquidityController.java
│   ├── BatchPaymentController.java
│   ├── AutoCollectionController.java
│   └── ReportingController.java
├── dto/                  # Data transfer objects
│   ├── PayrollBatchRequest.java
│   ├── ReceivableInvoiceRequest.java
│   └── ...
└── exception/            # Exception classes
    ├── PayrollBatchNotFoundException.java
    └── ...
```

### Module Registration
**Modify:** `src/main/java/com/banking/BankingApplication.java`
- Add `@ComponentScan("com.banking.cashmanagement")`
- Add `@EntityScan("com.banking.cashmanagement.domain.entity")`
- Add `@EnableJpaRepositories("com.banking.cashmanagement.repository")`

---

## Implementation Phases

### Phase 1: Core Infrastructure & Module Setup

### Task 1: Create module package structure and configuration

**Files:**
- Create: `src/main/java/com/banking/cashmanagement/domain/entity/.gitkeep`
- Create: `src/main/java/com/banking/cashmanagement/domain/enums/.gitkeep`
- Create: `src/main/java/com/banking/cashmanagement/domain/embeddable/.gitkeep`
- Create: `src/main/java/com/banking/cashmanagement/repository/.gitkeep`
- Create: `src/main/java/com/banking/cashmanagement/service/.gitkeep`
- Create: `src/main/java/com/banking/cashmanagement/controller/.gitkeep`
- Create: `src/main/java/com/banking/cashmanagement/dto/.gitkeep`
- Create: `src/main/java/com/banking/cashmanagement/exception/.gitkeep`
- Modify: `src/main/java/com/banking/BankingApplication.java`

- [ ] **Step 1: Create directory structure**

```bash
mkdir -p src/main/java/com/banking/cashmanagement/{domain/{entity,enums,embeddable},repository,service,controller,dto,exception}
```

- [ ] **Step 2: Update BankingApplication.java to include Cash Management module**

```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.banking.customer",
    "com.banking.account",
    "com.banking.product",
    "com.banking.limits",
    "com.banking.charges",
    "com.banking.masterdata",
    "com.banking.cashmanagement"  // ADD THIS
})
@EntityScan(basePackages = {
    "com.banking.customer.domain.entity",
    "com.banking.account.domain.entity",
    "com.banking.product.domain.entity",
    "com.banking.limits.domain.entity",
    "com.banking.charges.domain.entity",
    "com.banking.masterdata.domain.entity",
    "com.banking.cashmanagement.domain.entity"  // ADD THIS
})
@EnableJpaRepositories(basePackages = {
    "com.banking.customer.repository",
    "com.banking.account.repository",
    "com.banking.product.repository",
    "com.banking.limits.repository",
    "com.banking.charges.repository",
    "com.banking.masterdata.repository",
    "com.banking.cashmanagement.repository"  // ADD THIS
})
public class BankingApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingApplication.class, args);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/banking/cashmanagement/ src/main/java/com/banking/BankingApplication.java
git commit -m "feat(cash-management): create module package structure and configuration"
```

### Task 2: Create shared domain components (AuditFields embeddable)

**Files:**
- Create: `src/main/java/com/banking/cashmanagement/domain/embeddable/AuditFields.java`

- [ ] **Step 1: Write the failing test**

```java
// test/java/com/banking/cashmanagement/domain/embeddable/AuditFieldsTest.java
package com.banking.cashmanagement.domain.embeddable;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class AuditFieldsTest {
    
    @Test
    void shouldCreateAuditFieldsWithTimestamps() {
        AuditFields auditFields = new AuditFields();
        auditFields.setCreatedBy("user123");
        auditFields.setCreatedAt(LocalDateTime.now());
        auditFields.setUpdatedBy("user456");
        auditFields.setUpdatedAt(LocalDateTime.now());
        
        assertEquals("user123", auditFields.getCreatedBy());
        assertNotNull(auditFields.getCreatedAt());
        assertEquals("user456", auditFields.getUpdatedBy());
        assertNotNull(auditFields.getUpdatedAt());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.banking.cashmanagement.domain.embeddable.AuditFieldsTest"`
Expected: FAIL - class not found

- [ ] **Step 3: Write implementation**

```java
package com.banking.cashmanagement.domain.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;

@Embeddable
public class AuditFields {
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "updated_by")
    private String updatedBy;
    
    public AuditFields() {
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.banking.cashmanagement.domain.embeddable.AuditFieldsTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/banking/cashmanagement/domain/embeddable/AuditFields.java
git commit -m "feat(cash-management): add AuditFields embeddable for entity tracking"
```

### Task 3: Create enumeration classes

**Files:**
- Create: `src/main/java/com/banking/cashmanagement/domain/enums/PayrollBatchStatus.java`
- Create: `src/main/java/com/banking/cashmanagement/domain/enums/PayrollFileFormat.java`
- Create: `src/main/java/com/banking/cashmanagement/domain/enums/InvoiceStatus.java`
- Create: `src/main/java/com/banking/cashmanagement/domain/enums/PaymentStatus.java`
- Create: `src/main/java/com/banking/cashmanagement/domain/enums/...`

- [ ] **Step 1: Write tests for PayrollBatchStatus enum**

```java
// test/java/com/banking/cashmanagement/domain/enums/PayrollBatchStatusTest.java
package com.banking.cashmanagement.domain.enums;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PayrollBatchStatusTest {
    
    @Test
    void shouldHaveAllExpectedStatuses() {
        assertEquals(5, PayrollBatchStatus.values().length);
        assertNotNull(PayrollBatchStatus.PENDING);
        assertNotNull(PayrollBatchStatus.PROCESSING);
        assertNotNull(PayrollBatchStatus.COMPLETED);
        assertNotNull(PayrollBatchStatus.FAILED);
        assertNotNull(PayrollBatchStatus.CANCELLED);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.banking.cashmanagement.domain.enums.PayrollBatchStatusTest"`
Expected: FAIL - class not found

- [ ] **Step 3: Write implementation**

```java
package com.banking.cashmanagement.domain.enums;

public enum PayrollBatchStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.banking.cashmanagement.domain.enums.PayrollBatchStatusTest"`
Expected: PASS

- [ ] **Step 5: Create remaining enums ( PayrollFileFormat, InvoiceStatus, PaymentStatus, LiquidityPositionStatus, CashPoolingType, CashPoolingStatus, BatchPaymentStatus, BatchFileFormat, PaymentInstructionStatus, PaymentType, AutoCollectionTrigger, CollectionAmountType, AutoCollectionStatus, UserType, CashManagementEventType )**

Repeat similar pattern for each enum.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/banking/cashmanagement/domain/enums/
git commit -m "feat(cash-management): add enumeration classes for payroll, receivables, liquidity, batch payments, auto-collection"
```

---

### Phase 2: Core Entities & Flyway Migration

### Task 4: Create PayrollBatch entity and Flyway migration

**Files:**
- Create: `src/main/java/com/banking/cashmanagement/domain/entity/PayrollBatch.java`
- Create: `src/main/resources/db/migration/V1__create_payroll_batch_table.sql`

- [ ] **Step 1: Write the failing test**

```java
// test/java/com/banking/cashmanagement/domain/entity/PayrollBatchTest.java
package com.banking.cashmanagement.domain.entity;

import org.junit.jupiter.api.Test;
import com.banking.cashmanagement.domain.enums.PayrollBatchStatus;
import com.banking.cashmanagement.domain.enums.PayrollFileFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class PayrollBatchTest {
    
    @Test
    void shouldCreatePayrollBatchWithAllFields() {
        PayrollBatch batch = new PayrollBatch();
        batch.setBatchReference("PAY-2026-001");
        batch.setCustomerId(1L);
        batch.setStatus(PayrollBatchStatus.PENDING);
        batch.setFileFormat(PayrollFileFormat.CSV);
        batch.setRecordCount(50);
        batch.setTotalAmount(new BigDecimal("50000.00"));
        batch.setCurrency("USD");
        batch.setPaymentDate(LocalDate.now().plusDays(2));
        
        assertEquals("PAY-2026-001", batch.getBatchReference());
        assertEquals(1L, batch.getCustomerId());
        assertEquals(PayrollBatchStatus.PENDING, batch.getStatus());
        assertEquals(PayrollFileFormat.CSV, batch.getFileFormat());
        assertEquals(50, batch.getRecordCount());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.banking.cashmanagement.domain.entity.PayrollBatchTest"`
Expected: FAIL - class not found

- [ ] **Step 3: Write PayrollBatch entity**

```java
package com.banking.cashmanagement.domain.entity;

import com.banking.cashmanagement.domain.embeddable.AuditFields;
import com.banking.cashmanagement.domain.enums.PayrollBatchStatus;
import com.banking.cashmanagement.domain.enums.PayrollFileFormat;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payroll_batch")
public class PayrollBatch {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "batch_reference", unique = true, nullable = false)
    private String batchReference;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PayrollBatchStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "file_format", nullable = false)
    private PayrollFileFormat fileFormat;
    
    @Column(name = "record_count")
    private Integer recordCount;
    
    @Column(name = "processed_count")
    private Integer processedCount;
    
    @Column(name = "error_count")
    private Integer errorCount;
    
    @Column(name = "total_amount", precision = 19, scale = 4)
    private BigDecimal totalAmount;
    
    @Column(name = "currency", length = 3)
    private String currency;
    
    @Column(name = "payment_date")
    private LocalDate paymentDate;
    
    @Embedded
    private AuditFields auditFields = new AuditFields();
    
    @PrePersist
    protected void onCreate() {
        auditFields.setCreatedAt(java.time.LocalDateTime.now());
        auditFields.setCreatedBy("system");
    }
    
    @PreUpdate
    protected void onUpdate() {
        auditFields.setUpdatedAt(java.time.LocalDateTime.now());
        auditFields.setUpdatedBy("system");
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchReference() { return batchReference; }
    public void setBatchReference(String batchReference) { this.batchReference = batchReference; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public PayrollBatchStatus getStatus() { return status; }
    public void setStatus(PayrollBatchStatus status) { this.status = status; }
    public PayrollFileFormat getFileFormat() { return fileFormat; }
    public void setFileFormat(PayrollFileFormat fileFormat) { this.fileFormat = fileFormat; }
    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
    public Integer getProcessedCount() { return processedCount; }
    public void setProcessedCount(Integer processedCount) { this.processedCount = processedCount; }
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    public AuditFields getAuditFields() { return auditFields; }
    public void setAuditFields(AuditFields auditFields) { this.auditFields = auditFields; }
}
```

- [ ] **Step 4: Write Flyway migration**

```sql
-- V1__create_payroll_batch_table.sql
CREATE TABLE payroll_batch (
    id BIGSERIAL PRIMARY KEY,
    batch_reference VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    file_format VARCHAR(20) NOT NULL,
    record_count INTEGER,
    processed_count INTEGER,
    error_count INTEGER,
    total_amount DECIMAL(19, 4),
    currency VARCHAR(3),
    payment_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMP,
    updated_by VARCHAR(100)
);

CREATE INDEX idx_payroll_batch_customer_id ON payroll_batch(customer_id);
CREATE INDEX idx_payroll_batch_status ON payroll_batch(status);
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "com.banking.cashmanagement.domain.entity.PayrollBatchTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/banking/cashmanagement/domain/entity/PayrollBatch.java src/main/resources/db/migration/V1__create_payroll_batch_table.sql
git commit -m "feat(cash-management): add PayrollBatch entity and migration"
```

### Task 5: Create remaining core entities

Following similar pattern as Task 4, create:
- PayrollRecord
- ReceivableInvoice
- ReceivablePayment
- LiquidityPosition
- CashPoolingTransaction
- BatchPayment
- BatchPaymentInstruction
- AutoCollectionRule
- AutoCollectionAttempt
- CashManagementAudit

Each entity requires:
1. Write failing test
2. Implement entity
3. Write Flyway migration
4. Run test to verify pass
5. Commit

---

### Phase 3: Repository Layer

### Task 6: Create repository interfaces

**Files:**
- Create: `src/main/java/com/banking/cashmanagement/repository/PayrollBatchRepository.java`
- Create: `src/main/java/com/banking/cashmanagement/repository/ReceivableInvoiceRepository.java`
- Create: `src/main/java/com/banking/cashmanagement/repository/...`

- [ ] **Step 1: Write failing test**

```java
// test/java/com/banking/cashmanagement/repository/PayrollBatchRepositoryTest.java
package com.banking.cashmanagement.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import com.banking.cashmanagement.domain.entity.PayrollBatch;
import com.banking.cashmanagement.domain.enums.PayrollBatchStatus;
import com.banking.cashmanagement.domain.enums.PayrollFileFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PayrollBatchRepositoryTest {
    
    @Autowired
    private PayrollBatchRepository repository;
    
    @Test
    void shouldSaveAndRetrievePayrollBatch() {
        PayrollBatch batch = new PayrollBatch();
        batch.setBatchReference("PAY-TEST-001");
        batch.setCustomerId(1L);
        batch.setStatus(PayrollBatchStatus.PENDING);
        batch.setFileFormat(PayrollFileFormat.CSV);
        batch.setRecordCount(10);
        batch.setTotalAmount(new BigDecimal("10000.00"));
        batch.setCurrency("USD");
        batch.setPaymentDate(LocalDate.now().plusDays(1));
        
        PayrollBatch saved = repository.save(batch);
        
        assertNotNull(saved.getId());
        assertEquals("PAY-TEST-001", saved.getBatchReference());
        
        Optional<PayrollBatch> found = repository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(PayrollBatchStatus.PENDING, found.get().getStatus());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.banking.cashmanagement.repository.PayrollBatchRepositoryTest"`
Expected: FAIL - repository not found

- [ ] **Step 3: Write repository**

```java
package com.banking.cashmanagement.repository;

import com.banking.cashmanagement.domain.entity.PayrollBatch;
import com.banking.cashmanagement.domain.enums.PayrollBatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollBatchRepository extends JpaRepository<PayrollBatch, Long> {
    
    Optional<PayrollBatch> findByBatchReference(String batchReference);
    
    List<PayrollBatch> findByCustomerId(Long customerId);
    
    List<PayrollBatch> findByStatus(PayrollBatchStatus status);
    
    List<PayrollBatch> findByCustomerIdAndStatus(Long customerId, PayrollBatchStatus status);
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.banking.cashmanagement.repository.PayrollBatchRepositoryTest"`
Expected: PASS

- [ ] **Step 5: Create remaining repositories**

Following similar pattern create repositories for all entities.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/banking/cashmanagement/repository/
git commit -m "feat(cash-management): add repository interfaces for all entities"
```

---

### Phase 4: Service Layer (Business Logic)

### Task 7: Create PayrollService with TDD

**Files:**
- Create: `src/main/java/com/banking/cashmanagement/service/PayrollService.java`
- Create: `src/main/java/com/banking/cashmanagement/dto/PayrollBatchRequest.java`
- Create: `src/main/java/com/banking/cashmanagement/dto/PayrollBatchResponse.java`
- Create: `src/main/java/com/banking/cashmanagement/exception/PayrollBatchNotFoundException.java`
- Create: `src/main/java/com/banking/cashmanagement/exception/InvalidPayrollFileException.java`

- [ ] **Step 1: Write failing test - BDD Scenario: Successful payroll file upload**

From BDD: `@US-PM-01 @FR-PM-01 - Successful payroll file upload and validation`

```java
// test/java/com/banking/cashmanagement/service/PayrollServiceTest.java
package com.banking.cashmanagement.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.banking.cashmanagement.domain.entity.PayrollBatch;
import com.banking.cashmanagement.domain.enums.PayrollBatchStatus;
import com.banking.cashmanagement.domain.enums.PayrollFileFormat;
import com.banking.cashmanagement.repository.PayrollBatchRepository;
import com.banking.cashmanagement.dto.PayrollBatchRequest;
import com.banking.cashmanagement.dto.PayrollBatchResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {
    
    @Mock
    private PayrollBatchRepository payrollBatchRepository;
    
    @InjectMocks
    private PayrollService payrollService;
    
    @Test
    void shouldCreatePayrollBatchSuccessfully() {
        // Given: a valid payroll batch request
        PayrollBatchRequest request = new PayrollBatchRequest();
        request.setCustomerId(1L);
        request.setFileFormat(PayrollFileFormat.CSV);
        request.setRecordCount(50);
        request.setTotalAmount(new BigDecimal("50000.00"));
        request.setCurrency("USD");
        request.setPaymentDate(LocalDate.now().plusDays(2));
        
        // And: repository returns saved entity
        PayrollBatch savedBatch = new PayrollBatch();
        savedBatch.setId(1L);
        savedBatch.setBatchReference("PAY-2026-001");
        savedBatch.setCustomerId(1L);
        savedBatch.setStatus(PayrollBatchStatus.PENDING);
        savedBatch.setFileFormat(PayrollFileFormat.CSV);
        savedBatch.setRecordCount(50);
        savedBatch.setTotalAmount(new BigDecimal("50000.00"));
        savedBatch.setCurrency("USD");
        savedBatch.setPaymentDate(LocalDate.now().plusDays(2));
        
        when(payrollBatchRepository.save(any(PayrollBatch.class))).thenReturn(savedBatch);
        
        // When: creating payroll batch
        PayrollBatchResponse response = payrollService.createPayrollBatch(request);
        
        // Then: should return created batch with reference
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("PAY-2026-001", response.getBatchReference());
        assertEquals(PayrollBatchStatus.PENDING, response.getStatus());
        verify(payrollBatchRepository, times(1)).save(any(PayrollBatch.class));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.banking.cashmanagement.service.PayrollServiceTest"`
Expected: FAIL - service class not found

- [ ] **Step 3: Write DTO classes first**

```java
// PayrollBatchRequest.java
package com.banking.cashmanagement.dto;

import com.banking.cashmanagement.domain.enums.PayrollFileFormat;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PayrollBatchRequest {
    
    @NotNull
    private Long customerId;
    
    @NotNull
    private PayrollFileFormat fileFormat;
    
    @NotNull
    private Integer recordCount;
    
    @NotNull
    private BigDecimal totalAmount;
    
    @NotNull
    private String currency;
    
    @NotNull
    private LocalDate paymentDate;
    
    // Getters and Setters
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public PayrollFileFormat getFileFormat() { return fileFormat; }
    public void setFileFormat(PayrollFileFormat fileFormat) { this.fileFormat = fileFormat; }
    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
}
```

```java
// PayrollBatchResponse.java
package com.banking.cashmanagement.dto;

import com.banking.cashmanagement.domain.enums.PayrollBatchStatus;
import com.banking.cashmanagement.domain.enums.PayrollFileFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PayrollBatchResponse {
    
    private Long id;
    private String batchReference;
    private Long customerId;
    private PayrollBatchStatus status;
    private PayrollFileFormat fileFormat;
    private Integer recordCount;
    private Integer processedCount;
    private Integer errorCount;
    private BigDecimal totalAmount;
    private String currency;
    private LocalDate paymentDate;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBatchReference() { return batchReference; }
    public void setBatchReference(String batchReference) { this.batchReference = batchReference; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public PayrollBatchStatus getStatus() { return status; }
    public void setStatus(PayrollBatchStatus status) { this.status = status; }
    public PayrollFileFormat getFileFormat() { return fileFormat; }
    public void setFileFormat(PayrollFileFormat fileFormat) { this.fileFormat = fileFormat; }
    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }
    public Integer getProcessedCount() { return processedCount; }
    public void setProcessedCount(Integer processedCount) { this.processedCount = processedCount; }
    public Integer getErrorCount() { return errorCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
}
```

- [ ] **Step 4: Write exception classes**

```java
// PayrollBatchNotFoundException.java
package com.banking.cashmanagement.exception;

public class PayrollBatchNotFoundException extends RuntimeException {
    
    private static final String ERROR_CODE = "CAS-001";
    
    public PayrollBatchNotFoundException(Long id) {
        super("Payroll batch not found with id: " + id);
    }
    
    public PayrollBatchNotFoundException(String batchReference) {
        super("Payroll batch not found with reference: " + batchReference);
    }
    
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
```

```java
// InvalidPayrollFileException.java
package com.banking.cashmanagement.exception;

public class InvalidPayrollFileException extends RuntimeException {
    
    private static final String ERROR_CODE = "CAS-002";
    
    public InvalidPayrollFileException(String message) {
        super(message);
    }
    
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
```

- [ ] **Step 5: Write PayrollService**

```java
package com.banking.cashmanagement.service;

import com.banking.cashmanagement.domain.entity.PayrollBatch;
import com.banking.cashmanagement.domain.enums.PayrollBatchStatus;
import com.banking.cashmanagement.dto.PayrollBatchRequest;
import com.banking.cashmanagement.dto.PayrollBatchResponse;
import com.banking.cashmanagement.exception.PayrollBatchNotFoundException;
import com.banking.cashmanagement.repository.PayrollBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Transactional
public class PayrollService {
    
    private final PayrollBatchRepository payrollBatchRepository;
    
    public PayrollService(PayrollBatchRepository payrollBatchRepository) {
        this.payrollBatchRepository = payrollBatchRepository;
    }
    
    public PayrollBatchResponse createPayrollBatch(PayrollBatchRequest request) {
        PayrollBatch batch = new PayrollBatch();
        batch.setBatchReference(generateBatchReference());
        batch.setCustomerId(request.getCustomerId());
        batch.setStatus(PayrollBatchStatus.PENDING);
        batch.setFileFormat(request.getFileFormat());
        batch.setRecordCount(request.getRecordCount());
        batch.setTotalAmount(request.getTotalAmount());
        batch.setCurrency(request.getCurrency());
        batch.setPaymentDate(request.getPaymentDate());
        
        PayrollBatch saved = payrollBatchRepository.save(batch);
        return mapToResponse(saved);
    }
    
    @Transactional(readOnly = true)
    public PayrollBatchResponse getPayrollBatch(Long id) {
        PayrollBatch batch = payrollBatchRepository.findById(id)
            .orElseThrow(() -> new PayrollBatchNotFoundException(id));
        return mapToResponse(batch);
    }
    
    private String generateBatchReference() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "PAY-" + date + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
    
    private PayrollBatchResponse mapToResponse(PayrollBatch batch) {
        PayrollBatchResponse response = new PayrollBatchResponse();
        response.setId(batch.getId());
        response.setBatchReference(batch.getBatchReference());
        response.setCustomerId(batch.getCustomerId());
        response.setStatus(batch.getStatus());
        response.setFileFormat(batch.getFileFormat());
        response.setRecordCount(batch.getRecordCount());
        response.setProcessedCount(batch.getProcessedCount());
        response.setErrorCount(batch.getErrorCount());
        response.setTotalAmount(batch.getTotalAmount());
        response.setCurrency(batch.getCurrency());
        response.setPaymentDate(batch.getPaymentDate());
        return response;
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew test --tests "com.banking.cashmanagement.service.PayrollServiceTest"`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/banking/cashmanagement/service/ src/main/java/com/banking/cashmanagement/dto/ src/main/java/com/banking/cashmanagement/exception/
git commit -m "feat(cash-management): add PayrollService with create and get operations"
```

### Task 8: Continue implementing remaining services

Following the same TDD pattern, implement:

1. **ReceivablesService** - Invoice CRUD, payment matching
   - BDD: `@US-RM-01 @FR-RM-01` - Invoice presentment
   - BDD: `@US-RM-02 @FR-RM-02` - Payment matching

2. **LiquidityService** - Cash position, pooling, forecasting
   - BDD: `@US-LO-01 @FR-LO-01` - Cash position viewing
   - BDD: `@US-LO-02 @FR-LO-02` - Cash pooling

3. **BatchPaymentService** - Batch payment processing
   - BDD: `@US-BP-01 @FR-BP-01` - Batch upload
   - BDD: `@US-BP-03 @FR-BP-03` - Limit checking

4. **AutoCollectionService** - Collection rules and execution
   - BDD: `@US-AC-01 @FR-AC-01` - Collection setup
   - BDD: `@US-AC-02 @FR-AC-02` - Collection execution

5. **ReportingService** - Dashboard, reports, audit trails
   - BDD: `@US-RA-01 @FR-RA-01` - Dashboard
   - BDD: `@US-RA-03 @FR-RA-03` - Audit trail reporting

---

### Phase 5: Controller Layer (REST API)

### Task 9: Create PayrollController

**Files:**
- Create: `src/main/java/com/banking/cashmanagement/controller/PayrollController.java`

- [ ] **Step 1: Write failing test**

```java
// test/java/com/banking/cashmanagement/controller/PayrollControllerTest.java
package com.banking.cashmanagement.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.banking.cashmanagement.dto.PayrollBatchResponse;
import com.banking.cashmanagement.service.PayrollService;
import com.banking.cashmanagement.domain.enums.PayrollBatchStatus;
import com.banking.cashmanagement.domain.enums.PayrollFileFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PayrollController.class)
class PayrollControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private PayrollService payrollService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void shouldReturn201WhenCreatingPayrollBatch() throws Exception {
        PayrollBatchResponse response = new PayrollBatchResponse();
        response.setId(1L);
        response.setBatchReference("PAY-2026-001");
        response.setStatus(PayrollBatchStatus.PENDING);
        
        when(payrollService.createPayrollBatch(any())).thenReturn(response);
        
        String requestJson = """
            {
                "customerId": 1,
                "fileFormat": "CSV",
                "recordCount": 50,
                "totalAmount": 50000.00,
                "currency": "USD",
                "paymentDate": "2026-03-26"
            }
            """;
        
        mockMvc.perform(post("/api/cash-management/payroll/batches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.batchReference").value("PAY-2026-001"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.banking.cashmanagement.controller.PayrollControllerTest"`
Expected: FAIL - controller not found

- [ ] **Step 3: Write PayrollController**

```java
package com.banking.cashmanagement.controller;

import com.banking.cashmanagement.dto.PayrollBatchRequest;
import com.banking.cashmanagement.dto.PayrollBatchResponse;
import com.banking.cashmanagement.service.PayrollService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cash-management/payroll")
public class PayrollController {
    
    private final PayrollService payrollService;
    
    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }
    
    @PostMapping("/batches")
    public ResponseEntity<Map<String, Object>> createPayrollBatch(
            @Valid @RequestBody PayrollBatchRequest request) {
        PayrollBatchResponse response = payrollService.createPayrollBatch(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of("success", true, "data", response));
    }
    
    @GetMapping("/batches/{id}")
    public ResponseEntity<Map<String, Object>> getPayrollBatch(@PathVariable Long id) {
        PayrollBatchResponse response = payrollService.getPayrollBatch(id);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }
    
    @GetMapping("/batches")
    public ResponseEntity<Map<String, Object>> listPayrollBatches(
            @RequestParam(required = false) Long customerId) {
        List<PayrollBatchResponse> batches = payrollService.listPayrollBatches(customerId);
        return ResponseEntity.ok(Map.of("success", true, "data", batches));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew test --tests "com.banking.cashmanagement.controller.PayrollControllerTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/banking/cashmanagement/controller/
git commit -m "feat(cash-management): add PayrollController with REST endpoints"
```

---

### Phase 6: Foundation Module Integration

### Task 10: Integrate with Customer, Account, Limits, and Charges modules

**Files:**
- Modify: `src/main/java/com/banking/cashmanagement/service/PayrollService.java`
- Create: `src/main/java/com/banking/cashmanagement/integration/CustomerServiceAdapter.java`
- Create: `src/main/java/com/banking/cashmanagement/integration/AccountServiceAdapter.java`

- [ ] **Step 1: Write failing test - Integration with Account Module**

From BDD: `@US-INT-02 @FR-INT-02 - Account balance verification for funding`

```java
@Test
void shouldVerifyAccountBalanceBeforePayrollProcessing() {
    // Given: payroll batch with funding account
    Long fundingAccountId = 100L;
    BigDecimal payrollAmount = new BigDecimal("50000.00");
    
    // When: verifying account balance via adapter
    // Then: should call account service to get balance
    when(accountServiceAdapter.getAvailableBalance(fundingAccountId))
        .thenReturn(new BigDecimal("100000.00"));
    
    boolean hasSufficientFunds = payrollService.verifySufficientFunds(
        fundingAccountId, payrollAmount);
    
    assertTrue(hasSufficientFunds);
    verify(accountServiceAdapter, times(1)).getAvailableBalance(fundingAccountId);
}
```

- [ ] **Step 2: Run test to verify it fails**

Expected: FAIL - adapter not found

- [ ] **Step 3: Create service adapters (following modular monolith principles - only use .api interfaces)**

```java
// AccountServiceAdapter.java - Uses Account Module API only
package com.banking.cashmanagement.integration;

import com.banking.account.api.AccountQueryService;
import com.banking.account.dto.AccountBalanceResponse;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class AccountServiceAdapter {
    
    private final AccountQueryService accountQueryService;
    
    public AccountServiceAdapter(AccountQueryService accountQueryService) {
        this.accountQueryService = accountQueryService;
    }
    
    public BigDecimal getAvailableBalance(Long accountId) {
        AccountBalanceResponse balance = accountQueryService.getAccountBalance(accountId);
        return balance.getAvailableBalance();
    }
}
```

```java
// CustomerServiceAdapter.java - Uses Customer Module API only
package com.banking.cashmanagement.integration;

import com.banking.customer.api.CustomerValidationService;
import org.springframework.stereotype.Component;

@Component
public class CustomerServiceAdapter {
    
    private final CustomerValidationService customerValidationService;
    
    public CustomerServiceAdapter(CustomerValidationService customerValidationService) {
        this.customerValidationService = customerValidationService;
    }
    
    public boolean isValidCustomer(Long customerId) {
        return customerValidationService.isValidCustomer(customerId);
    }
}
```

```java
// LimitsServiceAdapter.java - Uses Limits Module API only
package com.banking.cashmanagement.integration;

import com.banking.limits.api.LimitCheckService;
import com.banking.limits.dto.LimitCheckRequest;
import com.banking.limits.dto.LimitCheckResponse;
import org.springframework.stereotype.Component;

@Component
public class LimitsServiceAdapter {
    
    private final LimitCheckService limitCheckService;
    
    public LimitsServiceAdapter(LimitCheckService limitCheckService) {
        this.limitCheckService = limitCheckService;
    }
    
    public LimitCheckResponse checkLimit(LimitCheckRequest request) {
        return limitCheckService.checkLimit(request);
    }
}
```

```java
// ChargesServiceAdapter.java - Uses Charges Module API only
package com.banking.cashmanagement.integration;

import com.banking.charges.api.ChargeCalculationService;
import com.banking.charges.dto.ChargeCalculationRequest;
import com.banking.charges.dto.ChargeCalculationResponse;
import org.springframework.stereotype.Component;

@Component
public class ChargesServiceAdapter {
    
    private final ChargeCalculationService chargeCalculationService;
    
    public ChargesServiceAdapter(ChargeCalculationService chargeCalculationService) {
        this.chargeCalculationService = chargeCalculationService;
    }
    
    public ChargeCalculationResponse calculateCharge(ChargeCalculationRequest request) {
        return chargeCalculationService.calculateCharge(request);
    }
}
```

- [ ] **Step 4: Update PayrollService to use adapters**

```java
@Service
@Transactional
public class PayrollService {
    
    private final PayrollBatchRepository payrollBatchRepository;
    private final AccountServiceAdapter accountServiceAdapter;
    private final LimitsServiceAdapter limitsServiceAdapter;
    private final ChargesServiceAdapter chargesServiceAdapter;
    
    public PayrollService(
            PayrollBatchRepository payrollBatchRepository,
            AccountServiceAdapter accountServiceAdapter,
            LimitsServiceAdapter limitsServiceAdapter,
            ChargesServiceAdapter chargesServiceAdapter) {
        this.payrollBatchRepository = payrollBatchRepository;
        this.accountServiceAdapter = accountServiceAdapter;
        this.limitsServiceAdapter = limitsServiceAdapter;
        this.chargesServiceAdapter = chargesServiceAdapter;
    }
    
    public boolean verifySufficientFunds(Long accountId, BigDecimal amount) {
        BigDecimal availableBalance = accountServiceAdapter.getAvailableBalance(accountId);
        return availableBalance.compareTo(amount) >= 0;
    }
    
    // ... existing methods
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "com.banking.cashmanagement.service.PayrollServiceTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/banking/cashmanagement/integration/
git commit -m "feat(cash-management): add service adapters for foundation module integration"
```

---

### Phase 7: Event Publishing (Kafka Integration)

### Task 11: Implement domain events for Cash Management

**Files:**
- Create: `src/main/java/com/banking/cashmanagement/event/PayrollProcessedEvent.java`
- Create: `src/main/java/com/banking/cashmanagement/event/PaymentCompletedEvent.java`
- Create: `src/main/java/com/banking/cashmanagement/event/CollectionSuccessEvent.java`
- Modify: `src/main/java/com/banking/cashmanagement/service/PayrollService.java`

- [ ] **Step 1: Write failing test**

From BDD: `@US-INT-04 @FR-INT-04 - Charge application for cash management services`

```java
@Test
void shouldPublishEventAfterPayrollProcessing() {
    // Given: completed payroll batch
    PayrollBatch batch = createCompletedBatch();
    
    // When: processing is complete
    payrollService.completePayrollBatch(batch.getId());
    
    // Then: should publish event for downstream processing
    verify(eventPublisher, times(1)).publishEvent(any(PayrollProcessedEvent.class));
}
```

- [ ] **Step 2: Write event classes**

```java
// PayrollProcessedEvent.java
package com.banking.cashmanagement.event;

import org.springframework.context.ApplicationEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayrollProcessedEvent extends ApplicationEvent {
    
    private final Long payrollBatchId;
    private final String batchReference;
    private final Long customerId;
    private final BigDecimal totalAmount;
    private final String currency;
    private final LocalDateTime processedAt;
    
    public PayrollProcessedEvent(Object source, Long payrollBatchId, String batchReference,
            Long customerId, BigDecimal totalAmount, String currency) {
        super(source);
        this.payrollBatchId = payrollBatchId;
        this.batchReference = batchReference;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.processedAt = LocalDateTime.now();
    }
    
    // Getters
    public Long getPayrollBatchId() { return payrollBatchId; }
    public String getBatchReference() { return batchReference; }
    public Long getCustomerId() { return customerId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getCurrency() { return currency; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
```

- [ ] **Step 3: Modify service to publish events**

```java
@Service
@Transactional
public class PayrollService {
    
    private final ApplicationEventPublisher eventPublisher;
    
    public PayrollService(..., ApplicationEventPublisher eventPublisher) {
        // ... existing dependencies
        this.eventPublisher = eventPublisher;
    }
    
    public PayrollBatchResponse completePayrollBatch(Long id) {
        PayrollBatch batch = payrollBatchRepository.findById(id)
            .orElseThrow(() -> new PayrollBatchNotFoundException(id));
        
        batch.setStatus(PayrollBatchStatus.COMPLETED);
        PayrollBatch saved = payrollBatchRepository.save(batch);
        
        // Publish event for downstream processing (charges, notifications)
        PayrollProcessedEvent event = new PayrollProcessedEvent(
            this,
            saved.getId(),
            saved.getBatchReference(),
            saved.getCustomerId(),
            saved.getTotalAmount(),
            saved.getCurrency()
        );
        eventPublisher.publishEvent(event);
        
        return mapToResponse(saved);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

- [ ] **Step 5: Commit**

---

### Phase 8: Security & Authorization

### Task 12: Add role-based access control

**Files:**
- Modify: `src/main/java/com/banking/cashmanagement/controller/PayrollController.java`

- [ ] **Step 1: Write failing test**

From BDD: `@US-SEC-01 @FR-SEC-01 - Role-based access control for cash management functions`

```java
@Test
void shouldDenyAccessToPaymentInitiationForViewerRole() {
    // Given: user with Treasury Viewer role
    // When: attempting to access payment initiation
    // Then: should return 403 Forbidden
    mockMvc.perform(post("/api/cash-management/payroll/batches")
            .with(user("viewer").roles("TREASURY_VIEWER")))
            .andExpect(status().isForbidden());
}
```

- [ ] **Step 2: Add security annotations to controller**

```java
@Secured({"ROLE_TREASURY_MANAGER", "ROLE_PAYROLL_ADMIN"})
@PostMapping("/batches")
public ResponseEntity<Map<String, Object>> createPayrollBatch(
        @Valid @RequestBody PayrollBatchRequest request) {
    // ...
}
```

- [ ] **Step 3: Run test to verify it passes**

- [ ] **Step 4: Commit**

---

### Phase 9: Exception Handling & Validation

### Task 13: Add global exception handler

**Files:**
- Create: `src/main/java/com/banking/cashmanagement/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: Write failing test**

```java
@Test
void shouldReturnProperErrorResponseWhenBatchNotFound() {
    when(payrollService.getPayrollBatch(999L))
        .thenThrow(new PayrollBatchNotFoundException(999L));
    
    mockMvc.perform(get("/api/cash-management/payroll/batches/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.errorCode").value("CAS-001"));
}
```

- [ ] **Step 2: Write exception handler**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(PayrollBatchNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePayrollBatchNotFound(
            PayrollBatchNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of(
                "success", false,
                "errorCode", ex.getErrorCode(),
                "message", ex.getMessage()
            ));
    }
    
    @ExceptionHandler(InvalidPayrollFileException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidPayrollFile(
            InvalidPayrollFileException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "success", false,
                "errorCode", ex.getErrorCode(),
                "message", ex.getMessage()
            ));
    }
}
```

- [ ] **Step 3: Run test to verify it passes**

- [ ] **Step 4: Commit**

---

### Phase 10: Testing & Documentation

### Task 14: Run full test suite and generate coverage report

- [ ] **Step 1: Run tests**

```bash
./gradlew test
```

- [ ] **Step 2: Generate coverage**

```bash
./gradlew test --coverage
```

- [ ] **Step 3: Verify OpenAPI spec**

```bash
./gradlew exportOpenApiSpec
```

- [ ] **Step 4: Commit final**

```bash
git add .
git commit -m "feat(cash-management): complete Cash Management Services module implementation"
```

---

## Summary

This implementation plan follows:
- TDD methodology with failing tests first
- Modular monolith architecture with proper module boundaries
- API-first design using foundation module APIs only
- Event-driven architecture for async processing
- Role-based security with Spring Security
- Flyway migrations for database schema
- Full traceability from BDD scenarios to implementation

The plan is organized in bite-sized tasks (2-5 minutes each) with clear steps:
1. Write failing test (aligned to BDD scenario)
2. Run test to verify it fails
3. Write minimal implementation
4. Run test to verify it passes
5. Commit
