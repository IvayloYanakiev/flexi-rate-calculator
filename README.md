# flexi-rate-calculator

A REST service that calculates each user's monthly usage bill.

Send the users and their complete usage for the month. The service validates the events, applies the free allowance, chooses a price, and returns a bill for each user.

Built with **Java 21, Spring Boot 4.1.1, Maven, Spring MVC, MapStruct and Caffeine**.

## Start the application

Install Java 21 and Maven 3.6.3 or later, then run:

```sh
mvn clean verify
java -jar target/flexi-rate-calculator-1.0.0.jar
```

For development, use `mvn spring-boot:run`. The default address is `http://localhost:8080`.

## Try the examples

The [examples folder](examples/README.md) contains **16 scenarios**, each with:

- Request JSON.
- Expected response JSON and response headers.
- A cURL command you can paste into Postman Import.
- A PowerShell cURL script.
- A short explanation of the result.

Examples cover normal usage, malformed event types such as `duration_minutes: [15]`, missing fields, duplicate users and events, requests with and without a billing cycle, timezone boundaries, enterprise pricing, peak hours and empty usage.

Import the [Postman collection](examples/flexi-rate.postman_collection.json) to get all requests, saved responses and assertions together. Or copy one command from [CURL-REQUESTS.md](examples/CURL-REQUESTS.md).

To run the original paid example from PowerShell:

```powershell
curl.exe -i -X POST http://localhost:8080/api/v1/billing/calculate -H "Content-Type: application/json" --data-binary "@examples/request.json"
```

Its expected total is **$0.30**, with `X-Invalid-Event-Count: 1`.

## Request and response

Send JSON to `POST /api/v1/billing/calculate`:

```json
{
  "users": [
    {"user_id": "alice", "is_enterprise": false, "timezone": "Europe/Sofia"}
  ],
  "events": [
    {"event_id": "e1", "user_id": "alice", "timestamp": "2026-10-02T10:00:00Z", "duration_minutes": 15}
  ],
  "billing_cycle": "2026-10"
}
```

This request has only 15 minutes of usage, so it is covered by the free allowance:

```json
{
  "users": [{
    "user_id": "alice",
    "billing_cycle": "2026-10",
    "total_cost": 0.00,
    "tier_breakdown": [{
      "tier": 1,
      "hourly_rate": 1.00,
      "eligible_hours": 0.00,
      "free_hours": 0.25,
      "billable_hours": 0.00,
      "cost": 0.00
    }],
    "applied_surcharges": []
  }]
}
```

The response header is `X-Invalid-Event-Count: 0`. Every listed user receives a bill, in request order, including users with zero usage.

## How billing works

1. Each user gets their **first 10 hours free per billing month**.
2. Events are sorted by timestamp so the earliest usage gets the free allowance. Event ID breaks timestamp ties.
3. For regular users, the rate is selected using hours after the free allowance. For enterprise users, the rate is selected using all hours; the first 10 hours are still free.
4. One rate applies to **all paid hours**:

| Hours used to select the rate | Price per paid hour |
| --- | --- |
| Less than 100 | $1.00 |
| 100 to less than 500 | $0.80 |
| 500 or more | $0.50 |

5. Paid usage from **09:00 inclusive to 17:00 exclusive UTC**, every day, gets an extra **20%** charge.

Example: 10 hours 15 minutes of regular usage leaves 15 paid minutes. At $1/hour, the base charge is $0.25. If those paid minutes are in peak hours, add $0.05: **total $0.30**.

Each request is a complete monthly snapshot. The free allowance is calculated from that request, so retrying the same snapshot with an explicit cycle gives the same bill. Unused free hours expire at the end of the cycle. Usage is held in memory for the calculation; clients retain the source logs and returned bills.

Calculations use exact seconds and `BigDecimal`. Base and surcharge costs are rounded separately to cents using `HALF_UP`, then added, so the breakdown matches the total. Response hours also use two decimal places; charges are calculated from exact seconds before that display rounding. Currency is USD.

## Timezones and billing months

Every user supplies a timezone. It determines which events belong to their billing month. Peak hours are always evaluated in UTC.

For example, October 2026 in `Europe/Sofia` runs from `2026-09-30T21:00:00Z` up to, but excluding, `2026-10-31T22:00:00Z`. Both are local midnight; the UTC offset changes when daylight saving ends. Events crossing a boundary contribute only their time inside the selected month.

`billing_cycle` uses `YYYY-MM`, with years 0001 through 9998. In the current implementation it is optional: when absent or null, the service uses the current month in each user's timezone, based on one captured clock instant.

## Validation and skipped events

Users need a unique, nonblank ID, a boolean `is_enterprise`, and a valid timezone. IDs are case-sensitive. User/event IDs can contain up to 128 characters; timezone and timestamp strings up to 64. Empty user and event arrays are accepted.

A valid event has a known user, an event ID, an offset-bearing timestamp and integer `duration_minutes: 15`. It starts on a UTC quarter hour (`:00`, `:15`, `:30` or `:45`), with zero seconds and fractional seconds. Extra event fields are ignored.

The request processing works as follows:

1. **`RequestLimitFilter` checks the body size** before JSON binding. If Content-Length exceeds 16 MiB, it rejects immediately. Otherwise it reads at most the configured limit plus one byte and rejects an oversized body with HTTP 413. Accepted JSON is then bound to DTOs; user/event count checks run afterward in the service.
2. **Jackson binds the request to DTOs.** `EventBindingModule` registers a separate `RecoveringEventDeserializer` that wraps normal event binding. If an individual event has an incompatible type, such as `[15]` instead of an integer, it produces a `null` list entry so processing can continue.
3. **`ValidationService` checks each event's fields and values.** Null entries, missing fields, bad dates, wrong durations and starts such as `12:07` are skipped.
4. **`BillingService` checks user references and duplicate event IDs.** It counts every skipped event once.
5. **The controller returns that count in `X-Invalid-Event-Count`.** A successful batch can contain some or even all invalid events.

The first valid event ID for a known user wins across the request. Later duplicates are skipped. Valid events outside the month are ignored for billing and still reserve their IDs. Different IDs add usage even when their times overlap.

| Status | Meaning |
| --- | --- |
| 200 | Calculation completed; invalid-event count is in the response header |
| 400 | Invalid request structure, broken JSON, invalid user, duplicate user ID or invalid cycle |
| 413 | Request body or batch exceeds a configured limit |
| 405 / 415 | Unsupported HTTP method or content type |

Broken JSON, duplicate JSON keys and parser-limit violations reject the whole request. Request errors use central `@RestControllerAdvice`; the body-size filter returns its errors before MVC. Rejected requests have no invalid-event-count header.

## Code structure

| Folder | Purpose |
| --- | --- |
| `controllers` | Accept request DTOs, call the service and set response headers |
| `service` / `service.impl` | Interfaces and implementations for validation, billing time, usage calculation and pricing |
| `domain` | Calculation objects, including standalone `Bill`, `Tier`, `Surcharge` and `UsageSummary` records |
| `dto` | Request and response objects |
| `mapper` | MapStruct mappings; the service returns the response DTO |
| `util` | Date conversions and decimal arithmetic |
| `repository` | Access to cached billing-month boundaries |
| `config` | JSON binding, request-size filter, clock and application settings |
| `exceptions` | Billing exceptions and central error handling |

`BillingServiceImpl` coordinates the request. `ValidationServiceImpl` validates inputs. `UserBillingCalculatorImpl` summarizes usage through `BillingTimeServiceImpl`. `NonProgressivePricingPolicy` calculates the bill. Comments explain the calculation steps and examples.

For `n` events and `u` users, processing takes **O(n log n + u)** time because events are sorted per user, and **O(n + u)** working space. Each request owns its counters, lists and duplicate-ID set; concurrent requests are tested for isolation.

## Caffeine cache

Caffeine remembers the UTC start and end of a month for a `(timezone, billingCycle)` pair. A repeated lookup can reuse those boundaries.

Our local benchmark measured approximately **167 ns without caching and 62 ns with caching** per repeated lookup. These are **nanoseconds per lookup**, not milliseconds per request. This is a small measured saving; whole-request performance has not been benchmarked.

The cache holds up to **1,024 entries** by default. An entry expires after **one hour without access**. Entries are immutable and local to each application instance; eviction, expiration or a process restart simply causes the next lookup to calculate the boundaries again. Timezone-rule updates should be deployed consistently across instances. Set `billing.cache-enabled: false` to calculate boundaries directly.

## Request limits and deployment

Defaults in `src/main/resources/application.yml`:

```yaml
billing:
  max-users: 1000
  max-events: 100000
  max-request-bytes: 16777216 # 16 MiB
  cache-enabled: true
  cache-maximum-size: 1024
```

The body limit bounds one raw request, not total JVM memory. DTOs and event buffers add memory, and simultaneous requests multiply that cost. Production request limits and gateway traffic controls should match the available heap.

A single application instance is the starting point for this assignment. Scaling can be added when measured traffic requires it. Request-owned calculation state already supports independent instances behind a load balancer. Shared client quotas could then be enforced at a gateway.

Caffeine suits the current local calendar cache. If a future deployment needs a cache shared across instances, Redis is an option to evaluate. A distributed cache would add network calls and failure handling, so that change should follow a measured need. Current cache misses are handled by calculating the boundaries locally.

## Tests and coverage

Each service has a dedicated JUnit 5 test class: `BillingServiceTest`, `ValidationServiceTest`, `BillingTimeServiceTest`, `UserBillingCalculatorTest` and `NonProgressivePricingPolicyTest`. Calculation tests include comments explaining the inputs, arithmetic and expected bill.

`BillingApiIntegrationTest` starts the real Spring application and checks DTO binding, validation, statuses, headers and billing. It includes actual HTTP tests through embedded Tomcat, including malformed-event recovery, snapshot retries and duplicate-user rejection. Filter tests check request-body limits.

```sh
mvn test          # 118 unit tests
mvn clean verify # also runs 17 integration tests and checks coverage
```

Latest verified result: **135 tests passed**, **95.30% line coverage**, **88.14% branch coverage**. Four services have 100% line coverage; validation has 94.44%. All services have 100% branch coverage.

JaCoCo checks at least **90% overall line coverage**, **85% overall branch coverage** and **90% line coverage per service**. All production classes are included. Open `target/site/jacoco/index.html` after `verify` for the report. [Saved coverage results](docs/coverage-results.json) record the measured run.

## Session and review records

[AI session notes](docs/ai-session.md) contain the implementation request, clarifications and work summaries. [Verification notes](docs/verification.md) record commands and results. The [code review](docs/code-review.md) records the architecture and technical reviews and later refinements.
