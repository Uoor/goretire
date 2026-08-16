# Alibaba Java Development Manual — Enforced Subset

Lookup table. AI reads this when triggered by `skills/java-alibaba-standard` or when self-auditing before commit. Each entry is pinned to the 强制 (mandatory) level of the Alibaba Java Development Manual (泰山版/嵩山版/华山版) and tailored to this repository's Java 17 + Spring Boot 3.3.5 + JPA stack.

Format: `# ID — Rule one-liner`, then `DO` and `DONT` snippets.

---

## 1. Naming

### 1.1 No `_` or `$` prefix/suffix on identifiers
- DO: `userName`, `MAX_RETRY`
- DONT: `_userName`, `name_`, `$svc`

### 1.2 Class `UpperCamel`, method/variable `lowerCamel`, constants `UPPER_SNAKE`
- DO: `class RoutingService`, `void resolveChannel()`, `static final int MAX_POOL = 32`
- DONT: `class routingService`, `void Resolve_Channel()`, `static final int maxPool`

### 1.3 No pinyin, no ambiguous abbreviations
- DO: `customerCount`, `channelAbility`
- DONT: `kehuCount`, `chanAb`

### 1.4 Abstract class starts with `Abstract`, exception ends with `Exception`, test ends with `Test` / `IT`
- DO: `AbstractAdapterClient`, `RouteNotFoundException`, `RoutingServiceTest`
- DONT: `BaseAdapterClient`, `RouteNotFoundError`

### 1.5 Package names: singular `com.aibrg.service`, not `services`
- DO: `package com.aibrg.service;`
- DONT: `package com.aibrg.services;`

### 1.6 Enum members `UPPER_SNAKE`
- DO: `enum RouteKind { PRIMARY, FALLBACK }`
- DONT: `enum RouteKind { Primary, fallback }`

---

## 2. Constants

### 2.1 Do not inline magic numbers — name them
- DO: `private static final int MAX_RETRY = 3;`
- DONT: `if (retries > 3) { ... }`

### 2.2 `long` literals use uppercase `L`
- DO: `long id = 100L;`
- DONT: `long id = 100l;` (looks like `1001`)

### 2.3 Do not use magic strings for protocol keys
- DO: `public static final String HEADER_BRIDGE_TOKEN = "X-Bridge-Adapter-Token";`
- DONT: scattering `"X-Bridge-Adapter-Token"` in multiple places

---

## 3. Formatting

### 3.1 4-space indent, no tabs
### 3.2 Single blank line between methods; no double blanks
### 3.3 Lines ≤ 120 chars; wrap long method chains at the dot
### 3.4 Braces even for single-statement `if`

- DO:
  ```java
  if (channel == null) {
      return RouteTarget.empty();
  }
  ```
- DONT:
  ```java
  if (channel == null) return RouteTarget.empty();
  ```

---

## 4. OOP

### 4.1 POJO nullable fields use wrapper types
- DO: `Long accountId; Integer priority;`
- DONT: `long accountId; int priority;`

### 4.2 POJO Boolean fields do not start with `is`
- DO: `Boolean enabled;`
- DONT: `Boolean isEnabled;` (breaks JSON serialization and some frameworks)

### 4.3 Override both `equals` and `hashCode`
### 4.4 Use `BigDecimal#compareTo` for equality, never `equals`
- DO: `if (price.compareTo(zero) > 0) { ... }`
- DONT: `if (price.equals(zero)) { ... }`

### 4.5 Constant on the left of `equals` to avoid NPE
- DO: `"chat.completions".equals(request.getType())`
- DONT: `request.getType().equals("chat.completions")`

### 4.6 Do not modify method parameters
- DONT: reassign `list = new ArrayList<>(list);` then mutate

### 4.7 Final immutable fields for value objects

---

## 5. Collections

### 5.1 `foreach` + `Collection.remove` is forbidden — `ConcurrentModificationException`
- DO: `list.removeIf(x -> x.isStale());`
- DONT: `for (Item i : list) { if (i.isStale()) list.remove(i); }`

### 5.2 `Map#keySet` / `entrySet` / `values` are views — do not cache & mutate
### 5.3 `Arrays.asList` returns fixed-size; use `new ArrayList<>(Arrays.asList(...))` if you need to add
### 5.4 Prefer `List.of`, `Map.of`, `Set.of` for immutable constants
### 5.5 Declare collection interface types, not implementations
- DO: `List<Channel> active;`
- DONT: `ArrayList<Channel> active;`

### 5.6 Collection → array: use `list.toArray(new Foo[0])` (JEP/JDK-recommended form)

---

## 6. Concurrency

### 6.1 Thread pools created explicitly with named `ThreadFactory`
- DO:
  ```java
  ThreadFactory tf = r -> { Thread t = new Thread(r, "router-pool-" + counter.incrementAndGet()); t.setDaemon(true); return t; };
  ExecutorService pool = new ThreadPoolExecutor(4, 8, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(256), tf, new ThreadPoolExecutor.CallerRunsPolicy());
  ```
- DONT: `Executors.newFixedThreadPool(8)` (default `ThreadFactory`, unbounded queue)

### 6.2 `ThreadLocal.remove()` in `finally`
- DO:
  ```java
  try { ctx.set(value); ... } finally { ctx.remove(); }
  ```

### 6.3 Prefer `ConcurrentHashMap` over `Collections.synchronizedMap` for hot paths

### 6.4 Double-checked locking requires `volatile`

### 6.5 Do not catch `InterruptedException` without restoring interrupt status
- DO: `Thread.currentThread().interrupt();` then handle

### 6.6 Locks must be released in `finally`

---

## 7. Control Flow

### 7.1 `switch` must have a `default` branch
### 7.2 Avoid deep nesting; extract methods when depth > 3
### 7.3 Boolean parameters are a code smell — prefer two methods
### 7.4 Short-circuit guard clauses at the top of the method

---

## 8. Comments

### 8.1 Javadoc for public types and public methods in `service/`, `web/`
### 8.2 Javadoc uses `/** ... */`, inline uses `//`; no `/* */` blocks scattered in code
### 8.3 No commented-out code — delete it; git remembers
### 8.4 TODO must have owner and date: `// TODO(alice, 2026-05): migrate to Resilience4j`

---

## 9. Exceptions

### 9.1 Never catch-and-ignore
- DONT: `catch (Exception e) { }`

### 9.2 Never catch `Throwable` unless you are a framework-level boundary

### 9.3 Do not use exceptions for control flow
- DONT: throw `NotFoundException` to break a loop

### 9.4 Log the throwable as the last argument
- DO: `log.error("adapter call failed channel={}", channelId, e);`
- DONT: `log.error("adapter failed " + e.getMessage());`

### 9.5 `finally` must not contain `return` or `throw`

### 9.6 Prefer try-with-resources for `AutoCloseable`

---

## 10. Logging

### 10.1 Use SLF4J, not `System.out` / `System.err`

### 10.2 Placeholder syntax, not concatenation
- DO: `log.info("routed request id={} channel={}", reqId, channelId);`
- DONT: `log.info("routed " + reqId + " " + channelId);`

### 10.3 Guard expensive `debug` calls
- DO: `if (log.isDebugEnabled()) log.debug("payload={}", serialize(body));`

### 10.4 Never log secrets — API keys, bearer tokens, passwords, raw request bodies that may contain user PII

### 10.5 `service/` logs must include `traceId` (via MDC) and, where applicable, `channelId` and `accountId`

---

## 11. Security

### 11.1 Never hard-code keys, tokens, DB passwords — use `application.yml` + env vars
### 11.2 `Random` is not secure — use `SecureRandom` for tokens, salts, and IDs that must be unpredictable
### 11.3 API keys hashed at rest (see `ApiKeyService`)
### 11.4 External input must be validated (`@Valid`, size limits, whitelist)
### 11.5 SQL must be parameterized — no string concat in JPQL/native queries

---

## 12. MySQL & JPA

### 12.1 Column names `lower_snake`; table names `lower_snake`
### 12.2 Each table has `id bigint unsigned auto_increment primary key` and `created_at datetime not null`
### 12.3 `decimal(m, n)` for money with explicit scale; never `float` / `double`
### 12.4 `varchar` length must be justified; prefer power-of-two bands (32/64/128/255/512/1024)
### 12.5 Indexes named `idx_<table>_<cols>`, unique `uk_<table>_<cols>`
### 12.6 Flyway migrations are append-only — never edit an applied `V{n}__*.sql`; create `V{n+1}__*.sql`
### 12.7 No `SELECT *` in custom `@Query`; list the columns / fields explicitly
### 12.8 Pagination queries always bounded: `size <= 200`
### 12.9 `@Transactional(rollbackFor = Exception.class)` — the default `RuntimeException`-only rollback will miss checked exceptions

---

## How AI Uses This File

1. On "check this file against the Alibaba standard", `read_file` this document, then scan the target Java file for violations of items above.
2. Output format: a table of `{rule id, line, severity, snippet, fix}`. Severity levels: `MUST` (强制), `SHOULD` (推荐), `MAY` (参考). All items above are `MUST`.
3. Do NOT auto-fix without user approval. Suggest edits, wait for go-ahead.
