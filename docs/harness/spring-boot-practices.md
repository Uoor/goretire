# Spring Boot Practices — Backend

Operational patterns for Spring Boot 3.3.5 + Java 17 in this repo. AI reads this when scaffolding controllers, services, repositories, or configuration. Each section states the rule, then a DO / DONT snippet tuned to existing packages (`com.aibrg.{web,service,domain,repo,config}`).

---

## 1. Dependency Injection

### Constructor injection only
- DO:
  ```java
  @Service
  public class RoutingService {
      private final ChannelRepo channelRepo;
      private final BillingService billingService;

      public RoutingService(ChannelRepo channelRepo, BillingService billingService) {
          this.channelRepo = channelRepo;
          this.billingService = billingService;
      }
  }
  ```
- DONT: field `@Autowired`, setter injection, `ApplicationContext.getBean` lookups.

### Rationale
Final fields, easy to mock, no hidden container lifecycle.

---

## 2. Controller Layer (`com.aibrg.web`)

### Rule
Controllers are thin. They accept DTOs, validate, delegate to a single service method, and map the result to a response DTO.

### DO
```java
@RestController
@RequestMapping("/api/api-keys")
public class ApiKeyController {
    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @PostMapping
    public ResponseEntity<CreateApiKeyResponse> create(
            @Valid @RequestBody CreateApiKeyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ApiKeyResult result = apiKeyService.issue(principal.userId(), request.name());
        return ResponseEntity.ok(CreateApiKeyResponse.from(result));
    }
}
```

### DONT
- Return JPA entities directly (leaks internal schema, lazy-loading pitfalls).
- Put try/catch for control flow — use `@RestControllerAdvice`.
- Inline business branches like `if (user.isAdmin()) {...} else {...}` — belongs in service.
- Use `@RequestMapping` without HTTP verb.

---

## 3. Service Layer (`com.aibrg.service`)

### Rule
Services own the business transaction boundary and orchestrate repos + external calls.

### DO
```java
@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final RoutingService routingService;
    private final BillingService billingService;
    private final CrossRegionAdapterClient adapterClient;

    public ChatService(RoutingService routingService,
                       BillingService billingService,
                       CrossRegionAdapterClient adapterClient) {
        this.routingService = routingService;
        this.billingService = billingService;
        this.adapterClient = adapterClient;
    }

    @Transactional(rollbackFor = Exception.class)
    public BillingSession preAuthorize(ChatRequest request) {
        RouteTarget target = routingService.resolve(request);
        return billingService.preAuthorize(request.accountId(), target);
    }
}
```

### Transactions
- `@Transactional` lives on the service method, not the controller.
- Always `rollbackFor = Exception.class`.
- Keep transactions short — no outbound HTTP inside.
- Do not nest `@Transactional` calls via `this.xxx()` (self-invocation bypasses the proxy).

### Logging
- One logger per class, `private static final Logger log = LoggerFactory.getLogger(Xxx.class);`.
- Every public method entry in `service/` logs at least one structured line with `traceId` via MDC.

---

## 4. Repository Layer (`com.aibrg.repo`)

### Rule
Spring Data interfaces only. No custom implementations without design approval.

### DO
```java
public interface ApiKeyRepo extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyHashAndActiveTrue(String keyHash);

    @Query("select k from ApiKey k where k.accountId = :accountId and k.active = true")
    List<ApiKey> findActiveByAccount(@Param("accountId") Long accountId);
}
```

### DONT
- `SELECT *` in native queries.
- Unbounded `findAll()` on large tables — always paginate with `Pageable`, cap at 200.
- Business logic inside the repo (e.g. calling another service).

---

## 5. Entity Layer (`com.aibrg.domain`)

### Rule
JPA entities are state carriers. No business logic beyond trivial getters.

### DO
```java
@Entity
@Table(name = "api_key")
public class ApiKey {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "key_hash", length = 128, nullable = false, unique = true)
    private String keyHash;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // getters + setters only
}
```

### DONT
- Business methods like `apiKey.rotate()` (put it on a service).
- Bidirectional `@OneToMany` without a reason (fetch surprises).
- `FetchType.EAGER` on collections.
- `@Data` from Lombok (Lombok is not on the classpath; check `pom.xml` before using).

---

## 6. Configuration (`com.aibrg.config`)

### Rule
Externalized config through `@ConfigurationProperties`, one class per cohesive block.

### DO
```java
@ConfigurationProperties(prefix = "aibrg.cross-region")
public class CrossRegionProperties {
    private int poolSize = 4;
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration requestTimeout = Duration.ofSeconds(30);
    // getters + setters
}
```

Register with `@EnableConfigurationProperties(CrossRegionProperties.class)` on a `@Configuration` class.

### DONT
- Scatter `@Value("${aibrg.cross-region.pool-size}")` across services.
- Hard-code values in Java — put defaults in `application.yml`.

---

## 7. DTO & Validation

- Requests / responses live in `web/` as Java records or small POJOs.
- `jakarta.validation.constraints.*` on request DTOs.
- Controllers add `@Valid`. Global handler maps `MethodArgumentNotValidException` to `400`.

```java
public record CreateApiKeyRequest(
        @NotBlank @Size(max = 128) String name) { }
```

---

## 8. Global Exception Handling

### Rule
Exactly one `@RestControllerAdvice` class in `web/`. Map framework exceptions and custom domain exceptions to a canonical error DTO.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> onValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest().body(ApiError.of("invalid_request", firstMessage(ex)));
    }

    @ExceptionHandler(RouteNotFoundException.class)
    public ResponseEntity<ApiError> onNoRoute(RouteNotFoundException ex) {
        return ResponseEntity.status(503).body(ApiError.of("no_route", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> onUnknown(Exception ex) {
        log.error("unhandled", ex);
        return ResponseEntity.status(500).body(ApiError.of("internal_error", "unexpected"));
    }
}
```

---

## 9. HTTP Clients

- Reuse `CrossRegionAdapterClient` / the shared `HttpClient` pool. Do NOT `new HttpClient()` inside a service method.
- Every outbound call: explicit connect timeout + explicit request timeout, both sourced from `CrossRegionProperties` or an equivalent properties class.
- Retry / circuit-breaker logic is a design decision — do not add ad-hoc retries in a service method.

---

## 10. Flyway Migrations

- Path: `src/main/resources/db/migration`
- Naming: `V{next}__snake_case.sql` — pick `next` as `max(existing) + 1`.
- Never edit an applied migration. Never reorder.
- For data backfills that are non-idempotent, gate with `INSERT ... ON DUPLICATE KEY UPDATE` or `INSERT ... SELECT ... WHERE NOT EXISTS`.

---

## 11. Observability

- SLF4J + logback (Spring Boot default). No `System.out`.
- MDC: put `traceId`, `accountId`, `channelId` in MDC at request boundary; clear in `finally`.
- springdoc-openapi annotations (`@Operation`, `@ApiResponse`) on every public controller method.

---

## 12. What NOT to Introduce Without Discussion

- Lombok (not on classpath)
- JUnit (we use TestNG)
- Reactive stack (WebFlux) — project is MVC
- Kotlin
- Custom `ApplicationContext` manipulation
- `@Async` on billing/routing paths
