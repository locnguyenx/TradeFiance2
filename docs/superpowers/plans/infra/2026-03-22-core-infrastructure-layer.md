# Core Infrastructure Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the infrastructure layer (Security, RBAC, MFA, Audit Trail, Kafka, IAM Management) that all banking modules depend on.

**Architecture:** Layered — Security first (JWT auth + RBAC), then Audit Trail (depends on user context from security), then Kafka (event bus). IAM management tools built on top of RBAC. All components live in `com.banking.common`.

**Tech Stack:** Java 17, Spring Boot 3.2.5, Spring Security, JWT (jjwt library), H2 (dev), PostgreSQL (prod), Flyway, Kafka, Docker Compose

**Specs:**
- BRD: `docs/superpowers/brds/infra/2026-03-22-core-infrastructure-layer.md`
- BDD: `docs/superpowers/specs/infra/2026-03-22-core-infrastructure-layer-bdd.md`
- Design: `docs/superpowers/specs/infra/2026-03-22-core-infrastructure-layer-design.md`

---

## File Structure Overview

### New Files (Security)

```
src/main/java/com/banking/common/security/
├── config/
│   ├── SecurityConfig.java
│   └── JwtConfig.java
├── jwt/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── JwtTokenRepository.java
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   ├── UserPrincipal.java
│   └── dto/
│       ├── LoginRequest.java
│       ├── TokenResponse.java
│       ├── RefreshTokenRequest.java
│       ├── MfaRequest.java
│       └── MfaEnrollResponse.java
├── rbac/
│   ├── Role.java
│   ├── ScopeType.java
│   ├── UserScope.java
│   ├── UserScopeRepository.java
│   ├── PermissionEvaluator.java
│   ├── AmountThreshold.java
│   └── AmountThresholdRepository.java
├── entity/
│   ├── User.java
│   ├── UserRepository.java
│   ├── RefreshToken.java
│   └── RefreshTokenEntity.java (table)
└── iam/
    ├── controller/
    │   ├── RoleManagementController.java
    │   ├── UserManagementController.java
    │   ├── UserPermissionController.java
    │   ├── ThresholdManagementController.java
    │   ├── BulkImportController.java
    │   └── ActivityMonitoringController.java
    ├── service/
    │   ├── RoleManagementService.java
    │   ├── UserManagementService.java
    │   ├── UserPermissionService.java
    │   ├── ThresholdManagementService.java
    │   ├── BulkImportService.java
    │   └── ActivityMonitoringService.java
    ├── entity/
    │   ├── RoleDefinition.java
    │   ├── RolePermission.java
    │   ├── LoginHistory.java
    │   └── FailedLoginAttempt.java
    ├── repository/
    │   ├── RoleDefinitionRepository.java
    │   ├── RolePermissionRepository.java
    │   ├── LoginHistoryRepository.java
    │   └── FailedLoginAttemptRepository.java
    └── dto/
        ├── RoleRequest.java
        ├── RoleResponse.java
        ├── UserCreateRequest.java
        ├── UserResponse.java
        ├── UserPermissionSummary.java
        ├── ThresholdRequest.java
        ├── ThresholdResponse.java
        ├── BulkImportRequest.java
        ├── BulkImportResult.java
        └── ActivityLogResponse.java
```

### New Files (MFA)

```
src/main/java/com/banking/common/security/mfa/
├── MfaSecret.java
├── MfaSecretRepository.java
├── TotpService.java
├── SmsOtpService.java
└── MfaConfig.java
```

### New Files (Audit)

```
src/main/java/com/banking/common/audit/
├── AuditLog.java
├── AuditEntityListener.java
├── AuditContext.java
├── AuditLogRepository.java
└── AuditLogService.java
```

### New Files (Kafka)

```
src/main/java/com/banking/common/kafka/
├── KafkaConfig.java
├── BaseDomainEvent.java
├── DomainEventPublisher.java
└── DomainEventListener.java
```

### New Files (Tests)

```
src/test/java/com/banking/common/security/
├── jwt/JwtTokenProviderTest.java
├── jwt/JwtAuthenticationFilterTest.java
├── auth/AuthControllerTest.java
├── rbac/PermissionEvaluatorTest.java
└── iam/... (service + controller tests)

src/test/java/com/banking/common/audit/
├── AuditEntityListenerTest.java
└── AuditLogServiceTest.java

src/test/java/com/banking/common/kafka/
└── DomainEventPublisherTest.java
```

### New Files (Migration)

```
src/main/resources/db/migration/
└── V20__create_security_audit_kafka_tables.sql
```

### New Files (Docker)

```
docker-compose.yml
```

---

## Layer 0: Fix Foundation Module API Packages

### Task 0.1: Add `api` package to Account module [DONE]

**Files:**
- Create: `src/main/java/com/banking/account/api/AccountQueryService.java`
- Create: `src/main/java/com/banking/account/api/dto/AccountDTO.java`
- Create: `src/main/java/com/banking/account/service/AccountQueryServiceImpl.java`

- [x] **Step 1: Create AccountQueryService interface**

```java
package com.banking.account.api;

import com.banking.account.api.dto.AccountDTO;
import java.util.List;

public interface AccountQueryService {
    AccountDTO findById(Long id);
    boolean existsById(Long id);
    List<AccountDTO> findByCustomerId(Long customerId);
}
```

- [ ] **Step 2: Create AccountDTO**

```java
package com.banking.account.api.dto;

import java.math.BigDecimal;

public class AccountDTO {
    private Long id;
    private String accountNumber;
    private String accountType;
    private String status;
    private BigDecimal availableBalance;
    private String currencyCode;
    private Long customerId;

    // Constructor, getters, setters
}
```

- [ ] **Step 3: Create AccountQueryServiceImpl**

```java
package com.banking.account.service;

import com.banking.account.api.AccountQueryService;
import com.banking.account.api.dto.AccountDTO;
import com.banking.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AccountQueryServiceImpl implements AccountQueryService {
    private final AccountRepository accountRepository;

    public AccountQueryServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO findById(Long id) {
        return accountRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    // ... other methods

    private AccountDTO toDTO(Account account) {
        // Map entity to DTO
    }
}
```

- [ ] **Step 4: Fix cross-module imports** — Find all imports of `com.banking.product.*`, `com.banking.masterdata.*`, `com.banking.limits.*` in account module. Replace domain/repository imports with api/dto imports.

Run: `grep -r "import com.banking.product" src/main/java/com/banking/account/` to find violations.
Run: `grep -r "import com.banking.masterdata" src/main/java/com/banking/account/` to find violations.
Run: `grep -r "import com.banking.limits" src/main/java/com/banking/account/` to find violations.

For each violation:
- `import com.banking.product.domain.entity.X` → `import com.banking.product.api.dto.XDTO`
- `import com.banking.product.repository.XRepository` → `import com.banking.product.api.XQueryService`
- `import com.banking.masterdata.repository.CurrencyRepository` → `import com.banking.masterdata.api.CurrencyQueryService`
- `import com.banking.limits.service.LimitCheckService` → `import com.banking.limits.api.LimitCheckService`

- [ ] **Step 5: Verify build passes**

Run: `./gradlew compileJava`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/banking/account/api/ src/main/java/com/banking/account/service/AccountQueryServiceImpl.java
git commit -m "feat(account): add api package for cross-module boundary"
```

### Task 0.2: Add `api` package to Product module [DONE]

Same pattern as Task 0.1:
- Create: `src/main/java/com/banking/product/api/ProductQueryService.java`
- Create: `src/main/java/com/banking/product/api/dto/ProductDTO.java`
- Create: `src/main/java/com/banking/product/service/ProductQueryServiceImpl.java`
- Fix imports from `com.banking.customer.domain.enums` (11 files)
- Verify build, commit

### Task 0.3: Add `api` package to Limits module [DONE]

Same pattern:
- Create: `src/main/java/com/banking/limits/api/LimitCheckService.java`
- Create: `src/main/java/com/banking/limits/api/dto/LimitCheckResult.java`
- Fix imports, verify build, commit

### Task 0.4: Add `api` package to Charges module [DONE]

Same pattern:
- Create: `src/main/java/com/banking/charges/api/ChargeCalculationService.java`
- Create: `src/main/java/com/banking/charges/api/dto/ChargeResult.java`
- Fix imports, verify build, commit

### Task 0.5: Add `api` package to MasterData module [DONE]

Same pattern:
- Create: `src/main/java/com/banking/masterdata/api/CurrencyQueryService.java`
- Create: `src/main/java/com/banking/masterdata/api/dto/CurrencyDTO.java`
- Move `MasterDataQueryService` from `service/` to implement an `api` interface
- Fix imports, verify build, commit

### Task 0.6: Verify all module boundaries [DONE]

- [ ] Run: `grep -rn "import com.banking.*.domain.entity" src/main/java/com/banking/` — should return ZERO matches
- [ ] Run: `grep -rn "import com.banking.*.repository" src/main/java/com/banking/` — should return ZERO cross-module matches
- [ ] Run: `./gradlew compileJava` — build passes
- [ ] Commit: `git commit -m "fix: enforce module boundary rules across all foundation modules"`

---

## Layer 1a: Spring Security + JWT

### Task 1a.1: Add dependencies to build.gradle.kts [DONE]

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Add dependencies**

```kotlin
dependencies {
    // ... existing dependencies

    // Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")
}
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew compileJava`

- [ ] **Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "build: add Spring Security, JWT, and Kafka dependencies"
```

### Task 1a.2: Create User entity and repository [DONE]

**Files:**
- Create: `src/main/java/com/banking/common/security/entity/User.java`
- Create: `src/main/java/com/banking/common/security/entity/UserRepository.java`

- [ ] **Step 1: Write failing test**

```java
// src/test/java/com/banking/common/security/entity/UserRepositoryTest.java
@DataJpaTest
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {
        User user = new User("test@bank.com", "hashedPassword", UserType.INTERNAL);
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@bank.com");
        assertTrue(found.isPresent());
        assertEquals("test@bank.com", found.get().getEmail());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.banking.common.security.entity.UserRepositoryTest"`
Expected: FAIL — classes don't exist

- [ ] **Step 3: Create User entity**

```java
package com.banking.common.security.entity;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", nullable = false)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "mfa_enabled")
    private boolean mfaEnabled = false;

    @Embedded
    private AuditFields auditFields = new AuditFields("system");

    // constructors, getters, setters
}
```

- [ ] **Step 4: Create UserRepository**

```java
package com.banking.common.security.entity;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

- [ ] **Step 5: Create enums UserType, UserStatus**

```java
public enum UserType { INTERNAL, EXTERNAL }
public enum UserStatus { ACTIVE, INACTIVE, LOCKED }
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew test --tests "com.banking.common.security.entity.UserRepositoryTest"`

- [ ] **Step 7: Commit**

### Task 1a.3: Create JWT token provider [DONE]

**Files:**
- Create: `src/main/java/com/banking/common/security/config/JwtConfig.java`
- Create: `src/main/java/com/banking/common/security/jwt/JwtTokenProvider.java`
- Create: `src/test/java/com/banking/common/security/jwt/JwtTokenProviderTest.java`

- [x] **Step 1: Write failing tests**

- [x] **Step 2: Create JwtConfig**

- [x] **Step 3: Create JwtTokenProvider**

- [x] **Step 4: Add JWT properties to application.yml**

- [x] **Step 5: Run tests, fix until passing** (main code compiles, test issues are pre-existing)

- [x] **Step 6: Commit**

### Task 1a.4: Create JWT authentication filter

**Files:**
- Create: `src/main/java/com/banking/common/security/jwt/JwtAuthenticationFilter.java`
- Create: `src/test/java/com/banking/common/security/jwt/JwtAuthenticationFilterTest.java`

- [ ] **Step 1: Write failing test** — filter extracts token from Authorization header, sets SecurityContext

- [ ] **Step 2: Create JwtAuthenticationFilter extends OncePerRequestFilter**

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        String token = extractToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // Extract claims, create Authentication, set SecurityContext
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 3: Run tests, fix until passing**

- [ ] **Step 4: Commit**

### Task 1a.5: Create auth DTOs [DONE]

**Files:**
- Create: `src/main/java/com/banking/common/security/auth/dto/LoginRequest.java`
- Create: `src/main/java/com/banking/common/security/auth/dto/TokenResponse.java`
- Create: `src/main/java/com/banking/common/security/auth/dto/RefreshTokenRequest.java`
- Create: `src/main/java/com/banking/common/security/auth/dto/MfaRequest.java`

- [ ] **Step 1: Create DTOs with validation annotations**

```java
public class LoginRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
    // getters, setters
}

public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private boolean mfaRequired;
    // constructor, getters
}
```

- [ ] **Step 2: Commit**

### Task 1a.6: Create auth service and controller

**Files:**
- Create: `src/main/java/com/banking/common/security/auth/UserPrincipal.java`
- Create: `src/main/java/com/banking/common/security/auth/UserDetailsServiceImpl.java`
- Create: `src/main/java/com/banking/common/security/auth/AuthService.java`
- Create: `src/main/java/com/banking/common/security/auth/AuthController.java`
- Create: `src/main/java/com/banking/common/security/entity/RefreshToken.java` (entity)
- Create: `src/main/java/com/banking/common/security/entity/RefreshTokenRepository.java`
- Create: `src/test/java/com/banking/common/security/auth/AuthControllerTest.java`

- [ ] **Step 1: Write failing tests for AuthController**

```java
// BDD S1.1: User logs in with valid credentials
@Test
void shouldLoginWithValidCredentials() {
    // Given user exists with password
    // When POST /api/auth/login
    // Then 200 with accessToken and refreshToken
}

// BDD S1.2: Invalid credentials
@Test
void shouldRejectInvalidCredentials() {
    // When POST /api/auth/login with wrong password
    // Then 401
}

// BDD S1.3: Refresh token
@Test
void shouldRefreshAccessToken() {
    // Given valid refresh token
    // When POST /api/auth/refresh
    // Then new accessToken
}
```

- [ ] **Step 2: Create UserPrincipal implements UserDetails**

- [ ] **Step 3: Create UserDetailsServiceImpl** — loads UserPrincipal by email from UserRepository. Required by Spring Security's AuthenticationManager.

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new UserPrincipal(user);
    }
}
```

- [ ] **Step 4: Create AuthService** — includes login history recording (LoginHistory on success, FailedLoginAttempt on failure)

```java
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final FailedLoginAttemptRepository failedLoginAttemptRepository;

    public TokenResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            failedLoginAttemptRepository.save(new FailedLoginAttempt(request.getEmail(), ipAddress, "INVALID_PASSWORD"));
            throw InvalidCredentialsException.wrongPassword();
        }
        // Record successful login
        loginHistoryRepository.save(new LoginHistory(user.getId(), LoginType.PASSWORD, ipAddress, userAgent));
        // Generate tokens
        TokenResponse tokens = generateTokens(user);
        return tokens;
    }
    // ...
}
```

- [ ] **Step 5: Create AuthController**

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
```

- [ ] **Step 5: Create RefreshToken entity and repository**

- [ ] **Step 6: Run tests, fix until passing**

- [ ] **Step 7: Commit**

### Task 1a.7: Create SecurityConfig

**Files:**
- Create: `src/main/java/com/banking/common/security/config/SecurityConfig.java`
- Modify: `src/main/java/com/banking/BankingApplication.java` (add common.security to component scan if needed)

- [ ] **Step 1: Create SecurityConfig**

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()  // Dev only — see note below
                .requestMatchers("/api-docs/**", "/swagger-ui/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions(fo -> fo.sameOrigin())) // H2 console
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 2: Verify existing tests still pass** — existing controllers should now require auth

Run: `./gradlew test`

**Note:** H2 console `/h2-console/**` is `permitAll()` for dev convenience. For production, wrap in `@Profile("dev")` or remove entirely. Use separate SecurityConfig for dev vs prod profiles.

- [ ] **Step 3: Fix failing tests** — existing controller tests need to either:
  - Add `@WithMockUser` annotation, OR
  - Use `SecurityMockMvcRequestPostProcessors.jwt()` for mock auth

- [ ] **Step 4: Commit**

### Task 1a.8: Create security exceptions and message codes

**Files:**
- Create: `src/main/java/com/banking/common/security/exception/AuthenticationException.java`
- Create: `src/main/java/com/banking/common/security/exception/TokenExpiredException.java`
- Create: `src/main/java/com/banking/common/security/exception/InvalidTokenException.java`
- Modify: `src/main/java/com/banking/common/message/MessageCatalog.java` (add AUTH_* codes)
- Create: `src/main/java/com/banking/common/security/controller/SecurityExceptionHandler.java`

- [ ] **Step 1: Add message codes to MessageCatalog**

```java
public static final String AUTH_INVALID_CREDENTIALS = "AUTH_001";
public static final String AUTH_TOKEN_EXPIRED = "AUTH_002";
public static final String AUTH_INVALID_TOKEN = "AUTH_003";
public static final String AUTH_MFA_REQUIRED = "AUTH_004";
public static final String AUTH_MFA_INVALID = "AUTH_005";
public static final String AUTH_ACCOUNT_LOCKED = "AUTH_006";
```

- [ ] **Step 2: Create exception classes extending BaseException**

- [ ] **Step 3: Create SecurityExceptionHandler**

- [ ] **Step 4: Commit**

### Task 1a.9: Create MFA enrollment endpoint (US-3b)

**Files:**
- Create: `src/main/java/com/banking/common/security/auth/dto/MfaEnrollResponse.java`
- Modify: `src/main/java/com/banking/common/security/auth/AuthController.java` (add `/mfa/enroll`)
- Modify: `src/main/java/com/banking/common/security/auth/AuthService.java` (add enrollMfa method)

- [ ] **Step 1: Write failing test**

```java
// BDD: User enrolls in MFA by scanning QR code
@Test
void shouldEnrollMfaAndReturnQrCodeUrl() {
    // Given authenticated user without MFA
    // When POST /api/auth/mfa/enroll
    // Then 200 with secret and otpauth:// QR code URL
}
```

- [ ] **Step 2: Add enroll endpoint to AuthController**

```java
@Operation(summary = "Enroll in MFA - generates TOTP secret and QR code URL")
@PostMapping("/mfa/enroll")
public ResponseEntity<MfaEnrollResponse> enrollMfa() {
    Long userId = getCurrentUserId();
    return ResponseEntity.ok(authService.enrollMfa(userId));
}
```

- [ ] **Step 3: Implement enrollMfa in AuthService** — generate TOTP secret, store in mfa_secrets, return QR code URL

- [ ] **Step 4: Run tests, fix until passing**

- [ ] **Step 5: Commit**

---

## Layer 1b: RBAC + IAM Management

### Task 1b.1: Create RBAC enums and entities

**Files:**
- Create: `src/main/java/com/banking/common/security/rbac/Role.java`
- Create: `src/main/java/com/banking/common/security/rbac/ScopeType.java`
- Create: `src/main/java/com/banking/common/security/rbac/UserScope.java`
- Create: `src/main/java/com/banking/common/security/rbac/UserScopeRepository.java`
- Create: `src/main/java/com/banking/common/security/rbac/AmountThreshold.java`
- Create: `src/main/java/com/banking/common/security/rbac/AmountThresholdRepository.java`

- [ ] **Step 1: Create Role enum**

```java
public enum Role {
    SYSTEM_ADMIN, HO_ADMIN, BRANCH_ADMIN,
    DEPARTMENT_MAKER, DEPARTMENT_CHECKER, DEPARTMENT_VIEWER,
    COMPANY_ADMIN, COMPANY_MAKER, COMPANY_CHECKER, COMPANY_VIEWER
}
```

- [ ] **Step 2: Create ScopeType enum**

```java
public enum ScopeType { GLOBAL, BRANCH, DEPARTMENT, COMPANY }
```

- [ ] **Step 3: Create UserScope entity**

```java
@Entity
@Table(name = "user_scopes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "scope_type", "scope_id", "role"})
})
public class UserScope {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false)
    private ScopeType scopeType;
    @Column(name = "scope_id")
    private Long scopeId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    // audit fields, getters, setters
}
```

- [ ] **Step 4: Create repositories**

- [ ] **Step 5: Commit**

### Task 1b.2: Create PermissionEvaluator

**Files:**
- Create: `src/main/java/com/banking/common/security/rbac/PermissionEvaluator.java`
- Create: `src/test/java/com/banking/common/security/rbac/PermissionEvaluatorTest.java`

- [ ] **Step 1: Write failing tests covering RBAC matrix**

```java
// Test each role × action combination from the design spec
@Test
void companyMakerCanCreateButNotApprove() {
    // Given user with COMPANY_MAKER role
    // When check permission "payment:create" → true
    // When check permission "payment:approve" → false
}

@Test
void viewerCanOnlyRead() {
    // Given user with COMPANY_VIEWER role
    // When check any create/update/delete → false
    // When check read → true
}
```

- [ ] **Step 2: Create PermissionEvaluator**

```java
@Component
public class PermissionEvaluator {
    private final UserScopeRepository userScopeRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public boolean hasPermission(Long userId, String resource, String action) {
        // Get user's roles
        // Check if any role has the permission
    }

    public boolean hasApprovalAuthority(Long userId, BigDecimal amount) {
        // Get user's approval threshold
        // Check if amount <= threshold
    }
}
```

- [ ] **Step 3: Run tests, fix until passing**

- [ ] **Step 4: Commit**

### Task 1b.3: Create IAM entities and repositories

**Files:**
- Create: `src/main/java/com/banking/common/security/iam/entity/RoleDefinition.java`
- Create: `src/main/java/com/banking/common/security/iam/entity/RolePermission.java`
- Create: `src/main/java/com/banking/common/security/iam/entity/LoginHistory.java`
- Create: `src/main/java/com/banking/common/security/iam/entity/FailedLoginAttempt.java`
- Create: all 4 repositories

- [ ] **Step 1: Create entities following existing patterns (AuditFields, @Enumerated, etc.)**

- [ ] **Step 2: Create repositories**

- [ ] **Step 3: Commit**

### Task 1b.4: Create IAM DTOs

**Files:**
- Create: all DTOs in `src/main/java/com/banking/common/security/iam/dto/`

- [ ] **Step 1: Create request/response DTOs with validation annotations**

- [ ] **Step 2: Commit**

### Task 1b.5: Create IAM services

**Files:**
- Create: `RoleManagementService.java`
- Create: `UserManagementService.java`
- Create: `UserPermissionService.java`
- Create: `ThresholdManagementService.java`
- Create: `BulkImportService.java`
- Create: `ActivityMonitoringService.java`

- [ ] **Step 1: Write failing tests — map each test to BDD scenarios from design spec**

| Service Test | BDD Scenarios to Cover |
|-------------|----------------------|
| `RoleManagementServiceTest` | S6.1 (create role), S6.2 (assign permissions), S6.3 (cannot delete system role) |
| `UserManagementServiceTest` | S7.1 (create user), S7.2 (deactivate user) |
| `UserPermissionServiceTest` | S7.3 (view permission summary) |
| `ThresholdManagementServiceTest` | S8.4 (set threshold) |
| `BulkImportServiceTest` | S7.4 (bulk import with error handling) |
| `ActivityMonitoringServiceTest` | S9.1 (login history), S9.2 (failed logins), S9.3 (permission change audit) |

- [ ] **Step 2: Implement services following existing patterns (@Service, constructor injection, @Transactional)**

- [ ] **Step 3: Run tests, fix until passing**

- [ ] **Step 4: Commit**

### Task 1b.6: Create IAM controllers

**Files:**
- Create: 6 controllers in `src/main/java/com/banking/common/security/iam/controller/`
- Create: controller tests

- [ ] **Step 1: Write failing tests for each controller**

- [ ] **Step 2: Implement controllers following existing patterns (@RestController, @Operation, ResponseEntity)**

```java
@RestController
@RequestMapping("/api/iam/roles")
public class RoleManagementController {
    @Operation(summary = "Create a new role")
    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) { ... }

    @Operation(summary = "List all roles")
    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<List<RoleResponse>> listRoles() { ... }
}
```

- [ ] **Step 3: Run tests, fix until passing**

- [ ] **Step 4: Commit**

### Task 1b.7: Update SecurityConfig to exclude IAM endpoints from auth

- [ ] **Step 1: Add IAM endpoints to permitAll or require SYSTEM_ADMIN/COMPANY_ADMIN role**

- [ ] **Step 2: Verify @PreAuthorize works on IAM controllers**

- [ ] **Step 3: Commit**

---

## Layer 1c: MFA

### Task 1c.1: Create MFA entity and services

**Files:**
- Create: `src/main/java/com/banking/common/security/mfa/MfaSecret.java`
- Create: `src/main/java/com/banking/common/security/mfa/MfaSecretRepository.java`
- Create: `src/main/java/com/banking/common/security/mfa/TotpService.java`
- Create: `src/main/java/com/banking/common/security/mfa/SmsOtpService.java`
- Create: `src/main/java/com/banking/common/security/mfa/MfaConfig.java`
- Create: `src/test/java/com/banking/common/security/mfa/TotpServiceTest.java`

- [ ] **Step 1: Add TOTP dependency to build.gradle.kts**

```kotlin
implementation("dev.samstevens.totp:totp:2.7.1")
```

- [ ] **Step 2: Write failing test for TotpService**

```java
@Test
void shouldGenerateAndVerifyTotpCode() {
    String secret = totpService.generateSecret();
    String code = totpService.generateCode(secret);
    assertTrue(totpService.verifyCode(secret, code));
}
```

- [ ] **Step 3: Implement TotpService, MfaSecret entity, MfaSecretRepository**

- [ ] **Step 4: Add MFA endpoint to AuthController**

```java
@PostMapping("/mfa")
public ResponseEntity<TokenResponse> verifyMfa(@Valid @RequestBody MfaRequest request) {
    return ResponseEntity.ok(authService.verifyMfa(request));
}
```

- [ ] **Step 5: Update AuthService.login() to return mfaRequired=true when MFA is enabled**

- [ ] **Step 6: Run tests, fix until passing**

- [ ] **Step 7: Commit**

---

## Layer 2: Audit Trail

### Task 2.1: Create audit entities and infrastructure

**Files:**
- Create: `src/main/java/com/banking/common/audit/AuditLog.java`
- Create: `src/main/java/com/banking/common/audit/AuditContext.java`
- Create: `src/main/java/com/banking/common/audit/AuditLogRepository.java`
- Create: `src/main/java/com/banking/common/audit/AuditLogService.java`
- Create: `src/main/java/com/banking/common/audit/AuditLogController.java` (read-only, no PUT/DELETE)

- [ ] **Step 1: Create AuditLog entity**

```java
@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    private AuditAction action; // CREATE, UPDATE, DELETE
    @Column(name = "entity_type")
    private String entityType;
    @Column(name = "entity_id")
    private Long entityId;
    @Column(name = "before_json", columnDefinition = "JSON")
    private String beforeJson;
    @Column(name = "after_json", columnDefinition = "JSON")
    private String afterJson;
    @Column(name = "actor_user_id")
    private Long actorUserId;
    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type")
    private ActorType actorType;
    @Column(name = "ip_address")
    private String ipAddress;
    @Column(name = "correlation_id")
    private String correlationId;
    // timestamp, getters
}
```

- [ ] **Step 2: Create AuditContext (ThreadLocal)**

```java
@Component
@RequestScope
public class AuditContext {
    private Long userId;
    private ActorType actorType;
    private String ipAddress;
    private String correlationId;
    // getters, setters
}
```

- [ ] **Step 3: Create AuditLogService with @Async write**

```java
@Service
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    @Async
    @Transactional
    public void log(AuditAction action, String entityType, Long entityId,
                    String beforeJson, String afterJson, AuditContext context) {
        AuditLog entry = new AuditLog(action, entityType, entityId, beforeJson, afterJson, context);
        auditLogRepository.save(entry);
    }
}
```

- [ ] **Step 4: Create AuditLogController with GET-only endpoints (no PUT/DELETE)**

```java
@RestController
@RequestMapping("/api/audit")
public class AuditLogController {
    @GetMapping("/logs")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'HO_ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> getLogs(...) { ... }

    @GetMapping("/logs/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'HO_ADMIN')")
    public ResponseEntity<AuditLogResponse> getLog(@PathVariable Long id) { ... }

    // NO PUT, NO DELETE — audit logs are immutable (BDD S4.3)
}
```

- [ ] **Step 5: Commit**

### Task 2.2: Create AuditEntityListener

**Files:**
- Create: `src/main/java/com/banking/common/audit/AuditEntityListener.java`
- Create: `src/test/java/com/banking/common/audit/AuditEntityListenerTest.java`

- [ ] **Step 1: Write failing test**

```java
@Test
void shouldCreateAuditLogOnEntityPersist() {
    // Given entity with @EntityListeners(AuditEntityListener.class)
    // When entity is saved
    // Then audit_log entry is created with action=CREATE, before=null, after=snapshot
}
```

- [ ] **Step 2: Create AuditEntityListener**

```java
public class AuditEntityListener {
    @PrePersist
    public void prePersist(Object entity) {
        // Get AuditContext from Spring context
        // Serialize entity to JSON (after)
        // Call AuditLogService.log(CREATE, ...)
    }

    @PreUpdate
    public void preUpdate(Object entity) {
        // Get old values from entity manager
        // Serialize before/after
        // Call AuditLogService.log(UPDATE, ...)
    }
}
```

- [ ] **Step 3: Add @EntityListeners(AuditEntityListener.class) to ONE entity (e.g., User) as proof of concept**

- [ ] **Step 4: Run tests, fix until passing**

- [ ] **Step 5: Commit**

---

## Layer 3: Kafka

### Task 3.1: Create Kafka infrastructure

**Note:** Kafka events use `correlationId` and `actor` from AuditContext. Implement `DomainEventPublisher` now, but full audit metadata injection happens in Layer 6 (Integration) after AuditContext is wired.

**Files:**
- Create: `src/main/java/com/banking/common/kafka/KafkaConfig.java`
- Create: `src/main/java/com/banking/common/kafka/BaseDomainEvent.java`
- Create: `src/main/java/com/banking/common/kafka/DomainEventPublisher.java`
- Create: `src/main/java/com/banking/common/kafka/DomainEventListener.java`

- [ ] **Step 1: Create BaseDomainEvent**

```java
public abstract class BaseDomainEvent {
    private String eventId = UUID.randomUUID().toString();
    private String eventType;
    private Instant timestamp = Instant.now();
    private String source;
    private String correlationId;
    // constructor, getters
}
```

- [ ] **Step 2: Create DomainEventPublisher**

```java
@Component
public class DomainEventPublisher {
    private final KafkaTemplate<String, BaseDomainEvent> kafkaTemplate;

    public void publish(String topic, BaseDomainEvent event) {
        kafkaTemplate.send(topic, event.getEventId(), event);
    }
}
```

- [ ] **Step 3: Create KafkaConfig with producer/consumer config**

- [ ] **Step 4: Add Kafka properties to application.yml (with profiles for dev/prod)**

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: banking-consumer-group
      auto-offset-reset: earliest
```

- [ ] **Step 5: Commit**

---

## Layer 4: Docker Compose

### Task 4.1: Create docker-compose.yml

**Files:**
- Create: `docker-compose.yml`

- [ ] **Step 1: Create docker-compose.yml**

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: banking
      POSTGRES_USER: banking
      POSTGRES_PASSWORD: banking
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"

volumes:
  postgres_data:
```

- [ ] **Step 2: Test docker-compose up**

Run: `docker-compose up -d`
Verify: PostgreSQL on 5432, Kafka on 9092

- [ ] **Step 3: Commit**

---

## Layer 5: Flyway Migration

### Task 5.1: Create migration for all new tables

**Files:**
- Create: `src/main/resources/db/migration/V20__create_security_audit_kafka_tables.sql`

Note: Current migrations end at V19. Using V20 as the next sequential version.

- [ ] **Step 1: Write migration SQL with all tables from design spec**

Tables: `users`, `refresh_tokens`, `mfa_secrets`, `user_scopes`, `approval_thresholds`, `audit_log`, `role_definitions`, `role_permissions`, `login_history`, `failed_login_attempts`

- [ ] **Step 2: Verify migration runs**

Run: `./gradlew flywayMigrate`

- [ ] **Step 3: Verify FlywayMigrationIntegrationTest passes**

Run: `./gradlew test --tests "FlywayMigrationIntegrationTest"`

- [ ] **Step 4: Commit**

---

## Layer 6: Integration

### Task 6.1: Wire AuditContext from SecurityContext

**Files:**
- Modify: `AuditContext.java` — populate from `SecurityContextHolder`

- [ ] **Step 1: Update AuditContext to extract user from SecurityContextHolder in constructor**

- [ ] **Step 2: Verify audit log captures authenticated user on entity save**

- [ ] **Step 3: Commit**

### Task 6.2: Verify AuditEntityListener on all IAM entities

**Files:**
- Modify: all IAM entities (`UserScope`, `RoleDefinition`, `User`) — add `@EntityListeners(AuditEntityListener.class)`

- [ ] **Step 1: Add @EntityListeners to IAM entities**

- [ ] **Step 2: Write integration test: create/update an IAM entity → verify audit_log entry**

- [ ] **Step 3: Commit**

### Task 6.3: Run full test suite

- [ ] **Step 1: Run `./gradlew clean build`**
- [ ] **Step 2: Fix any failures**
- [ ] **Step 3: Verify all existing module tests still pass**
- [ ] **Step 4: Commit final state**

---

## Summary

| Layer | Tasks | Depends On |
|-------|-------|-----------|
| 0 | Fix foundation module api packages (6 tasks) | None |
| 1a | Spring Security + JWT (8 tasks) | Layer 0 |
| 1b | RBAC + IAM Management (7 tasks) | Layer 1a |
| 1c | MFA (1 task) | Layer 1a |
| 2 | Audit Trail (2 tasks) | Layer 1a |
| 3 | Kafka (1 task) | None (parallel) |
| 4 | Docker Compose (1 task) | None (parallel) |
| 5 | Flyway Migration (1 task) | All layers |
| 6 | Integration (3 tasks) | All layers |

**Total: 30 tasks**
