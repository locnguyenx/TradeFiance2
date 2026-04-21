# Master Data Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Master Data Management module providing reference data (currencies, countries, industries, exchange rates, holidays, branches, channels, document types) via REST APIs.

**Architecture:** Leaf foundation module with no outbound dependencies. All entities are independent with no cross-entity JPA joins. Follows existing modular monolith patterns from Product/Account/Customer modules.

**Tech Stack:** Java 17, Spring Boot 3.2, Spring Data JPA, PostgreSQL, Flyway, React 18, TypeScript, Ant Design, Redux Toolkit

---

## File Structure

```
# Backend
src/main/java/com/banking/masterdata/
├── domain/
│   ├── entity/
│   │   ├── Currency.java
│   │   ├── Country.java
│   │   ├── Industry.java
│   │   ├── ExchangeRate.java
│   │   ├── Holiday.java
│   │   ├── Branch.java
│   │   ├── Channel.java
│   │   └── DocumentType.java
│   ├── enums/
│   └── embeddable/
│       └── AuditFields.java
├── repository/
│   ├── CurrencyRepository.java
│   ├── CountryRepository.java
│   ├── IndustryRepository.java
│   ├── ExchangeRateRepository.java
│   ├── HolidayRepository.java
│   ├── BranchRepository.java
│   ├── ChannelRepository.java
│   └── DocumentTypeRepository.java
├── service/
│   ├── CurrencyService.java
│   ├── CountryService.java
│   ├── IndustryService.java
│   ├── ExchangeRateService.java
│   ├── HolidayService.java
│   ├── BranchService.java
│   ├── ChannelService.java
│   ├── DocumentTypeService.java
│   └── MasterDataQueryService.java
├── controller/
│   ├── CurrencyController.java
│   ├── CountryController.java
│   ├── IndustryController.java
│   ├── ExchangeRateController.java
│   ├── HolidayController.java
│   ├── BranchController.java
│   ├── ChannelController.java
│   ├── DocumentTypeController.java
│   └── MasterDataExceptionHandler.java
├── dto/
│   ├── request/
│   │   ├── CreateCurrencyRequest.java
│   │   ├── UpdateCurrencyRequest.java
│   │   ├── CreateCountryRequest.java
│   │   ├── CreateIndustryRequest.java
│   │   ├── CreateExchangeRateRequest.java
│   │   ├── CreateHolidayRequest.java
│   │   ├── CreateBranchRequest.java
│   │   ├── UpdateBranchRequest.java
│   │   ├── CreateChannelRequest.java
│   │   ├── UpdateChannelRequest.java
│   │   ├── CreateDocumentTypeRequest.java
│   │   └── UpdateDocumentTypeRequest.java
│   └── response/
│       ├── CurrencyResponse.java
│       ├── CountryResponse.java
│       ├── IndustryResponse.java
│       ├── ExchangeRateResponse.java
│       ├── HolidayResponse.java
│       ├── BranchResponse.java
│       ├── ChannelResponse.java
│       └── DocumentTypeResponse.java
├── mapper/
│   └── MasterDataMapper.java
└── exception/
    ├── CurrencyNotFoundException.java
    ├── CurrencyAlreadyExistsException.java
    ├── CountryNotFoundException.java
    ├── IndustryNotFoundException.java
    ├── ExchangeRateNotFoundException.java
    ├── HolidayNotFoundException.java
    ├── BranchNotFoundException.java
    ├── BranchAlreadyExistsException.java
    ├── ChannelNotFoundException.java
    ├── ChannelAlreadyExistsException.java
    ├── DocumentTypeNotFoundException.java
    └── DocumentTypeAlreadyExistsException.java

# Flyway Migrations
src/main/resources/db/migration/
├── V6__create_master_data_schema.sql
└── V7__seed_master_data.sql

# Frontend
frontend/src/
├── types/masterData.types.ts
├── services/masterDataService.ts
├── store/slices/masterDataSlice.ts
└── pages/master-data/
    ├── MasterDataListPage.tsx
    ├── MasterDataFormPage.tsx
    ├── ExchangeRatePage.tsx
    └── index.ts
```

---

## Task 1: Scaffold Module & AuditFields

**Files:**
- Create: `src/main/java/com/banking/masterdata/domain/embeddable/AuditFields.java`
- Create: `src/main/java/com/banking/masterdata/MasterDataModule.java` (config class)

- [ ] **Step 1: Copy AuditFields embeddable**

```java
package com.banking.masterdata.domain.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

@Embeddable
public class AuditFields {
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    public AuditFields() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/banking/masterdata/domain/embeddable/AuditFields.java
git commit -m "feat(masterdata): scaffold module with AuditFields embeddable"
```

---

## Task 2: Currency Entity, Repository, Service, Controller

**Files:**
- Create: `src/main/java/com/banking/masterdata/domain/entity/Currency.java`
- Create: `src/main/java/com/banking/masterdata/repository/CurrencyRepository.java`
- Create: `src/main/java/com/banking/masterdata/exception/CurrencyNotFoundException.java`
- Create: `src/main/java/com/banking/masterdata/exception/CurrencyAlreadyExistsException.java`
- Create: `src/main/java/com/banking/masterdata/dto/request/CreateCurrencyRequest.java`
- Create: `src/main/java/com/banking/masterdata/dto/response/CurrencyResponse.java`
- Create: `src/main/java/com/banking/masterdata/mapper/MasterDataMapper.java`
- Create: `src/main/java/com/banking/masterdata/service/CurrencyService.java`
- Create: `src/main/java/com/banking/masterdata/controller/CurrencyController.java`

**BDD Reference:** S1.1, S1.2, S1.3, S1.4, S1.5, S1.8, S1.9

- [ ] **Step 1: Write Currency entity**

```java
package com.banking.masterdata.domain.entity;

import com.banking.masterdata.domain.embeddable.AuditFields;
import jakarta.persistence.*;

@Entity
@Table(name = "currencies")
public class Currency {
    @Id
    @Column(length = 3)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 5)
    private String symbol;

    @Column(name = "decimal_places", nullable = false)
    private Integer decimalPlaces;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Embedded
    private AuditFields audit = new AuditFields();

    protected Currency() {}

    public Currency(String code, String name, String symbol, Integer decimalPlaces) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.decimalPlaces = decimalPlaces;
    }

    // Getters and setters
    public String getCode() { return code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public Integer getDecimalPlaces() { return decimalPlaces; }
    public void setDecimalPlaces(Integer decimalPlaces) { this.decimalPlaces = decimalPlaces; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public AuditFields getAudit() { return audit; }
}
```

- [ ] **Step 2: Write CurrencyRepository**

```java
package com.banking.masterdata.repository;

import com.banking.masterdata.domain.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {
    List<Currency> findByIsActiveTrue();
    boolean existsByCode(String code);
}
```

- [ ] **Step 3: Write exception classes**

```java
// CurrencyNotFoundException.java
public class CurrencyNotFoundException extends RuntimeException {
    public static final String ERROR_CODE = "MDATA-001";
    public CurrencyNotFoundException(String code) {
        super("Currency not found: " + code);
    }
    public String getErrorCode() { return ERROR_CODE; }
}

// CurrencyAlreadyExistsException.java
public class CurrencyAlreadyExistsException extends RuntimeException {
    public static final String ERROR_CODE = "MDATA-002";
    public CurrencyAlreadyExistsException(String code) {
        super("Currency code already exists: " + code);
    }
    public String getErrorCode() { return ERROR_CODE; }
}
```

- [ ] **Step 4: Write DTOs**

```java
// CreateCurrencyRequest.java
public class CreateCurrencyRequest {
    @NotBlank private String code;
    @NotBlank private String name;
    private String symbol;
    @NotNull @Min(0) @Max(10) private Integer decimalPlaces;
    // getters and setters
}

// CurrencyResponse.java
public class CurrencyResponse {
    private String code;
    private String name;
    private String symbol;
    private Integer decimalPlaces;
    private Boolean isActive;

    public static CurrencyResponse fromEntity(Currency c) {
        CurrencyResponse r = new CurrencyResponse();
        r.code = c.getCode();
        r.name = c.getName();
        r.symbol = c.getSymbol();
        r.decimalPlaces = c.getDecimalPlaces();
        r.isActive = c.getIsActive();
        return r;
    }
    // getters
}
```

- [ ] **Step 5: Write MasterDataMapper (partial)**

```java
package com.banking.masterdata.mapper;

import com.banking.masterdata.domain.entity.Currency;
import com.banking.masterdata.dto.request.CreateCurrencyRequest;
import org.springframework.stereotype.Component;

@Component
public class MasterDataMapper {
    public Currency toEntity(CreateCurrencyRequest request) {
        return new Currency(request.getCode(), request.getName(),
                          request.getSymbol(), request.getDecimalPlaces());
    }
}
```

- [ ] **Step 6: Write CurrencyService**

```java
package com.banking.masterdata.service;

@Service
@Transactional
public class CurrencyService {
    private final CurrencyRepository currencyRepository;
    private final MasterDataMapper mapper;

    public CurrencyService(CurrencyRepository currencyRepository, MasterDataMapper mapper) {
        this.currencyRepository = currencyRepository;
        this.mapper = mapper;
    }

    public CurrencyResponse createCurrency(CreateCurrencyRequest request) {
        if (currencyRepository.existsByCode(request.getCode())) {
            throw new CurrencyAlreadyExistsException(request.getCode());
        }
        Currency currency = mapper.toEntity(request);
        return CurrencyResponse.fromEntity(currencyRepository.save(currency));
    }

    @Transactional(readOnly = true)
    public CurrencyResponse getCurrency(String code) {
        Currency currency = currencyRepository.findById(code)
            .orElseThrow(() -> new CurrencyNotFoundException(code));
        return CurrencyResponse.fromEntity(currency);
    }

    @Transactional(readOnly = true)
    public List<CurrencyResponse> getAllCurrencies(boolean activeOnly) {
        List<Currency> currencies = activeOnly ?
            currencyRepository.findByIsActiveTrue() : currencyRepository.findAll();
        return currencies.stream().map(CurrencyResponse::fromEntity).toList();
    }

    public CurrencyResponse updateCurrency(String code, UpdateCurrencyRequest request) {
        Currency currency = currencyRepository.findById(code)
            .orElseThrow(() -> new CurrencyNotFoundException(code));
        if (request.getName() != null) currency.setName(request.getName());
        if (request.getSymbol() != null) currency.setSymbol(request.getSymbol());
        return CurrencyResponse.fromEntity(currencyRepository.save(currency));
    }

    public CurrencyResponse deactivateCurrency(String code) {
        Currency currency = currencyRepository.findById(code)
            .orElseThrow(() -> new CurrencyNotFoundException(code));
        currency.setIsActive(false);
        return CurrencyResponse.fromEntity(currencyRepository.save(currency));
    }
}
```

- [ ] **Step 7: Write CurrencyController**

```java
package com.banking.masterdata.controller;

@RestController
@RequestMapping("/api/master-data/currencies")
public class CurrencyController {
    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @PostMapping
    public ResponseEntity<CurrencyResponse> create(@Valid @RequestBody CreateCurrencyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(currencyService.createCurrency(request));
    }

    @GetMapping
    public ResponseEntity<List<CurrencyResponse>> getAll(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(currencyService.getAllCurrencies(activeOnly));
    }

    @GetMapping("/{code}")
    public ResponseEntity<CurrencyResponse> getByCode(@PathVariable String code) {
        return ResponseEntity.ok(currencyService.getCurrency(code));
    }

    @PutMapping("/{code}")
    public ResponseEntity<CurrencyResponse> update(@PathVariable String code,
                                                    @Valid @RequestBody UpdateCurrencyRequest request) {
        return ResponseEntity.ok(currencyService.updateCurrency(code, request));
    }

    @PutMapping("/{code}/deactivate")
    public ResponseEntity<CurrencyResponse> deactivate(@PathVariable String code) {
        return ResponseEntity.ok(currencyService.deactivateCurrency(code));
    }
}
```

- [ ] **Step 8: Write unit tests for CurrencyService**

```java
@SpringBootTest
class CurrencyServiceTest {
    @Autowired CurrencyService currencyService;

    @Test // BDD S1.1: Create a new currency (Happy Path)
    void createCurrency_success() { /* test */ }

    @Test // BDD S1.9: Create duplicate currency code (Negative)
    void createCurrency_duplicateCode_throws() { /* test */ }

    @Test // BDD S1.4: List all active currencies (Happy Path)
    void getAllCurrencies_activeOnly_excludesInactive() { /* test */ }

    @Test // BDD S1.6: Update a currency (Happy Path)
    void updateCurrency_success() { /* test */ }

    @Test // BDD S1.7: Deactivate a currency (Happy Path)
    void deactivateCurrency_success() { /* test */ }
}
```

- [ ] **Step 9: Run tests, ensure pass**

```bash
./gradlew test --tests "com.banking.masterdata.service.CurrencyServiceTest" -v
```

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/banking/masterdata/
git commit -m "feat(masterdata): add Currency entity, repository, service, controller with tests"
```

---

## Task 3: Country Entity

**Files:**
- Create: `src/main/java/com/banking/masterdata/domain/entity/Country.java`
- Create: `src/main/java/com/banking/masterdata/repository/CountryRepository.java`
- Create: `src/main/java/com/banking/masterdata/exception/CountryNotFoundException.java`
- Create: `src/main/java/com/banking/masterdata/dto/request/CreateCountryRequest.java`
- Create: `src/main/java/com/banking/masterdata/dto/response/CountryResponse.java`
- Modify: `src/main/java/com/banking/masterdata/mapper/MasterDataMapper.java`
- Create: `src/main/java/com/banking/masterdata/service/CountryService.java`
- Create: `src/main/java/com/banking/masterdata/controller/CountryController.java`

**BDD Reference:** S2.1, S2.2, S2.3, S2.4

- [ ] **Step 1: Write Country entity**

```java
@Entity
@Table(name = "countries")
public class Country {
    @Id
    @Column(name = "iso_code", length = 2)
    private String isoCode;

    @Column(nullable = false)
    private String name;

    @Column(length = 20)
    private String region;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Embedded
    private AuditFields audit = new AuditFields();

    protected Country() {}

    public Country(String isoCode, String name, String region) {
        this.isoCode = isoCode;
        this.name = name;
        this.region = region;
    }
    // getters/setters
}
```

- [ ] **Step 2: Write CountryRepository**

```java
@Repository
public interface CountryRepository extends JpaRepository<Country, String> {
    List<Country> findByIsActiveTrue();
}
```

- [ ] **Step 3: Write exception, DTO, mapper addition**

Same pattern as Currency. Add `CountryResponse.fromEntity()`, `CreateCountryRequest`, `CountryNotFoundException`.

- [ ] **Step 4: Write CountryService**

Same pattern as CurrencyService — create, get, getAll, deactivate.

- [ ] **Step 5: Write CountryController**

```java
@RestController
@RequestMapping("/api/master-data/countries")
public class CountryController {
    // POST /, GET /, GET /{isoCode}, PUT /{isoCode}/deactivate
}
```

- [ ] **Step 6: Write tests**

```java
@SpringBootTest
class CountryServiceTest {
    @Test // BDD S2.1: Create a country (Happy Path)
    void createCountry_success() { /* test */ }

    @Test // BDD S2.2: Create duplicate country code (Negative)
    void createCountry_duplicateCode_throws() { /* test */ }

    @Test // BDD S2.3: List active countries (Happy Path)
    void getAllCountries_activeOnly() { /* test */ }
}
```

- [ ] **Step 7: Run tests, commit**

```bash
./gradlew test --tests "com.banking.masterdata.service.CountryServiceTest" -v
git add .
git commit -m "feat(masterdata): add Country entity, repository, service, controller"
```

---

## Task 4: Industry Entity

**Files:**
- Create: `src/main/java/com/banking/masterdata/domain/entity/Industry.java`
- Create: `src/main/java/com/banking/masterdata/repository/IndustryRepository.java`
- Create: `src/main/java/com/banking/masterdata/exception/IndustryNotFoundException.java`
- Create: `src/main/java/com/banking/masterdata/dto/request/CreateIndustryRequest.java`
- Create: `src/main/java/com/banking/masterdata/dto/response/IndustryResponse.java`
- Modify: `src/main/java/com/banking/masterdata/mapper/MasterDataMapper.java`
- Create: `src/main/java/com/banking/masterdata/service/IndustryService.java`
- Create: `src/main/java/com/banking/masterdata/controller/IndustryController.java`

**BDD Reference:** S3.1, S3.2, S3.3

- [ ] **Step 1: Write Industry entity**

```java
@Entity
@Table(name = "industries")
public class Industry {
    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_code", length = 10)
    private String parentCode;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Embedded
    private AuditFields audit = new AuditFields();

    // constructors, getters/setters
}
```

- [ ] **Step 2: Write IndustryRepository**

```java
@Repository
public interface IndustryRepository extends JpaRepository<Industry, String> {
    List<Industry> findByIsActiveTrue();
    List<Industry> findByParentCode(String parentCode);
}
```

- [ ] **Step 3: Write service, controller, tests**

Same pattern. Service validates parent exists on create (BDD S3.3).

- [ ] **Step 4: Run tests, commit**

```bash
git add .
git commit -m "feat(masterdata): add Industry entity, repository, service, controller"
```

---

## Task 5: Exchange Rate Entity

**Files:**
- Create: `src/main/java/com/banking/masterdata/domain/entity/ExchangeRate.java`
- Create: `src/main/java/com/banking/masterdata/repository/ExchangeRateRepository.java`
- Create: `src/main/java/com/banking/masterdata/exception/ExchangeRateNotFoundException.java`
- Create: `src/main/java/com/banking/masterdata/dto/request/CreateExchangeRateRequest.java`
- Create: `src/main/java/com/banking/masterdata/dto/response/ExchangeRateResponse.java`
- Modify: `src/main/java/com/banking/masterdata/mapper/MasterDataMapper.java`
- Create: `src/main/java/com/banking/masterdata/service/ExchangeRateService.java`
- Create: `src/main/java/com/banking/masterdata/controller/ExchangeRateController.java`

**BDD Reference:** S4.1-S4.9

- [ ] **Step 1: Write ExchangeRate entity**

```java
@Entity
@Table(name = "exchange_rates", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"base_currency", "target_currency", "effective_date"})
})
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_currency", nullable = false)
    private Currency baseCurrency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_currency", nullable = false)
    private Currency targetCurrency;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Embedded
    private AuditFields audit = new AuditFields();
    // constructors, getters/setters
}
```

- [ ] **Step 2: Write ExchangeRateRepository**

```java
@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findTopByBaseCurrencyCodeAndTargetCurrencyCodeOrderByEffectiveDateDesc(
        String baseCurrency, String targetCurrency);

    List<ExchangeRate> findByBaseCurrencyCodeAndTargetCurrencyCodeAndEffectiveDateBetween(
        String base, String target, LocalDate from, LocalDate to);
}
```

- [ ] **Step 3: Write ExchangeRateService**

Key methods:
- `createExchangeRate` — validates currencies exist (BDD S4.3, S4.4, S4.5)
- `getLatestRate` — queries latest effective date (BDD S4.6)
- `convertAmount` — multiplies by latest rate, applies decimal places (BDD S4.8, S4.9)

- [ ] **Step 4: Write controller and tests**

```java
@RestController
@RequestMapping("/api/master-data/exchange-rates")
public class ExchangeRateController {
    // POST /, GET /latest?base=X&target=Y, GET /convert?base=X&target=Y&amount=100
}
```

- [ ] **Step 5: Run tests, commit**

```bash
git add .
git commit -m "feat(masterdata): add ExchangeRate entity, service with latest query and conversion"
```

---

## Task 6: Holiday Entity

**Files:**
- Create: `src/main/java/com/banking/masterdata/domain/entity/Holiday.java`
- Create: `src/main/java/com/banking/masterdata/repository/HolidayRepository.java`
- Create: `src/main/java/com/banking/masterdata/exception/HolidayNotFoundException.java`
- Create: `src/main/java/com/banking/masterdata/dto/request/CreateHolidayRequest.java`
- Create: `src/main/java/com/banking/masterdata/dto/response/HolidayResponse.java`
- Modify: `src/main/java/com/banking/masterdata/mapper/MasterDataMapper.java`
- Create: `src/main/java/com/banking/masterdata/service/HolidayService.java`
- Create: `src/main/java/com/banking/masterdata/controller/HolidayController.java`

**BDD Reference:** S5.1-S5.8

- [ ] **Step 1: Write Holiday entity**

```java
@Entity
@Table(name = "holidays", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"country_code", "holiday_date"})
})
public class Holiday {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_code", nullable = false)
    private Country country;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(columnDefinition = "TEXT")
    private String description;
    // constructors, getters/setters
}
```

- [ ] **Step 2: Write HolidayRepository**

```java
@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    List<Holiday> findByCountryCodeAndHolidayDateBetween(String countryCode, LocalDate from, LocalDate to);
    boolean existsByCountryCodeAndHolidayDate(String countryCode, LocalDate date);
}
```

- [ ] **Step 3: Write HolidayService**

Key methods:
- `createHoliday` — validates country exists (BDD S5.2)
- `isHoliday` — returns boolean (BDD S5.6, S5.7)
- `getHolidays` — by country + year (BDD S5.4)

- [ ] **Step 4: Write controller and tests**

```java
@RestController
@RequestMapping("/api/master-data/holidays")
public class HolidayController {
    // POST /, GET /?countryCode=X&year=2026, GET /check?countryCode=X&date=2026-01-01
}
```

- [ ] **Step 5: Run tests, commit**

```bash
git add .
git commit -m "feat(masterdata): add Holiday entity, service with isHoliday query"
```

---

## Task 7: Branch Entity

**Files:**
- Create: `src/main/java/com/banking/masterdata/domain/entity/Branch.java`
- Create: `src/main/java/com/banking/masterdata/repository/BranchRepository.java`
- Create: `src/main/java/com/banking/masterdata/exception/BranchNotFoundException.java`
- Create: `src/main/java/com/banking/masterdata/exception/BranchAlreadyExistsException.java`
- Create: `src/main/java/com/banking/masterdata/dto/request/CreateBranchRequest.java`
- Create: `src/main/java/com/banking/masterdata/dto/response/BranchResponse.java`
- Modify: `src/main/java/com/banking/masterdata/mapper/MasterDataMapper.java`
- Create: `src/main/java/com/banking/masterdata/service/BranchService.java`
- Create: `src/main/java/com/banking/masterdata/controller/BranchController.java`

**BDD Reference:** S6.1-S6.6

- [ ] **Step 1-5: Same pattern as Country — entity with code as PK, service, controller, tests**

Branch has country as FK (ManyToOne).

- [ ] **Step 6: Run tests, commit**

```bash
git add .
git commit -m "feat(masterdata): add Branch entity, service, controller"
```

---

## Task 8: Channel Entity

**Files:**
- Create: `src/main/java/com/banking/masterdata/domain/entity/Channel.java`
- Create: `src/main/java/com/banking/masterdata/repository/ChannelRepository.java`
- Create: `src/main/java/com/banking/masterdata/exception/ChannelNotFoundException.java`
- Create: `src/main/java/com/banking/masterdata/exception/ChannelAlreadyExistsException.java`
- Create: `src/main/java/com/banking/masterdata/dto/request/CreateChannelRequest.java`
- Create: `src/main/java/com/banking/masterdata/dto/response/ChannelResponse.java`
- Create: `src/main/java/com/banking/masterdata/service/ChannelService.java`
- Create: `src/main/java/com/banking/masterdata/controller/ChannelController.java`

**BDD Reference:** S7.1-S7.4

- [ ] **Step 1-4: Simple CRUD entity with code as PK**

- [ ] **Step 5: Run tests, commit**

```bash
git add .
git commit -m "feat(masterdata): add Channel entity, service, controller"
```

---

## Task 9: DocumentType Entity

**Files:**
- Create: `src/main/java/com/banking/masterdata/domain/entity/DocumentType.java`
- Create: `src/main/java/com/banking/masterdata/repository/DocumentTypeRepository.java`
- Create: `src/main/java/com/banking/masterdata/exception/DocumentTypeNotFoundException.java`
- Create: `src/main/java/com/banking/masterdata/exception/DocumentTypeAlreadyExistsException.java`
- Create: `src/main/java/com/banking/masterdata/dto/request/CreateDocumentTypeRequest.java`
- Create: `src/main/java/com/banking/masterdata/dto/response/DocumentTypeResponse.java`
- Create: `src/main/java/com/banking/masterdata/service/DocumentTypeService.java`
- Create: `src/main/java/com/banking/masterdata/controller/DocumentTypeController.java`

**BDD Reference:** S8.1-S8.6

- [ ] **Step 1-4: CRUD entity with code as PK + category field**

- [ ] **Step 5: Run tests, commit**

```bash
git add .
git commit -m "feat(masterdata): add DocumentType entity, service, controller"
```

---

## Task 10: Exception Handler

**Files:**
- Create: `src/main/java/com/banking/masterdata/controller/MasterDataExceptionHandler.java`

- [ ] **Step 1: Write exception handler**

Follow pattern from `ProductExceptionHandler.java`:
```java
@RestControllerAdvice(basePackages = "com.banking.masterdata")
public class MasterDataExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MasterDataExceptionHandler.class);

    @ExceptionHandler(CurrencyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCurrencyNotFound(CurrencyNotFoundException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CurrencyAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCurrencyExists(CurrencyAlreadyExistsException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex.getMessage(), HttpStatus.CONFLICT);
    }

    // ... handlers for all other exceptions

    private ResponseEntity<ErrorResponse> buildErrorResponse(String code, String msg, HttpStatus status) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, msg, status.value(), Instant.now()));
    }

    static class ErrorResponse {
        private String errorCode;
        private String message;
        private int status;
        private Instant timestamp;
        // constructor, getters
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "feat(masterdata): add MasterDataExceptionHandler with all exception mappings"
```

---

## Task 11: MasterDataQueryService

**Files:**
- Create: `src/main/java/com/banking/masterdata/service/MasterDataQueryService.java`

- [ ] **Step 1: Write read-only query service for downstream modules**

```java
@Service
@Transactional(readOnly = true)
public class MasterDataQueryService {
    private final CurrencyRepository currencyRepository;
    private final CountryRepository countryRepository;
    private final ExchangeRateService exchangeRateService;
    private final HolidayService holidayService;
    private final BranchRepository branchRepository;
    private final ChannelRepository channelRepository;
    private final DocumentTypeRepository documentTypeRepository;

    // All read methods delegating to repositories
    public List<CurrencyResponse> getActiveCurrencies() { ... }
    public List<CountryResponse> getActiveCountries() { ... }
    public BigDecimal convertAmount(String base, String target, BigDecimal amount) { ... }
    public boolean isHoliday(String countryCode, LocalDate date) { ... }
    public List<BranchResponse> getActiveBranches() { ... }
    public List<ChannelResponse> getActiveChannels() { ... }
    public List<DocumentTypeResponse> getActiveDocumentTypes(String category) { ... }
}
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "feat(masterdata): add MasterDataQueryService for downstream module access"
```

---

## Task 12: Flyway Migration — Schema

**Files:**
- Create: `src/main/resources/db/migration/V6__create_master_data_schema.sql`

- [ ] **Step 1: Write schema migration**

Use the SQL from design spec Section 6. Ensure it runs after V5.

- [ ] **Step 2: Run migration**

```bash
./gradlew flywayMigrate
```

- [ ] **Step 3: Verify tables exist**

```bash
./gradlew flywayInfo
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/migration/V6__create_master_data_schema.sql
git commit -m "feat(masterdata): add Flyway migration V6 for master data schema"
```

---

## Task 13: Flyway Migration — Seed Data

**Files:**
- Create: `src/main/resources/db/migration/V7__seed_master_data.sql`

- [ ] **Step 1: Write seed data**

Include:
- Major currencies (USD, EUR, GBP, SGD, JPY, CAD, AUD, CHF, CNY, HKD)
- Common countries (US, UK, SG, HK, JP, CN, DE, FR, AU, CA)
- Standard channels (BRANCH, ONLINE, MOBILE, ATM, PHONE)
- Common document types (PASSPORT, DRIVERS_LICENSE, UTILITY_BILL, INC_CERT, ARTICLES_OF_INC)

- [ ] **Step 2: Run migration, verify data**

```bash
./gradlew flywayMigrate
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/db/migration/V7__seed_master_data.sql
git commit -m "feat(masterdata): seed currencies, countries, channels, document types"
```

---

## Task 14: Integration Tests

**Files:**
- Create: `src/test/java/com/banking/masterdata/controller/CurrencyControllerTest.java`
- Create: `src/test/java/com/banking/masterdata/controller/ExchangeRateControllerTest.java`
- Create: `src/test/java/com/banking/masterdata/controller/HolidayControllerTest.java`

- [ ] **Step 1: Write CurrencyControllerTest (MockMvc)**

```java
@SpringBootTest
@AutoConfigureMockMvc
class CurrencyControllerTest {
    @Test // BDD S1.1
    void createCurrency_returns201() { /* MockMvc POST /api/master-data/currencies */ }

    @Test // BDD S1.9
    void createDuplicateCurrency_returns409() { /* test */ }

    @Test // BDD S1.4
    void listActiveCurrencies_returns200() { /* test */ }
}
```

- [ ] **Step 2: Write ExchangeRateControllerTest**

```java
@SpringBootTest
@AutoConfigureMockMvc
class ExchangeRateControllerTest {
    @Test // BDD S4.6
    void getLatestRate_returnsMostRecent() { /* test */ }

    @Test // BDD S4.8
    void convertAmount_returnsCorrectConversion() { /* test */ }
}
```

- [ ] **Step 3: Write HolidayControllerTest**

```java
@SpringBootTest
@AutoConfigureMockMvc
class HolidayControllerTest {
    @Test // BDD S5.6
    void checkIsHoliday_returnsTrue() { /* test */ }

    @Test // BDD S5.7
    void checkNonHoliday_returnsFalse() { /* test */ }
}
```

- [ ] **Step 4: Run all tests**

```bash
./gradlew test --tests "com.banking.masterdata.*" -v
```

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/banking/masterdata/
git commit -m "test(masterdata): add integration tests for Currency, ExchangeRate, Holiday"
```

---

## Task 15: Frontend — Types & Service

**Files:**
- Create: `frontend/src/types/masterData.types.ts`
- Create: `frontend/src/services/masterDataService.ts`

- [ ] **Step 1: Write TypeScript types**

```typescript
// masterData.types.ts
export interface Currency {
  code: string;
  name: string;
  symbol: string;
  decimalPlaces: number;
  isActive: boolean;
}

export interface Country {
  isoCode: string;
  name: string;
  region: string;
  isActive: boolean;
}

export interface ExchangeRate {
  id: number;
  baseCurrency: string;
  targetCurrency: string;
  rate: number;
  effectiveDate: string;
}

export interface Holiday {
  id: number;
  countryCode: string;
  holidayDate: string;
  description: string;
}

export interface Branch {
  code: string;
  name: string;
  countryCode: string;
  address: string;
  isActive: boolean;
}

export interface Channel {
  code: string;
  name: string;
  isActive: boolean;
}

export interface DocumentType {
  code: string;
  name: string;
  category: string;
  isActive: boolean;
}

export interface CreateCurrencyRequest {
  code: string;
  name: string;
  symbol?: string;
  decimalPlaces: number;
}
```

- [ ] **Step 2: Write API service**

```typescript
// masterDataService.ts
import apiClient from './apiClient';
import { Currency, Country, ExchangeRate, Holiday, Branch, Channel, DocumentType, CreateCurrencyRequest } from '../types/masterData.types';

const BASE = '/api/master-data';

export const masterDataService = {
  // Currency
  getCurrencies(activeOnly = false): Promise<Currency[]> {
    return apiClient.get(`${BASE}/currencies`, { params: { activeOnly } }).then(r => r.data);
  },
  getCurrency(code: string): Promise<Currency> {
    return apiClient.get(`${BASE}/currencies/${code}`).then(r => r.data);
  },
  createCurrency(request: CreateCurrencyRequest): Promise<Currency> {
    return apiClient.post(`${BASE}/currencies`, request).then(r => r.data);
  },
  updateCurrency(code: string, request: Partial<CreateCurrencyRequest>): Promise<Currency> {
    return apiClient.put(`${BASE}/currencies/${code}`, request).then(r => r.data);
  },
  deactivateCurrency(code: string): Promise<Currency> {
    return apiClient.put(`${BASE}/currencies/${code}/deactivate`).then(r => r.data);
  },

  // Country
  getCountries(activeOnly = false): Promise<Country[]> {
    return apiClient.get(`${BASE}/countries`, { params: { activeOnly } }).then(r => r.data);
  },

  // Exchange Rate
  getLatestRate(base: string, target: string): Promise<ExchangeRate> {
    return apiClient.get(`${BASE}/exchange-rates/latest`, { params: { base, target } }).then(r => r.data);
  },
  convertAmount(base: string, target: string, amount: number): Promise<number> {
    return apiClient.get(`${BASE}/exchange-rates/convert`, { params: { base, target, amount } }).then(r => r.data);
  },

  // Holiday
  getHolidays(countryCode: string, year: number): Promise<Holiday[]> {
    return apiClient.get(`${BASE}/holidays`, { params: { countryCode, year } }).then(r => r.data);
  },
  isHoliday(countryCode: string, date: string): Promise<boolean> {
    return apiClient.get(`${BASE}/holidays/check`, { params: { countryCode, date } }).then(r => r.data);
  },

  // Branch
  getBranches(activeOnly = true): Promise<Branch[]> {
    return apiClient.get(`${BASE}/branches`, { params: { activeOnly } }).then(r => r.data);
  },

  // Channel
  getChannels(): Promise<Channel[]> {
    return apiClient.get(`${BASE}/channels`).then(r => r.data);
  },

  // Document Type
  getDocumentTypes(category?: string): Promise<DocumentType[]> {
    return apiClient.get(`${BASE}/document-types`, { params: { category } }).then(r => r.data);
  },
};
```

- [ ] **Step 3: Update types/index.ts barrel export**

```typescript
export * from './masterData.types';
```

- [ ] **Step 4: Update services/index.ts barrel export**

```typescript
export { masterDataService } from './masterDataService';
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/types/masterData.types.ts frontend/src/services/masterDataService.ts frontend/src/types/index.ts frontend/src/services/index.ts
git commit -m "feat(masterdata-frontend): add TypeScript types and API service"
```

---

## Task 16: Frontend — Redux Slice

**Files:**
- Create: `frontend/src/store/slices/masterDataSlice.ts`
- Modify: `frontend/src/store/index.ts`

- [ ] **Step 1: Write masterDataSlice**

```typescript
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { masterDataService } from '../../services/masterDataService';
import { Currency, Country, Branch, Channel, DocumentType } from '../../types/masterData.types';

interface MasterDataState {
  currencies: Currency[];
  countries: Country[];
  branches: Branch[];
  channels: Channel[];
  documentTypes: DocumentType[];
  loading: boolean;
  error: string | null;
}

const initialState: MasterDataState = {
  currencies: [], countries: [], branches: [],
  channels: [], documentTypes: [],
  loading: false, error: null,
};

export const fetchCurrencies = createAsyncThunk('masterData/fetchCurrencies', async (activeOnly = false) => {
  return await masterDataService.getCurrencies(activeOnly);
});
// ... similar thunks for countries, branches, channels, documentTypes

const masterDataSlice = createSlice({
  name: 'masterData',
  initialState,
  reducers: { clearError: (state) => { state.error = null; } },
  extraReducers: (builder) => {
    builder
      .addCase(fetchCurrencies.pending, (state) => { state.loading = true; })
      .addCase(fetchCurrencies.fulfilled, (state, action) => {
        state.loading = false;
        state.currencies = action.payload;
      })
      .addCase(fetchCurrencies.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message || 'Failed to fetch currencies';
      });
    // ... similar for other thunks
  },
});

export const { clearError } = masterDataSlice.actions;
export default masterDataSlice.reducer;
```

- [ ] **Step 2: Register in store/index.ts**

```typescript
import masterDataReducer from './slices/masterDataSlice';
// Add to configureStore reducer map: masterData: masterDataReducer
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/store/slices/masterDataSlice.ts frontend/src/store/index.ts
git commit -m "feat(masterdata-frontend): add Redux slice with async thunks"
```

---

## Task 17: Frontend — Pages

**Files:**
- Create: `frontend/src/pages/master-data/MasterDataListPage.tsx`
- Create: `frontend/src/pages/master-data/MasterDataFormPage.tsx`
- Create: `frontend/src/pages/master-data/ExchangeRatePage.tsx`
- Create: `frontend/src/pages/master-data/index.ts`

- [ ] **Step 1: Write MasterDataListPage**

```tsx
// Renders a tabbed view: Currencies, Countries, Industries, Branches, Channels, Document Types
// Each tab has an Ant Design Table with columns, search, pagination
// "Create" button opens MasterDataFormPage modal or navigates to form
```

- [ ] **Step 2: Write MasterDataFormPage**

```tsx
// Ant Design Form for creating/editing entities
// Dynamic form based on entity type (passed via route param or prop)
```

- [ ] **Step 3: Write ExchangeRatePage**

```tsx
// Table of exchange rates with create form
// Currency converter widget (base, target, amount → result)
```

- [ ] **Step 4: Add route in App.tsx or routes.tsx**

```tsx
<Route path="/master-data" element={<MasterDataListPage />} />
<Route path="/master-data/exchange-rates" element={<ExchangeRatePage />} />
```

- [ ] **Step 5: Add sidebar entry**

```tsx
// In sidebar config, add "Master Data" menu item under System section
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/master-data/
git commit -m "feat(masterdata-frontend): add list, form, and exchange rate pages"
```

---

## Task 18: Run All Tests & Verify

- [ ] **Step 1: Run backend tests**

```bash
./gradlew test --tests "com.banking.masterdata.*" -v
```

- [ ] **Step 2: Run full build**

```bash
./gradlew build
```

- [ ] **Step 3: Run frontend tests**

```bash
cd frontend && npm run test:coverage
```

- [ ] **Step 4: Run frontend lint**

```bash
cd frontend && npm run lint
```

- [ ] **Step 5: Verify API manually**

```bash
./gradlew bootRun
# Test: curl http://localhost:8080/api/master-data/currencies
```

- [ ] **Step 6: Final commit**

```bash
git add -A
git commit -m "feat(masterdata): complete Master Data Management module"
```
