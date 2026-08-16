# Review Checklist — Backend

Checklist for the `java-code-review` skill. AI walks every diff through these 7 categories, classifying each finding as `MUST` (block), `SHOULD` (fix before merge), or `MAY` (optional).

Each item lists: **what to check**, **how to check** (regex / file / tool), and **severity**.

---

## 1. Naming & Structure (MUST / SHOULD)

| # | Check | How | Severity |
|---|-------|-----|----------|
| 1.1 | Class / method / field naming follows `alibaba-java-standard.md §1` | Read diff, match against rules 1.1–1.6 | MUST |
| 1.2 | File placed in the correct package (`web` / `service` / `domain` / `repo` / `config`) | See `backend/AGENTS.md §Package Conventions` | MUST |
| 1.3 | No `*Util`, `*Helper`, `*Manager` junk-drawer class added | grep `-E "class\s+\w+(Util|Helper|Manager)"` on the new class | SHOULD |
| 1.4 | Public types have Javadoc | Visual inspection | SHOULD |

---

## 2. Thread Safety & Concurrency (MUST)

| # | Check | How | Severity |
|---|-------|-----|----------|
| 2.1 | No new `Executors.newFixedThreadPool` / `newCachedThreadPool` with default factory | grep `Executors\.new(Fixed|Cached|Single)` | MUST |
| 2.2 | All new thread pools have a named `ThreadFactory` and bounded queue | Read the pool construction site | MUST |
| 2.3 | `ThreadLocal.set(...)` has matching `remove()` in `finally` | grep `ThreadLocal` then scan for `remove` in surrounding `finally` | MUST |
| 2.4 | `InterruptedException` restores interrupt flag | grep `catch.*InterruptedException` then look for `Thread.currentThread().interrupt()` | MUST |
| 2.5 | No new `synchronized` on `this` in a service; prefer `ReentrantLock` or `ConcurrentHashMap` | grep `synchronized\s*\(this\)` in new diff | SHOULD |

---

## 3. Transactions (MUST)

| # | Check | How | Severity |
|---|-------|-----|----------|
| 3.1 | `@Transactional` on service methods, never controllers | grep `@Transactional` path filter by `web/` → fail | MUST |
| 3.2 | `@Transactional(rollbackFor = Exception.class)` — no bare `@Transactional` | grep `@Transactional\s*$` or `@Transactional\s*\n` without attributes | MUST |
| 3.3 | No outbound HTTP or blocking call inside a `@Transactional` method | Visual: look for `HttpClient`, `adapterClient`, `restTemplate`, `webClient` within the method body | MUST |
| 3.4 | No self-invocation bypass (`this.otherTransactionalMethod()`) | grep within same class for calls to `@Transactional` methods | SHOULD |

---

## 4. External Calls / Availability (MUST)

Cross-check against `backend/AGENTS.md §High Availability Red Lines`.

| # | Check | How | Severity |
|---|-------|-----|----------|
| 4.1 | Every new outbound HTTP request has explicit connect + request timeouts | grep `HttpClient` / `HttpRequest.newBuilder` and verify `.timeout(` and `.connectTimeout(` | MUST |
| 4.2 | Uses shared client pool, not `new HttpClient()` in a method | grep `new\s+HttpClient` or `HttpClient\.newHttpClient\(\)` in service classes | MUST |
| 4.3 | Retries / fallbacks have explicit bounds and are documented | Visual — look for unbounded `while` loops around calls | MUST |
| 4.4 | TLS verification is not disabled, `sslContext` not overridden with "trust all" | grep `TrustManager`, `trustAll`, `HostnameVerifier\s*=` | MUST |
| 4.5 | Errors from the adapter are mapped to a deterministic HTTP status | Visual inspection of exception → response mapping | SHOULD |

---

## 5. Logging & Secrets (MUST)

| # | Check | How | Severity |
|---|-------|-----|----------|
| 5.1 | No `System.out` / `System.err` / `printStackTrace()` | grep `System\.(out|err)\.`, `printStackTrace` | MUST |
| 5.2 | SLF4J placeholders, not string concat | grep `log\.(info|warn|error|debug)\([^"]*"\s*\+` or `"\s*\+\s*\w+\s*\+\s*"` in log calls | MUST |
| 5.3 | No logging of API keys / bearer tokens / passwords / raw request bodies | grep `log\.` with surrounding context containing `apiKey`, `token`, `password`, `body`, `secret` | MUST |
| 5.4 | Service-layer logs carry `traceId` (via MDC) | grep `MDC.put` at request-boundary filters, and verify logs inside include `traceId=` pattern or rely on logback pattern | SHOULD |
| 5.5 | No hard-coded URLs, IPs, tokens, DB credentials | grep `https?://`, IPv4 regex, `sk_`, `password\s*=` in Java sources | MUST |

---

## 6. SQL, JPA & Migrations (MUST)

| # | Check | How | Severity |
|---|-------|-----|----------|
| 6.1 | No `SELECT *` in `@Query` or native SQL | grep `SELECT\s+\*` in `.java` + `.sql` | MUST |
| 6.2 | New pagination has a size cap ≤ 200 | Visual inspection of `PageRequest.of` / `Pageable` | MUST |
| 6.3 | New Flyway migration is `V{n}__*.sql`, never edits applied files | `git log --oneline -- src/main/resources/db/migration` — ensure only the new file changed | MUST |
| 6.4 | Money columns are `decimal(m, n)` with explicit scale; Java side uses `BigDecimal` | Read the migration + the entity | MUST |
| 6.5 | Indexes named `idx_<table>_<cols>` / `uk_<table>_<cols>` | Read the migration | SHOULD |
| 6.6 | Enum values stored as string name, not ordinal | grep `@Enumerated` → must be `EnumType.STRING` | MUST |

---

## 7. Tests (MUST / SHOULD)

| # | Check | How | Severity |
|---|-------|-----|----------|
| 7.1 | New test class extends `ApiTestBase` when hitting HTTP | Read the class header | MUST |
| 7.2 | No JUnit imports introduced | grep `import org\.junit` | MUST |
| 7.3 | Test added to `src/test/resources/testng.xml` | Read `testng.xml` | MUST |
| 7.4 | Test data isolated via `UUID.randomUUID()` | Visual — email/org/key naming | SHOULD |
| 7.5 | Assertions use `org.testng.Assert`, not `org.junit.jupiter.api.Assertions` | grep `import org.testng.Assert` vs junit | MUST |
| 7.6 | New public service methods have at least one test | Check diff: every `+ public` method in `service/` has a matching test file | SHOULD |

---

## Output Contract

After walking the diff, the AI produces:

1. **Summary line** — e.g. `Review: 3 MUST, 2 SHOULD, 0 MAY.`
2. **Findings table** — `{Category, ID, File:Line, Severity, Description, Suggested fix}`.
3. **Verdict**:
   - Any `MUST` open → `BLOCK`.
   - Only `SHOULD` / `MAY` → `PROCEED_WITH_FOLLOWUP`.
   - Zero findings → `APPROVED`.
4. **Do not auto-apply fixes.** Propose them, wait for user approval.

---

## Fast Scan Commands

```bash
# Secrets & hard-coded endpoints
grep -RnE "sk_[A-Za-z0-9]{12,}|password\s*=\s*\"|https?://" backend/src/main/java

# Banned thread pools
grep -RnE "Executors\.new(Fixed|Cached|Single)" backend/src/main/java

# Bare @Transactional
grep -RnE "@Transactional\s*(\(\s*\))?\s*$" backend/src/main/java

# Print statements
grep -RnE "System\.(out|err)\.|printStackTrace" backend/src/main/java

# JUnit imports (should be empty)
grep -Rn "org.junit" backend/src
```
