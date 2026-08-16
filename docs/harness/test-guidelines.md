# Test Guidelines — Backend

Rules for writing TestNG integration tests in this repo. AI reads this file when triggered by `skills/java-test-author` or when asked to add a test. Complements the existing `skills/prepare-test-data` for data setup.

---

## 1. Test Framework Facts

- **Framework:** TestNG 7.10.2 (`org.testng:testng`, scope `test`). **Not** JUnit.
- **Base class:** `com.aibrg.test.base.ApiTestBase` — provides `http`, health pre-check, user/admin tokens, API key issuance.
- **Utilities:** `com.aibrg.test.util.HttpJsonClient`, `com.aibrg.test.util.TestContext`.
- **Suite config:** `src/test/resources/testng.xml`. All test classes must be registered here.
- **Run command:** `mvn -q test` (requires backend running at `API_BASE_URL`, default `http://localhost:8080`).

Do not add JUnit, AssertJ, Mockito-JUnit-Jupiter, or any other alternative test framework without explicit user approval.

---

## 2. Where Tests Go

| Purpose | Package | Naming |
|---------|---------|--------|
| End-to-end API scenario | `com.aibrg.test.scenario` | `<Feature>Scenario` |
| Shared base / fixtures | `com.aibrg.test.base` | `<Name>Base` |
| HTTP / assertion helpers | `com.aibrg.test.util` | `<Name>Client`, `<Name>Util` |

Unit tests for pure utility classes may live under `com.aibrg.test.unit` (mirror the production package path). Today the project runs only integration-style scenarios — match that pattern.

---

## 3. Canonical Test Template

```java
package com.aibrg.test.scenario;

import com.aibrg.test.base.ApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.UUID;

public class ApiKeyLifecycleScenario extends ApiTestBase {

    @Test
    public void createsApiKeyAndListsIt() {
        String userToken = ensureUserToken();
        String name = "TestNG Key " + UUID.randomUUID();

        JsonNode created = http.post(
                "/api/api-keys",
                Map.of("name", name),
                authBearer(userToken)
        ).assertStatus(200).json();

        String apiKey = created.path("apiKey").asText();
        Assert.assertTrue(apiKey.startsWith("sk_"), "plaintext API key returned once");

        JsonNode list = http.get("/api/api-keys", authBearer(userToken)).assertStatus(200).json();
        Assert.assertTrue(list.isArray(), "list response is an array");
    }
}
```

### What this template enforces

- `extends ApiTestBase` — no raw HTTP bootstrapping, no token management duplication.
- `ensureUserToken()` / `ensureAdminToken()` / `ensureApiKey()` from the base; reuse, do not re-invent.
- `http` is the pre-wired `HttpJsonClient` keyed to `BASE_URL`.
- `org.testng.Assert`, `org.testng.annotations.Test` — no JUnit imports.

---

## 4. Isolation Rules

1. **Unique inputs per run.** Emails, org names, API key names use `UUID.randomUUID()`. See `ApiTestBase#ensureUserToken`.
2. **No shared mutable state across tests.** Use TestNG `@DataProvider` or method-local vars.
3. **Do NOT rely on record ordering unless you explicitly sort.**
4. **Never delete rows in `@BeforeSuite` to "clean up".** Tests are responsible for their own data.
5. **DB assertions** are secondary — prefer asserting on HTTP responses. Only hit the DB to verify side-effects not visible via API.

---

## 5. Assertions

- Use `org.testng.Assert.*`. Common helpers:
  - `assertEquals(actual, expected, message)`
  - `assertTrue(condition, message)`
  - `assertNotNull(value, message)`
- Always include a human-readable message as the last arg — it shows up in the report.
- For fluent HTTP status checks, use `.assertStatus(200)` on `HttpJsonClient` (it wraps the raw response).
- For JSON body checks, prefer `JsonNode.path(...)` over `.get(...)` to avoid NPEs.

---

## 6. Data Providers

TestNG's `@DataProvider` is the only parametrization mechanism in this repo.

```java
@DataProvider(name = "invalidEmails")
public Object[][] invalidEmails() {
    return new Object[][]{
        {"", "empty"},
        {"not-an-email", "no at sign"},
        {"a@", "no domain"}
    };
}

@Test(dataProvider = "invalidEmails")
public void rejectsInvalidEmail(String email, String reason) {
    http.post("/api/auth/register", Map.of("email", email, "password", "x", "orgName", "x"))
        .assertStatus(400);
}
```

---

## 7. Scenario Design Patterns

1. **One scenario, one behavior.** Test method names read as a sentence: `rejectsUnauthorizedRequests`, `billsPerCompletion`.
2. **Arrange / Act / Assert sections** with blank lines separating them.
3. **External stubs:** for true unit tests of a service, instantiate it directly and pass mocks. This repo does not yet ship Mockito; if you need it, propose adding `mockito-core` + `mockito-testng` in chat first.
4. **Streaming / long-running:** enforce an upper bound with `Awaitility`-style polling or TestNG's `timeOut` attribute: `@Test(timeOut = 5000)`.

---

## 8. Registering Tests in `testng.xml`

Any new class must be listed in `src/test/resources/testng.xml`. Follow the existing structure:

```xml
<suite name="ai-bridge">
    <test name="scenarios">
        <classes>
            <class name="com.aibrg.test.scenario.AuthScenario"/>
            <class name="com.aibrg.test.scenario.ApiKeyLifecycleScenario"/>
            <!-- add your class here -->
        </classes>
    </test>
</suite>
```

Do NOT use package-level `<packages>` elements unless the whole team agrees — we want explicit registration so AI-added tests cannot accidentally run in production-like environments.

---

## 9. Red Flags in Tests

Treat these as `MUST` findings in review:

- `Thread.sleep(…)` without a comment justifying the wait.
- Hard-coded IPs, ports other than `8080`, or third-party URLs.
- Catching `Exception` to "make the test pass".
- Tests that mutate `application.yml` or system properties without restoring in `@AfterMethod`.
- Tests that assume a specific DB state ("row id = 1") instead of creating their own fixtures.
- Assertions without messages.

---

## 10. Quick Checklist Before Adding a Test

- [ ] Extends `ApiTestBase` (for HTTP-level tests).
- [ ] All identifiers use `UUID.randomUUID()`.
- [ ] `org.testng.Assert`, `org.testng.annotations.Test` — no JUnit.
- [ ] Registered in `testng.xml`.
- [ ] Asserts against HTTP response first, DB second (if at all).
- [ ] `mvn -q test` passes locally (requires `mvn spring-boot:run` in another terminal).
