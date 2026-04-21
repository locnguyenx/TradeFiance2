# Product Configuration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- []`) syntax for tracking.

**Goal:** Build the Product Configuration module — a cross-cutting foundation module providing a versioned product catalog with maker-checker lifecycle, feature toggles, tiered pricing, and customer segment targeting for Account, Payment, and Trade Finance product families.

**Architecture:** Modular monolith. Product module in `com.banking.product` with its own entities, services, controllers, and frontend. Other modules (Account, Payment, Trade Finance) consume via `ProductQueryService`. No direct DB joins between modules.

**Tech Stack:** Java 17, Spring Boot 3.2, PostgreSQL, Flyway, React 18, TypeScript, Vite, Ant Design, Redux Toolkit

**Specs:**
- BRD: `docs/superpowers/brds/2026-03-20-product-configuration.md`
- BDD: `docs/superpowers/specs/2026-03-20-product-configuration-bdd.md`
- Design: `docs/superpowers/specs/2026-03-20-product-configuration-design.md`
- Test Strategy: `docs/superpowers/templates/test-strategy.md`

---

## File Structure

### Backend (Java)

```
src/main/java/com/banking/product/
├── domain/
│   ├── entity/
│   │   ├── Product.java
│   │   ├── ProductVersion.java
│   │   ├── ProductFeature.java
│   │   ├── ProductFeeEntry.java
│   │   ├── ProductFeeTier.java
│   │   ├── ProductCustomerSegment.java
│   │   └── ProductAuditLog.java
│   ├── enums/
│   │   ├── ProductFamily.java
│   │   ├── ProductStatus.java
│   │   ├── FeeCalculationMethod.java
│   │   └── AuditAction.java
│   └── embeddable/
│       └── AuditFields.java
├── repository/
│   ├── ProductRepository.java
│   ├── ProductVersionRepository.java
│   ├── ProductFeatureRepository.java
│   ├── ProductFeeEntryRepository.java
│   ├── ProductFeeTierRepository.java
│   ├── ProductCustomerSegmentRepository.java
│   └── ProductAuditLogRepository.java
├── service/
│   ├── ProductService.java
│   ├── ProductVersionService.java
│   ├── ProductFeatureService.java
│   ├── ProductPricingService.java
│   ├── ProductSegmentService.java
│   ├── ProductQueryService.java
│   ├── ProductAuditService.java
│   └── ProductEventPublisher.java
├── controller/
│   ├── ProductController.java
│   ├── ProductVersionController.java
│   ├── ProductFeatureController.java
│   ├── ProductPricingController.java
│   ├── ProductSegmentController.java
│   ├── ProductQueryController.java
│   └── ProductExceptionHandler.java
├── dto/
│   ├── request/
│   │   ├── CreateProductRequest.java
│   │   ├── UpdateProductRequest.java
│   │   ├── RejectProductRequest.java
│   │   ├── ProductFeatureRequest.java
│   │   ├── ProductFeeEntryRequest.java
│   │   └── ProductSegmentRequest.java
│   └── response/
│       ├── ProductResponse.java
│       ├── ProductVersionResponse.java
│       ├── ProductDetailResponse.java
│       ├── ProductSummaryResponse.java
│       ├── ProductFeatureResponse.java
│       ├── ProductFeeEntryResponse.java
│       ├── ProductFeeTierResponse.java
│       ├── ProductAuditLogResponse.java
│       └── ProductVersionDiffResponse.java
├── mapper/
│   └── ProductMapper.java
├── event/
│   ├── ProductActivatedEvent.java
│   ├── ProductRetiredEvent.java
│   └── ProductVersionCreatedEvent.java
└── exception/
    ├── ProductNotFoundException.java
    ├── ProductVersionNotFoundException.java
    ├── DuplicateProductCodeException.java
    ├── InvalidProductStatusException.java
    ├── ProductVersionNotEditableException.java
    ├── ProductHasActiveContractsException.java
    ├── MakerCheckerViolationException.java
    └── InvalidFeaturePrefixException.java
```

### Database

```
src/main/resources/db/migration/
├── V2__create_product_schema.sql
├── V3__seed_product_data.sql
└── V4__map_accounts_to_products.sql
```

### Frontend (TypeScript/React)

```
frontend/src/
├── types/
│   └── product.types.ts
├── services/
│   └── productService.ts
├── store/slices/
│   └── productSlice.ts
├── pages/products/
│   ├── ProductListPage.tsx
│   ├── ProductDetailPage.tsx
│   ├── ProductFormPage.tsx
│   └── ProductVersionComparePage.tsx
```

### Tests

```
src/test/java/com/banking/product/
├── service/
│   ├── ProductServiceTest.java
│   ├── ProductVersionServiceTest.java
│   ├── ProductFeatureServiceTest.java
│   ├── ProductPricingServiceTest.java
│   ├── ProductSegmentServiceTest.java
│   └── ProductQueryServiceTest.java
├── controller/
│   ├── ProductControllerTest.java
│   ├── ProductVersionControllerTest.java
│   └── ProductQueryControllerTest.java
└── integration/
    └── MakerCheckerIntegrationTest.java
```

---

## Task 1: Domain Enums and Exceptions

**Files:**
- Create: `src/main/java/com/banking/product/domain/enums/ProductFamily.java`
- Create: `src/main/java/com/banking/product/domain/enums/ProductStatus.java`
- Create: `src/main/java/com/banking/product/domain/enums/FeeCalculationMethod.java`
- Create: `src/main/java/com/banking/product/domain/enums/AuditAction.java`
- Create: `src/main/java/com/banking/product/domain/embeddable/AuditFields.java`
- Create: `src/main/java/com/banking/product/exception/ProductNotFoundException.java`
- Create: `src/main/java/com/banking/product/exception/ProductVersionNotFoundException.java`
- Create: `src/main/java/com/banking/product/exception/DuplicateProductCodeException.java`
- Create: `src/main/java/com/banking/product/exception/InvalidProductStatusException.java`
- Create: `src/main/java/com/banking/product/exception/ProductVersionNotEditableException.java`
- Create: `src/main/java/com/banking/product/exception/ProductHasActiveContractsException.java`
- Create: `src/main/java/com/banking/product/exception/MakerCheckerViolationException.java`
- Create: `src/main/java/com/banking/product/exception/InvalidFeaturePrefixException.java`

- [ ] **Step 1: Create ProductFamily enum**

```java
package com.banking.product.domain.enums;

public enum ProductFamily {
    ACCOUNT, PAYMENT, TRADE_FINANCE
}
```

- [ ] **Step 2: Create ProductStatus enum**

```java
package com.banking.product.domain.enums;

public enum ProductStatus {
    DRAFT, PENDING_APPROVAL, APPROVED, ACTIVE, SUPERSEDED, RETIRED
}
```

- [ ] **Step 3: Create FeeCalculationMethod enum**

```java
package com.banking.product.domain.enums;

public enum FeeCalculationMethod {
    FLAT, PERCENTAGE, TIERED_VOLUME
}
```

- [ ] **Step 4: Create AuditAction enum**

```java
package com.banking.product.domain.enums;

public enum AuditAction {
    CREATE, UPDATE, SUBMIT, APPROVE, REJECT, ACTIVATE, RETIRE
}
```

- [ ] **Step 5: Create AuditFields embeddable**

Follow existing pattern from `com.banking.account.domain.embeddable.AuditFields`:

```java
package com.banking.product.domain.embeddable;

@Embeddable
@Data
public class AuditFields {
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 6: Create all 8 exception classes**

Each exception must have `public static final String ERROR_CODE`. Follow pattern from `com.banking.customer.exception.CustomerNotFoundException`:

```java
package com.banking.product.exception;

public class ProductNotFoundException extends RuntimeException {
    public static final String ERROR_CODE = "PROD-001";

    public ProductNotFoundException(String identifier) {
        super("Product not found: " + identifier);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
```

Repeat for all 8 exceptions with error codes PROD-001 through PROD-008.

- [ ] **Step 7: Compile check**

Run: `./gradlew compileJava`
Expected: SUCCESS

---

## Task 2: Domain Entities

**Files:**
- Create: `src/main/java/com/banking/product/domain/entity/Product.java`
- Create: `src/main/java/com/banking/product/domain/entity/ProductVersion.java`
- Create: `src/main/java/com/banking/product/domain/entity/ProductFeature.java`
- Create: `src/main/java/com/banking/product/domain/entity/ProductFeeEntry.java`
- Create: `src/main/java/com/banking/product/domain/entity/ProductFeeTier.java`
- Create: `src/main/java/com/banking/product/domain/entity/ProductCustomerSegment.java`
- Create: `src/main/java/com/banking/product/domain/entity/ProductAuditLog.java`

- [ ] **Step 1: Create Product entity**

```java
package com.banking.product.domain.entity;

@Entity
@Table(name = "products")
@Getter @Setter
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductFamily family;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Version
    private Long version;

    @Embedded
    private AuditFields audit = new AuditFields();
}
```

- [ ] **Step 2: Create ProductVersion entity**

Follow the design spec Section 2 — includes status, submittedBy, approvedBy, rejectionComment, contractCount, and OneToMany relationships to features, feeEntries, customerSegments.

- [ ] **Step 3: Create ProductFeature entity**

- [ ] **Step 4: Create ProductFeeEntry entity**

- [ ] **Step 5: Create ProductFeeTier entity**

- [ ] **Step 6: Create ProductCustomerSegment entity** — imports `CustomerType` from `com.banking.customer.domain.enums`

- [ ] **Step 7: Create ProductAuditLog entity** — no AuditFields (immutable), has timestamp field

- [ ] **Step 8: Compile check**

Run: `./gradlew compileJava`
Expected: SUCCESS

---

## Task 3: Database Migration

**Files:**
- Create: `src/main/resources/db/migration/V2__create_product_schema.sql`
- Create: `src/main/resources/db/migration/V3__seed_product_data.sql`

- [ ] **Step 1: Write V2 migration**

Create all tables per design spec Section 6: products, product_versions, product_features, product_fee_entries, product_fee_tiers, product_customer_segments, product_audit_logs. Include indexes and unique constraints.

- [ ] **Step 2: Write V3 seed data**

Seed 4 account products (CURRENT, SAVINGS, FIXED-DEPOSIT, LOAN) with ACTIVE v1 versions. Seed basic features and segments per BDD examples.

- [ ] **Step 3: Verify migration runs**

Run: `./gradlew bootRun` — check startup logs for Flyway migration success
Expected: V2 and V3 applied successfully

---

## Task 4: Repositories

**Files:**
- Create: `src/main/java/com/banking/product/repository/ProductRepository.java`
- Create: `src/main/java/com/banking/product/repository/ProductVersionRepository.java`
- Create: `src/main/java/com/banking/product/repository/ProductFeatureRepository.java`
- Create: `src/main/java/com/banking/product/repository/ProductFeeEntryRepository.java`
- Create: `src/main/java/com/banking/product/repository/ProductFeeTierRepository.java`
- Create: `src/main/java/com/banking/product/repository/ProductCustomerSegmentRepository.java`
- Create: `src/main/java/com/banking/product/repository/ProductAuditLogRepository.java`

- [ ] **Step 1: Create ProductRepository**

```java
package com.banking.product.repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Optional<Product> findByCode(String code);
    boolean existsByCode(String code);
}
```

- [ ] **Step 2: Create ProductVersionRepository**

```java
@Repository
public interface ProductVersionRepository extends JpaRepository<ProductVersion, Long> {
    List<ProductVersion> findByProductIdOrderByVersionNumberDesc(Long productId);
    Optional<ProductVersion> findByProductIdAndVersionNumber(Long productId, Integer versionNumber);
    Optional<ProductVersion> findByProductIdAndStatus(Long productId, ProductStatus status);
    @Query("SELECT pv FROM ProductVersion pv WHERE pv.product.code = :code AND pv.status = :status")
    List<ProductVersion> findByProductCodeAndStatus(@Param("code") String code, @Param("status") ProductStatus status);
}
```

- [ ] **Step 3: Create remaining repositories** (Feature, FeeEntry, FeeTier, Segment, AuditLog)

Follow JPA repository pattern. Each extends `JpaRepository<Entity, Long>` with custom finders as needed.

- [ ] **Step 4: Compile check**

Run: `./gradlew compileJava`
Expected: SUCCESS

---

## Task 5: DTOs and Mapper

**Files:**
- Create: `src/main/java/com/banking/product/dto/request/CreateProductRequest.java`
- Create: `src/main/java/com/banking/product/dto/request/UpdateProductRequest.java`
- Create: `src/main/java/com/banking/product/dto/request/RejectProductRequest.java`
- Create: `src/main/java/com/banking/product/dto/request/ProductFeatureRequest.java`
- Create: `src/main/java/com/banking/product/dto/request/ProductFeeEntryRequest.java`
- Create: `src/main/java/com/banking/product/dto/request/ProductSegmentRequest.java`
- Create: `src/main/java/com/banking/product/dto/response/ProductResponse.java`
- Create: `src/main/java/com/banking/product/dto/response/ProductVersionResponse.java`
- Create: `src/main/java/com/banking/product/dto/response/ProductDetailResponse.java`
- Create: `src/main/java/com/banking/product/dto/response/ProductSummaryResponse.java`
- Create: `src/main/java/com/banking/product/dto/response/ProductFeatureResponse.java`
- Create: `src/main/java/com/banking/product/dto/response/ProductFeeEntryResponse.java`
- Create: `src/main/java/com/banking/product/dto/response/ProductFeeTierResponse.java`
- Create: `src/main/java/com/banking/product/dto/response/ProductAuditLogResponse.java`
- Create: `src/main/java/com/banking/product/dto/response/ProductVersionDiffResponse.java`
- Create: `src/main/java/com/banking/product/mapper/ProductMapper.java`

- [ ] **Step 1: Create request DTOs with validation annotations**

Follow existing pattern from `CreateCorporateCustomerRequest`. Use `@NotBlank`, `@NotNull`, `@Size`, `@Valid`.

- [ ] **Step 2: Create response DTOs**

Follow existing pattern from `CustomerResponse`. Include static `fromEntity()` factory methods where appropriate.

- [ ] **Step 3: Create ProductMapper**

Follow existing pattern from `CustomerMapper`. Manual mapping with `@Component`.

- [ ] **Step 4: Compile check**

Run: `./gradlew compileJava`
Expected: SUCCESS

---

## Task 6: ProductService (TDD)

**Files:**
- Create: `src/test/java/com/banking/product/service/ProductServiceTest.java`
- Create: `src/main/java/com/banking/product/service/ProductService.java`

BDD Scenarios: S1.1 (Create), S1.2 (Code immutable), S1.3 (Duplicate code), S1.4 (Edit draft)

- [ ] **Step 1: Write test for creating a product**

```java
// BDD S1.1: Create a new product as draft
@Test
void createProduct_createsDraftWithVersion1() {
    CreateProductRequest request = new CreateProductRequest("BIZ-CURRENT", "Business Current Account", ProductFamily.ACCOUNT, ...);
    ProductResponse response = productService.createProduct(request);
    assertEquals("BIZ-CURRENT", response.getCode());
    assertEquals(ProductStatus.DRAFT.name(), response.getStatus());
    assertEquals(1, response.getVersionNumber());
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.banking.product.service.ProductServiceTest.createProduct_createsDraftWithVersion1"`
Expected: FAIL — class not found

- [ ] **Step 3: Write minimal ProductService implementation**

Implement `createProduct()`: validate code uniqueness, create Product entity, create initial ProductVersion (v1, DRAFT), persist, return response.

- [ ] **Step 4: Write test for duplicate code rejection**

```java
// BDD S1.3: Duplicate product code is rejected
@Test
void createProduct_duplicateCode_throwsException() {
    productService.createProduct(new CreateProductRequest("BIZ-CURRENT", ...));
    assertThrows(DuplicateProductCodeException.class, () ->
        productService.createProduct(new CreateProductRequest("BIZ-CURRENT", ...)));
}
```

- [ ] **Step 5: Implement duplicate code check**

- [ ] **Step 6: Write test for code immutability**

```java
// BDD S1.2: Product code is immutable
@Test
void updateProduct_changeCode_throwsException() {
    Product product = createTestProduct("BIZ-CURRENT");
    assertThrows(InvalidProductStatusException.class, () ->
        productService.updateProduct(product.getId(), new UpdateProductRequest("BIZ-CURR-NEW", ...)));
}
```

- [ ] **Step 7: Implement code immutability check in updateProduct**

- [ ] **Step 8: Write test for editing draft product**

```java
// BDD S1.4: Edit a draft product
@Test
void updateProduct_draftVersion_updatesInPlace() {
    Product product = createTestProduct("BIZ-CURRENT");
    ProductResponse response = productService.updateProduct(product.getId(), new UpdateProductRequest(null, "Updated Name", ...));
    assertEquals("Updated Name", response.getName());
    assertEquals(1, response.getVersionNumber()); // still v1
}
```

- [ ] **Step 9: Implement draft edit behavior**

- [ ] **Step 10: Write tests for getProduct, getProductByCode, searchProducts**

- [ ] **Step 11: Implement get/search methods**

- [ ] **Step 12: Run all ProductServiceTest**

Run: `./gradlew test --tests "com.banking.product.service.ProductServiceTest"`
Expected: ALL PASS

- [ ] **Step 13: Commit**

```bash
git add src/main/java/com/banking/product/service/ProductService.java src/test/java/com/banking/product/service/ProductServiceTest.java
git commit -m "feat(product): add ProductService with CRUD and validation"
```

---

## Task 7: ProductVersionService — Lifecycle (TDD)

**Files:**
- Create: `src/test/java/com/banking/product/service/ProductVersionServiceTest.java`
- Create: `src/main/java/com/banking/product/service/ProductVersionService.java`
- Create: `src/main/java/com/banking/product/service/ProductAuditService.java`

BDD Scenarios: S2.1-S2.9 (Lifecycle), S3.1-S3.2 (Maker-Checker), S1.5-S1.8 (Edit creates version)

- [ ] **Step 1: Write test for submit for approval**

```java
// BDD S2.1: Submit product for approval
@Test
void submitForApproval_draftToPendingApproval() {
    ProductVersion version = createDraftVersion();
    ProductVersionResponse response = versionService.submitForApproval(version.getId(), "alice");
    assertEquals(ProductStatus.PENDING_APPROVAL, response.getStatus());
    assertEquals("alice", response.getSubmittedBy());
}
```

- [ ] **Step 2: Run test — FAIL**

- [ ] **Step 3: Implement submitForApproval** — validate status is DRAFT, set status to PENDING_APPROVAL, set submittedBy, log audit

- [ ] **Step 4: Write and implement approve** (S2.2)

- [ ] **Step 5: Write and implement reject with comment** (S2.3, S2.4)

- [ ] **Step 6: Write and implement activate** (S2.5) — validate APPROVED status, deactivate previous active version → SUPERSEDED

- [ ] **Step 7: Write test for one-active-version rule** (S2.6)

```java
@Test
void activate_supersedesPreviousActiveVersion() {
    ProductVersion v1 = createActiveVersion();
    ProductVersion v2 = createApprovedVersion();
    versionService.activate(v2.getId(), "charlie");
    assertEquals(ProductStatus.ACTIVE, v2.getStatus());
    assertEquals(ProductStatus.SUPERSEDED, v1.getStatus());
}
```

- [ ] **Step 8: Implement supersession logic**

- [ ] **Step 9: Write and implement retire** (S2.8) — validate ACTIVE status, check contractCount == 0

- [ ] **Step 10: Write test for cannot retire with contracts** (S2.9)

```java
@Test
void retire_withActiveContracts_throwsException() {
    ProductVersion version = createActiveVersionWithContracts(5);
    assertThrows(ProductHasActiveContractsException.class, () ->
        versionService.retire(version.getId(), "charlie"));
}
```

- [ ] **Step 11: Write and implement maker-checker validation** (S3.1)

```java
@Test
void approve_sameUserAsSubmitter_throwsException() {
    ProductVersion version = createPendingVersion(submittedBy: "alice");
    assertThrows(MakerCheckerViolationException.class, () ->
        versionService.approve(version.getId(), "alice"));
}
```

- [ ] **Step 12: Write and implement edit-creates-new-version** (S1.5)

```java
@Test
void updateProduct_activeVersion_createsNewDraftVersion() {
    Product product = createProductWithActiveVersion();
    ProductResponse response = productService.updateProduct(product.getId(), updateRequest);
    assertEquals(2, response.getVersionNumber());
    assertEquals(ProductStatus.DRAFT.name(), response.getStatus());
}
```

- [ ] **Step 13: Write and implement cannot-edit-non-draft** (S1.6, S1.7, S1.8)

- [ ] **Step 14: Write and implement version history** (S1.13) and compare versions (S1.14)

- [ ] **Step 15: Run all ProductVersionServiceTest**

Run: `./gradlew test --tests "com.banking.product.service.ProductVersionServiceTest"`
Expected: ALL PASS

- [ ] **Step 16: Commit**

```bash
git add src/main/java/com/banking/product/service/ src/test/java/com/banking/product/service/ProductVersionServiceTest.java
git commit -m "feat(product): add version lifecycle with maker-checker enforcement"
```

---

## Task 8: ProductFeatureService (TDD)

**Files:**
- Create: `src/test/java/com/banking/product/service/ProductFeatureServiceTest.java`
- Create: `src/main/java/com/banking/product/service/ProductFeatureService.java`

BDD Scenarios: S4.1-S4.5 (Feature toggles), S8.5 (Namespacing)

- [ ] **Step 1: Write test for adding features** (S4.1)

- [ ] **Step 2: Implement addFeature with DRAFT validation and prefix check**

- [ ] **Step 3: Write test for invalid prefix rejection** (S4.3)

```java
@Test
void addFeature_invalidPrefix_throwsException() {
    ProductVersion version = createAccountFamilyVersion();
    assertThrows(InvalidFeaturePrefixException.class, () ->
        featureService.addFeature(version.getId(), new ProductFeatureRequest("payment.instant_transfer", "true")));
}
```

- [ ] **Step 4: Implement prefix validation** — feature key must start with `{family.toLowerCase()}.`

- [ ] **Step 5: Write and implement toggle feature** (S4.2)

- [ ] **Step 6: Write and implement cannot modify on non-draft** (S4.4)

- [ ] **Step 7: Write and implement features preserved across versions** (S4.5)

- [ ] **Step 8: Run all tests, commit**

---

## Task 9: ProductPricingService (TDD)

**Files:**
- Create: `src/test/java/com/banking/product/service/ProductPricingServiceTest.java`
- Create: `src/main/java/com/banking/product/service/ProductPricingService.java`

BDD Scenarios: S5.1-S5.6 (Pricing)

- [ ] **Step 1: Write and implement flat fee** (S5.1)

- [ ] **Step 2: Write and implement percentage fee** (S5.2)

- [ ] **Step 3: Write and implement tiered volume pricing** (S5.3)

- [ ] **Step 4: Write and implement multiple fee entries** (S5.4)

- [ ] **Step 5: Write and implement cannot modify pricing on non-draft** (S5.5 — note: current BDD labels this as S5.5, fee-preserves-across-versions is also S5.6)

- [ ] **Step 6: Write and implement fee entries preserved across versions** (S5.6)

- [ ] **Step 7: Run all tests, commit**

---

## Task 10: ProductSegmentService (TDD)

**Files:**
- Create: `src/test/java/com/banking/product/service/ProductSegmentServiceTest.java`
- Create: `src/main/java/com/banking/product/service/ProductSegmentService.java`

BDD Scenarios: S6.1-S6.3 (Segments)

- [ ] **Step 1: Write and implement assign segments** (S6.1)

- [ ] **Step 2: Write and implement query by customer type** (S6.2)

- [ ] **Step 3: Write and implement query active products only** (S6.3)

- [ ] **Step 4: Run all tests, commit**

---

## Task 11: ProductQueryService (TDD)

**Files:**
- Create: `src/test/java/com/banking/product/service/ProductQueryServiceTest.java`
- Create: `src/main/java/com/banking/product/service/ProductQueryService.java`
- Create: `src/main/java/com/banking/product/service/ProductEventPublisher.java`

BDD Scenarios: S7.1-S7.8 (Downstream Integration)

- [ ] **Step 1: Write and implement getActiveProductByCode** (S7.3)

- [ ] **Step 2: Write and implement getProductVersionById** (S7.6) — works for any status including SUPERSEDED

- [ ] **Step 3: Write and implement getActiveProductsByFamily** (S7.7, S7.8)

- [ ] **Step 4: Write and implement getActiveProductsByCustomerType** (S6.2, S6.3)

- [ ] **Step 5: Write and implement version binding** (S7.1, S7.2, S7.4, S7.5)

- [ ] **Step 6: Create ProductEventPublisher and event classes**

Create event classes (each extends `ApplicationEvent`):
- Create: `src/main/java/com/banking/product/event/ProductActivatedEvent.java`
- Create: `src/main/java/com/banking/product/event/ProductRetiredEvent.java`
- Create: `src/main/java/com/banking/product/event/ProductVersionCreatedEvent.java`

Create publisher in service package:
- Create: `src/main/java/com/banking/product/service/ProductEventPublisher.java`

Publisher uses Spring `ApplicationEventPublisher` to publish events on activate/retire/create-version.

- [ ] **Step 7: Run all tests, commit**

---

## Task 12: Controllers and Exception Handler

**Files:**
- Create: `src/main/java/com/banking/product/controller/ProductController.java`
- Create: `src/main/java/com/banking/product/controller/ProductVersionController.java`
- Create: `src/main/java/com/banking/product/controller/ProductFeatureController.java`
- Create: `src/main/java/com/banking/product/controller/ProductPricingController.java`
- Create: `src/main/java/com/banking/product/controller/ProductSegmentController.java`
- Create: `src/main/java/com/banking/product/controller/ProductQueryController.java`
- Create: `src/main/java/com/banking/product/controller/ProductExceptionHandler.java`
- Create: `src/test/java/com/banking/product/controller/ProductControllerTest.java`
- Create: `src/test/java/com/banking/product/controller/ProductVersionControllerTest.java`
- Create: `src/test/java/com/banking/product/controller/ProductQueryControllerTest.java`

- [ ] **Step 1: Create ProductExceptionHandler**

Follow pattern from `GlobalExceptionHandler` and `AccountExceptionHandler`. `@RestControllerAdvice(basePackages = "com.banking.product")`. Handle all 8 exceptions with correct HTTP status codes.

- [ ] **Step 2: Create ProductController** — POST /api/products, GET /api/products/{id}, GET /api/products/code/{code}, PUT /api/products/{id}, GET /api/products/search

- [ ] **Step 3: Write ProductControllerTest** — test each endpoint with MockMvc

- [ ] **Step 4: Create ProductVersionController** — lifecycle endpoints (submit, approve, reject, activate, retire), version history, compare

- [ ] **Step 5: Write ProductVersionControllerTest**

- [ ] **Step 6: Create ProductFeatureController, ProductPricingController, ProductSegmentController**

- [ ] **Step 7: Create ProductQueryController** — downstream query endpoints

- [ ] **Step 8: Write ProductQueryControllerTest**

- [ ] **Step 9: Create MakerCheckerIntegrationTest** — full maker-checker flow

- [ ] **Step 10: Run all tests**

Run: `./gradlew test --tests "com.banking.product.*"`
Expected: ALL PASS

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/banking/product/controller/ src/test/java/com/banking/product/controller/
git commit -m "feat(product): add REST controllers with validation and exception handling"
```

---

## Task 13: Frontend — Types, Services, Redux

**Files:**
- Create: `frontend/src/types/product.types.ts`
- Create: `frontend/src/services/productService.ts`
- Create: `frontend/src/store/slices/productSlice.ts`
- Modify: `frontend/src/store/index.ts` (add product reducer)

- [ ] **Step 1: Create product.types.ts**

Define TypeScript interfaces matching backend DTOs: Product, ProductVersion, ProductFeature, ProductFeeEntry, ProductFeeTier, ProductSegment, ProductFamily, ProductStatus, FeeCalculationMethod.

- [ ] **Step 2: Create productService.ts**

API methods per design spec Section 5: CRUD, lifecycle, features, fees, segments.

- [ ] **Step 3: Create productSlice.ts**

Redux slice with createAsyncThunk for all service methods. Follow pattern from `accountSlice.ts`.

- [ ] **Step 4: Register in store/index.ts**

Add `products: productReducer` to the combined reducer.

- [ ] **Step 5: Commit**

---

## Task 14: Frontend — Pages and Routing

**Files:**
- Create: `frontend/src/pages/products/ProductListPage.tsx`
- Create: `frontend/src/pages/products/ProductDetailPage.tsx`
- Create: `frontend/src/pages/products/ProductFormPage.tsx`
- Create: `frontend/src/pages/products/ProductVersionComparePage.tsx`
- Modify: `frontend/src/App.tsx` (add routes)
- Modify: `frontend/src/components/layout/Sidebar.tsx` (add Products menu item)

- [ ] **Step 1: Create ProductListPage**

Ant Design Table with filters (family, status, segment). Follow pattern from `CustomerListPage.tsx`.

- [ ] **Step 2: Create ProductFormPage**

Create/edit form with fields for code, name, family, description, segments. Follow pattern from `CorporateCustomerForm.tsx`.

- [ ] **Step 3: Create ProductDetailPage**

Tabs for: Overview, Versions, Features, Pricing, Segments, Audit Trail. Include lifecycle action buttons (submit, approve, reject, activate, retire).

- [ ] **Step 4: Create ProductVersionComparePage**

Side-by-side diff view for comparing two product versions.

- [ ] **Step 5: Add routes to App.tsx**

- [ ] **Step 6: Add Products to Sidebar**

- [ ] **Step 7: Run frontend tests**

Run: `npm run test:coverage`
Expected: ALL PASS

- [ ] **Step 8: Run lint**

Run: `npm run lint`
Expected: No errors

- [ ] **Step 9: Commit**

---

## Task 15: Integration — Account Module Updates

**Files:**
- Modify: `src/main/java/com/banking/account/domain/entity/Account.java` (add productVersionId)
- Modify: `src/main/java/com/banking/account/dto/AccountOpeningRequest.java` (productCode resolution)
- Modify: `src/main/java/com/banking/account/dto/AccountResponse.java` (add product name)
- Modify: `src/main/java/com/banking/account/service/AccountService.java` (resolve product on open)
- Modify: `src/main/java/com/banking/account/mapper/AccountMapper.java` (map productVersionId)
- Create: `src/main/resources/db/migration/V4__map_accounts_to_products.sql`

- [ ] **Step 1: Add productVersionId field to Account entity**

```java
@Column(name = "product_version_id")
private Long productVersionId;
```

- [ ] **Step 2: Update AccountService.openAccount() to resolve product**

```java
ProductVersionResponse pv = productQueryService.getActiveProductByCode(request.getProductCode());
account.setProductVersionId(pv.getId());
```

- [ ] **Step 3: Update AccountMapper to populate product name from version**

- [ ] **Step 4: Create V4 migration to map existing accounts to product versions**

New migration `V4__map_accounts_to_products.sql` maps existing account `product_id` values (1-4) to the corresponding `product_versions.id` from V3 seed data. Adds `product_version_id` column to `accounts` table.

- [ ] **Step 5: Verify existing Account tests still pass**

Run: `./gradlew test --tests "com.banking.account.*"`
Expected: ALL PASS (no regressions)

- [ ] **Step 6: Commit**

---

## Task 16: Full Build Verification

- [ ] **Step 1: Clean build**

Run: `./gradlew clean build`
Expected: SUCCESS

- [ ] **Step 2: Frontend tests**

Run: `npm run test:coverage`
Expected: ALL PASS, coverage thresholds met

- [ ] **Step 3: Frontend lint**

Run: `npm run lint`
Expected: No errors

- [ ] **Step 4: Verify no duplicate classes**

Run: `grep -r "class ProductExceptionHandler" src/`
Expected: exactly 1 result in `controller/` package

- [ ] **Step 5: Commit final state**

```bash
git add -A
git commit -m "feat(product): complete product configuration module"
```

---

## Plan Review

After writing this plan, dispatch plan-document-reviewer to verify completeness.
