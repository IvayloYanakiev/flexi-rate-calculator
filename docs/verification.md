# Verification - 2026-10-14 (Europe/Sofia)

Environment: Windows, Eclipse Temurin Java 21.0.11, Maven 3.9.16. Spring Boot 4.1.1, MapStruct 1.6.3 and JUnit Jupiter 5.14.3.

## Initial clean build

Command: `mvn -B clean verify`.

Result: **BUILD SUCCESS**, exit code 0. Finished at 10:09:55 +03:00, total Maven build time 6.973 seconds. Compiled 24 application Java files and 4 test/benchmark Java files. No compiler deprecation warnings remained after updating Spring's HTTP 413 constant/matcher names.

| Test suite | Tests | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| RequestLimitFilterTest | 2 | 0 | 0 | 0 |
| BillingApiTest | 9 | 0 | 0 | 0 |
| BillingServiceTest | 34 | 0 | 0 | 0 |
| **Total** | **45** | **0** | **0** | **0** |

Parameterized cases count as separate tests. The manual CacheBenchmark is not a correctness test and is not run by Surefire. Maven's text/XML test reports are available under `target/surefire-reports` after building.

The integration suite started an actual Spring application with embedded Tomcat on an ephemeral port and made a real HTTP request. MVC tests used the same context and explicitly registered the request-limit filter. The service isolation test ran 100 concurrent calculations with different snapshots for the same user and checked each result.

## Packaged application smoke test

Command: `java -jar target/flexi-rate-calculator-1.0.0.jar --server.port=0`.

The executable jar started successfully on ephemeral port 52753. A Python urllib HTTP POST submitted the checked-in `examples/request.json`; assertions verified:

* HTTP **200**.
* `X-Invalid-Event-Count: 1`.
* Response JSON matched `examples/response.json` in full.
* Base cost **0.25**, UTC peak surcharge **0.05**, total cost **0.30**.

The temporary server was stopped after verification. Repackaged executable jar: `target/flexi-rate-calculator-1.0.0.jar`, 22,689,993 bytes.

## Caffeine microbenchmark

Commands:

```powershell
mvn -B test dependency:build-classpath '-Dmdep.outputFile=target/test-classpath.txt'
$billingClasspath = 'target/classes;target/test-classes;' + (Get-Content target/test-classpath.txt -Raw)
java -cp $billingClasspath com.flexirate.repository.CacheBenchmark
```

Observed output: `Repeated calendar lookup: uncached 166.9 ns/op; Caffeine 61.6 ns/op; speedup 2.71x`.

Single-process local measurement using 100,000 warm-up iterations and 1,000,000 measured lookups of the same zone/month. It demonstrates a small benefit for repeated calendar conversion only; it does not establish production throughput, tail latency or heap size. The later clean build removes `target/test-classpath.txt`; the documented command recreates it.

## Issues resolved during development

* Boot 4.1.1 defaults to JUnit 6; simply overriding to JUnit 5 caused `NoSuchMethodError` in SpringExtension. Integration lifecycle now uses JUnit 5 `BeforeAll`/`AfterAll` and starts/closes the application explicitly, avoiding that incompatible extension.
* A fractional-second grace-boundary test initially expected a surcharge of 0.02. Exact arithmetic placed the charge just above 0.025, which HALF_UP correctly rounds to 0.03. The expected result was corrected.
* A MockMvc request did not supply servletPath, exposing a filter test setup gap. Admission now covers all POST requests, and both MVC and unknown-content-length body-limit tests pass.

## Verification scope

The Java service, full clean build, tests, generated MapStruct mappings and packaged executable jar were verified. The optional application Dockerfile was supplied but no image build/container execution was performed. No PostgreSQL or broker was provisioned because those infrastructures are intentionally omitted under the confirmed contract. The source submission zip includes application/test code, Maven configuration, README, examples and AI/verification records; it excludes generated Maven output.

## Response mapping refactor

After moving response mapping into `BillingService`, ran `mvn -B verify`: **BUILD SUCCESS**, exit code 0, finished at 10:23:40 +03:00, total build time 6.557 seconds. All **45 tests passed**, with 0 failures, 0 errors and 0 skipped. The executable JAR was rebuilt. Service tests now exercise returned DTOs, and the HTTP contract test verifies that invalid-event count remains a header and is absent from the JSON body. The controller has no mapper dependency. The source ZIP was refreshed to include this refactor.

## Typed-event DTO refactor

After replacing `List<JsonNode>` with automatically bound `List<EventDto>`, ran `mvn -B verify`: **BUILD SUCCESS**, exit code 0, finished at 11:51:40 +03:00, total build time 6.993 seconds. Compiled 25 application source files and 4 test/benchmark source files; rebuilt the executable JAR.

Latest suite totals: RequestLimitFilterTest **2**, BillingApiTest **11**, BillingServiceTest **33**; **46 tests, 0 failures, 0 errors, 0 skipped**. Field-type/date binding cases moved into HTTP integration coverage; service tests directly construct DTOs and validate event fields. Mixed-event HTTP coverage verifies automatic DTO binding, ignored extra event fields, skipping malformed items and continued processing of a valid item after failures. Duplicate keys inside an event remain a request error. The latest source ZIP includes these changes. The earlier standalone JAR smoke and microbenchmark results above are historical; no new Docker image verification was performed.

## Standard Jackson binding without custom deserialization

Deleted the event deserializer wrapper and its registration. Ran `mvn -B clean verify`: **BUILD SUCCESS**, exit code 0, finished at 12:10:31 +03:00, total build time 6.999 seconds. A clean build ensures deleted wrapper classes are absent from the rebuilt executable JAR.

RequestLimitFilterTest **2**, BillingApiTest **12**, BillingServiceTest **36**: **50 tests, 0 failures, 0 errors, 0 skipped**. Tests verify that integer durations 5/10/25/30 fail service validation and contribute to the invalid count; 15-minute events starting at 12:07 are accepted and correctly billed. Bad timestamp strings, null/missing values and duplicates are still skipped individually. Incompatible JSON field types now cause HTTP 400 via standard Spring binding. Documentation and source ZIP were refreshed.

## Required quarter-hour starts (latest)

Following the client's clarified chunk-format rule, added service validation requiring UTC start minutes divisible by 15 and zero seconds/nanoseconds. Ran `mvn -B verify`: **BUILD SUCCESS**, exit code 0, finished at 12:14:07 +03:00, total build time 6.707 seconds. Rebuilt the executable JAR.

RequestLimitFilterTest **2**, BillingApiTest **12**, BillingServiceTest **47**: **61 tests, 0 failures, 0 errors, 0 skipped**. Tests verify accepted :00/:15/:30/:45 starts and UTC-equivalent offset timestamps; 12:07, 12:14, nonzero seconds, fractional seconds and offsets that produce off-quarter UTC starts are skipped and counted. Mixed-batch HTTP tests verify valid records still contribute usage and invalid starts increase the response header count. Month-boundary clipping uses aligned chunks with a fixed user offset whose local boundary lies inside the chunk. This replaces the earlier arbitrary-start assumption.

## Architecture refactor

Ran `mvn -B clean verify`: **BUILD SUCCESS**, exit code 0, finished at 12:30:59 +03:00, total build time 7.080 seconds. Compiled 33 application files and 4 test/benchmark files; rebuilt the executable JAR without stale deleted classes.

RequestLimitFilterTest **2**, BillingApiTest **12**, BillingServiceTest **49**: **63 tests, 0 failures, 0 errors, 0 skipped**. Existing rules/HTTP/current-cycle/DST/concurrency behavior remain verified. New regression cases ensure early filtering of out-of-month events still reserves valid IDs, and invalid events do not reserve an ID. Inspected the generated MapStruct implementation: full list mapping is generated, timestamp/timezone conversion delegates to `BillingDates`, and the response count/body mapping remains intact. `Calculation`, the DTO-shaped `Event`, temporary individual validator classes and the old peak helper were removed. Documentation and source ZIP were refreshed.

## Malformed event recovery and extracted bucket

Restored Jackson event-level type/coercion recovery according to the client's final clarification. Extracted standalone `domain.BillingBucket` and `domain.BillingWindow`, removing the nested service bucket and repository window type.

Final command: `mvn -B clean verify`. **BUILD SUCCESS**, exit code 0, finished at 13:04:00 +03:00, total build time 6.809 seconds. Compiled 36 application files and 4 test/benchmark files and rebuilt the executable JAR.

RequestLimitFilterTest **2**, BillingApiTest **15**, BillingServiceTest **49**: **66 tests, 0 failures, 0 errors, 0 skipped**. HTTP cases include `duration_minutes: [15]` with valid event identity/timestamp, wrong scalar/object/array types, null/missing fields, integer overflow, recovery between adjacent valid events, aggregate invalid-count headers and unchanged paid bills. Request-shape/syntax errors, duplicate keys and parser limits still reject the document. Existing pricing/grace/quarter-hour/timezone/concurrency coverage remains passing. Source ZIP was refreshed and its ZIP integrity checked.

## Whole-second duration accounting

Command: `mvn -B clean verify`. **BUILD SUCCESS**, exit code 0, finished at 14:26:36 +03:00, total build time 7.853 seconds. Rebuilt the executable JAR with seconds-based counters, grace/tiers and monetary duration conversion.

RequestLimitFilterTest **2**, BillingApiTest **15**, BillingServiceTest **50**: **67 tests, 0 failures, 0 errors, 0 skipped**. Existing bills and monetary rounding remain unchanged. Added a fixed-offset month-boundary case with exactly seven seconds inside the month to verify whole-second clipping is preserved. Fractional input rejection remains covered. No nanosecond billing counters or duration conversions remain; `getNano()` only enforces zero fractional seconds, and `System.nanoTime()` belongs to the optional performance benchmark. Updated documentation and refreshed the source ZIP.

## Documented interfaces and implementations

Command: `mvn -B clean verify`. **BUILD SUCCESS**, exit code 0, finished at 14:34:37 +03:00, total build time 6.877 seconds. Compiled 41 application source files and 4 test/benchmark files; rebuilt the executable JAR from clean output after moving classes to implementation packages.

RequestLimitFilterTest **2**, BillingApiTest **15**, BillingServiceTest **50**: **67 tests, 0 failures, 0 errors, 0 skipped**. Spring integration startup verifies interface-based injection for controller, validation/time/calculation collaborators and the window provider. Existing behavioral and concurrent-isolation assertions pass. No pricing or API contract change was intended. Class/method JavaDoc and comments were added; standalone HTML JavaDoc generation was not run. Documentation and source ZIP were refreshed.

## Removed custom concurrency admission

Command: `mvn -B clean verify`. **BUILD SUCCESS**, exit code 0, finished at 16:55:16 +03:00, total build time 7.057 seconds. Rebuilt the executable JAR after removing the semaphore, its configuration/status branch and custom Tomcat tuning.

RequestLimitFilterTest **2**, BillingApiTest **15**, BillingServiceTest **50**: **67 tests, 0 failures, 0 errors, 0 skipped**. Filter tests now cover replay of a valid bounded body and rejection of an oversized body with unknown content length. Existing HTTP body/batch limits and concurrent calculation isolation still pass. Application-level concurrency rejection is intentionally absent; potential production controls are documented in the README. Source ZIP was refreshed and integrity checked.

## Descriptive variables and billing DTO/exception names

Command: `mvn -B clean verify`. **BUILD SUCCESS**, exit code 0, finished at 17:13:22 +03:00, total build time 7.140 seconds. Rebuilt from clean output after renaming API envelopes to `BillingRequestDto`/`BillingResponseDto` and the exception to `BillingException`.

RequestLimitFilterTest **2**, BillingApiTest **15**, BillingServiceTest **50**: **67 tests, 0 failures, 0 errors, 0 skipped**. Existing HTTP JSON/header/status assertions, exception tests, generated collection mappings, pricing/timezone/validation/recovery and concurrent request isolation pass. Source review found no remaining old DTO/exception names, `EventDto dto` or generic `String value` parameters in production Java. README and source ZIP were refreshed; historical session entries retain the names used at the time.

## Removed optional Docker files

Deleted `Dockerfile` and `.dockerignore` at the user's request and removed the container build/run instructions. Refreshed the source ZIP and checked its integrity and absence of those files. No application code changed, so tests were not rerun; the latest Java verification remains the 67-test clean build above. Earlier Docker references in this verification/session history describe the state at that time, not current deliverables.

## Request/response/cURL examples (latest)

Rebuilt the current service with `mvn -B clean verify`: **BUILD SUCCESS**, 67 tests, 0 failures/errors/skipped, exit 0. Finished at 17:53:59 +03:00, build time 7.492 seconds.

Launched the packaged JAR on ephemeral localhost port 55742. Generated 16 contract-based request/response/header/cURL fixtures and an importable Postman collection. The Python live verifier passed all 16 requests/64 assertions, and Postman CLI 1.74.0 independently passed all 16 requests/64 assertions plus the current-month pre-request script, exit 0. Runs used the actual service with no mocks. Wrong-type/missing/value-invalid events, duplicate users/events, explicit/omitted cycles, local month boundaries, enterprise tier selection, UTC surcharges, empty/all-invalid usage and document failures are covered.

Detailed results and CLI limitations are documented in `examples/verification/README.md`; machine-readable local HTTP results are in `examples/verification/http-results.json`. No Postman workspace was created or published, and run event reporting was disabled. Verified each cURL inline body against its request file. Stopped the temporary server and refreshed the source ZIP, with Docker files still excluded. No Java behavior changed for this request.

## Two decimal places in response hours
Ran mvn -B clean verify: BUILD SUCCESS, 67 tests, zero failures/errors/skipped (2026-10-14 18:11 +03:00). Hour fields now use scale 2, including zero and clipped fractional-hour displays. Exact-second pricing and monetary reconciliation remain unchanged.


## Explicit Java variable types
Replaced every var declaration in production, test and benchmark Java code with explicit types, including generic collection types and try-with-resources. No behavior changes. Verified no var remains under src. Maven clean verify: BUILD SUCCESS, 67 tests, zero failures/errors/skipped (2026-10-14 18:27 +03:00).

## Simplified per-user calculation flow
User requested a more understandable UserBillingCalculatorImpl. Extracted summarizeUsage so calculate explicitly summarizes usage and then applies pricing. Added a comment explaining chronological free-allowance allocation and month/peak accounting. Maven clean verify passed all 67 tests with zero failures/errors/skipped at 2026-10-14 18:32 +03:00.

## Independent architecture and technical reviews
User requested two agents plus readability improvements. Both reviewers found no confirmed production correctness defect; fixed the exact-second test coverage gap and clarified validation side effects, usage accumulation, pricing and orchestration. Details: code-review.md. Maven clean verify passed 68 tests, zero failures/errors/skipped, at 2026-10-14 18:38 +03:00.

## Line-by-line time-service explanations
Added comments to every field and executable statement in BillingTimeServiceImpl, including UTC/local cycle examples, half-open month boundaries, quarter-hour validation, month clipping, partial free allowance and peak intersections. Only comments and condition line wrapping changed. Tests were not rerun for this documentation-only edit; latest clean build passed 68 tests.

## Simplified event collection
Removed acceptEventForBatch from ValidationService. collectEvents now directly skips invalid chunks, unknown users and duplicate IDs, then groups overlapping usage. Field/time validation remains in ValidationService. Existing invalid-event and duplicate-order semantics are preserved. Maven clean verify passed all 68 tests with zero failures/errors/skipped (2026-10-14 19:02 +03:00).

## Straight-line pricing and usage documentation
Simplified NonProgressivePricingPolicy into five documented steps: select rate, base charge, peak charge, breakdown, total. Inlined the small surcharge helper and named the 20% constant explicitly. Added line-by-line summarizeUsage comments, parameters, return documentation and a chronological free-allowance example. Maven clean verify passed all 68 tests, zero failures/errors/skipped (2026-10-14 19:14 +03:00).

## Standalone tier and surcharge records
Moved Bill.Tier and Bill.Surcharge into domain/Tier.java and domain/Surcharge.java. Updated pricing and MapStruct signatures. Maven clean verify: BUILD SUCCESS, 68 tests, zero failures/errors/skipped (2026-10-14 19:16 +03:00).

## Per-service JUnit tests, integration tests and coverage gates
User requested understandable JUnit tests for every service and at least 85-90% coverage. Added ValidationServiceTest (24), BillingTimeServiceTest (27), NonProgressivePricingPolicyTest (10), UserBillingCalculatorTest (3); retained BillingServiceTest (51) and RequestLimitFilterTest (2). Added given/when/then and arithmetic comments. Renamed the API suite to BillingApiIntegrationTest and configured Failsafe separately from Surefire. Added real HTTP paid/malformed snapshot retry and duplicate-user error tests: 17 integration cases total.

JaCoCo 0.8.14 now collects both phases and enforces 90% bundle lines, 85% bundle branches and 90% lines per service implementation. No class exclusions. Initial verification exposed a test-only attempt to deserialize the response's header-only ignored primitive count; corrected the test to inspect the public JSON directly. No production code change was needed.

Final command: mvn -B clean verify. BUILD SUCCESS at 2026-10-14 19:22:50 +03:00, exit 0, 12.243 seconds. 117 unit tests + 17 integration tests = 134, zero failures/errors/skipped. Lines: 384/403 = 95.29%; branches: 104/118 = 88.14%. BillingServiceImpl, BillingTimeServiceImpl, UserBillingCalculatorImpl and NonProgressivePricingPolicy: 100% lines; ValidationServiceImpl: 94.44%. All service branches: 100%. Results saved in coverage-results.json; navigable report in target/site/jacoco/index.html. Postman collection was not rerun: this change adds JUnit verification and leaves the API contract unchanged.

## Plain-language README refresh
Rewrote README around implemented behavior: build/run, 16 request/response/header/cURL examples and Postman collection, pricing, timezone boundaries, invalid-event recovery, architecture, request limits, Caffeine, tests and coverage. Removed the speculative broker/database discussion and stale 67-test verification paragraph. Explained that Jackson event recovery creates null placeholders while RequestLimitFilter enforces body size; the batch service counts skipped items in X-Invalid-Event-Count. Corrected the proposed cache units: approximately 167 to 62 nanoseconds per calendar lookup, not milliseconds or measured whole-request latency. Described a single-instance starting point and conditional future scaling/Redis if measured requirements justify them. README accurately reflects the current optional-cycle implementation.

Checked every local README link and retained the measured 134-test/95.29% line/88.14% branch results. Documentation-only change; tests were not rerun. Source archive refreshed and integrity checked.

## Event recovery readability and pre-service size limits
Extracted RecoveringEventDeserializer into its own package-private class and used an explicit EventDto type check in module registration. Preserved the token buffer and narrow exception recovery. Added a test proving declared oversized requests are rejected before opening their input stream or reaching JSON binding. Existing unknown-length limit and malformed-event integration tests pass. Documented raw-body vs DTO/concurrent memory costs. Maven clean verify: BUILD SUCCESS, 118 unit + 17 integration = 135 tests, zero failures/errors/skipped, 95.30% line and 88.14% branch coverage, at 2026-10-14 19:31:55 +03:00.

## Second architecture, technical and requirements review
Two agents independently reviewed the design and validation pipeline; no confirmed production defect found. Re-read the assignment PDF, which explicitly makes billing_cycle optional. User validation already exists and should reject invalid user settings with HTTP 400. Added 31 single-field event cases and 21 user cases to prevent incomplete fixtures masking coercion errors. Corrected stale null/false comments. Preserved user pom formatting edits. Details: requirements-review.md. Maven clean verify passed 187 tests (118 unit + 69 integration), zero failures/errors/skipped, 95.30% lines and 88.14% branches at 2026-10-14 19:43:16 +03:00.

## JSON-resource integration tests
User requested test resources instead of CSV data and one test per example request/response. Replaced 52 parameterized validation invocations with named tests reading complete JSON requests from src/test/resources/validation, preserving every case. Expected complete JSON responses are resources too. Added ExamplesIntegrationTest with 17 named real-HTTP tests: 16 scenario folders and the original pair. Maven maps existing example JSON into test resources without duplicate fixture copies. Headers and status are verified; a test-only fixed clock uses the saved omitted-cycle month. Maven clean verify succeeded at 2026-10-14 19:55:24 +03:00, exit 0: 118 unit + 86 integration = 204 tests, zero failures/errors/skipped; 95.30% lines and 88.14% branches. Source archive refreshed.

## Test expectations and complete API assertions
User requested comments explaining each test plus validation beyond status, especially exact invalid-event counts. Added expected-outcome documentation to all 139 test methods (204 executed parameterized/named cases). Added full response resources under contracts and reusable API response assertions for JSON content type, strict full-body comparison, exact X-Invalid-Event-Count or its absence on errors. Remaining actual-HTTP cases now compare full public JSON. Error cases verify complete codes/messages; framework 405/415 cases verify supported method/media headers. Existing example tests already assert complete bodies and exact count headers. Maven clean verify passed at 2026-10-14 20:04:31 +03:00: 118 unit + 86 integration = 204, zero failures/errors/skipped. All coverage gates passed.

## All integration payloads moved to files
Removed remaining inline JSON text blocks, escaped JSON literals, JSON fragments and StringBuilder-generated requests from BillingApiIntegrationTest. All 17 remaining methods now load saved request files or reuse example files. Moved mixed-invalid batch, paid batches, individual malformed-item variants, broken documents, duplicate keys, trailing JSON, excessive depth and batch-limit requests to resources/requests. Oversized body uses a text resource. Preserved complete response comparisons and exact invalid-event header checks through assertApiFixture. Maven clean verify: BUILD SUCCESS at 2026-10-14 20:09:32 +03:00, 204 tests (118 unit + 86 integration), zero failures/errors/skipped; all coverage gates passed. Searched both integration classes to confirm request bodies are loaded from resources.
