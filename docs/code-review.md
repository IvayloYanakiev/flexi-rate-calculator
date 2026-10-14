# Architecture and technical review

Reviewed on 2026-10-14 by two independent agents: architecture and technical correctness. The main agent evaluated their findings and implemented the changes below.

## Architecture

No blocking architectural defect was identified. The stateless complete-month snapshot design fits the agreed contract. Controllers remain thin; MapStruct mappings stay behind the service boundary; DTOs and domain objects remain separate. Request-owned buckets, counters and ID sets avoid shared billing state. The bounded cache stores immutable calendar boundaries only. Individual-event Jackson recovery remains necessary for malformed item types.

Findings and changes:

- Validation's `isValidForBatch` also reserved an event ID. Renamed it to `acceptEventForBatch` and made its reject/accept branches explicit. Reservation still occurs before month filtering.
- Dense pricing expressions obscured surcharge construction. Added a `peakSurcharges` helper, explicit surcharge-rate and total-cost variables, and readable constructor arguments.
- Renamed the time-service operation to `accumulateUsage` and named the clipped duration and paid peak seconds. It still performs month clipping, chronological grace allocation and peak accounting in that order.
- Added named snapshot-time and billing-window variables and split long orchestration calls.
- Kept the existing service interfaces and short per-user summarize-then-price flow; no extra service layer was needed.

## Technical correctness

No confirmed production correctness defect was found under the documented business rules. Inspection covered non-progressive thresholds, regular/enterprise grace, timezone month boundaries, UTC peak windows, rounding, malformed events, deduplication, request limits and concurrent request isolation.

One regression-test weakness was fixed: the seven-second month-boundary fixture displayed 0.00 hours after two-decimal rounding, so its response assertion could not detect discarded seconds. A separate test now asserts exactly seven total/free seconds before display conversion. The original response-rounding test remains.

The repeated-event fixture helper is now called `repeatedChunksAtSameTimestamp`, making its intentional additive-overlap behavior explicit. Expanded compressed domain methods and pre-sized the duplicate-user set using Java 21's `HashSet.newHashSet`.

## Verification and limits

`mvn -B clean verify`: BUILD SUCCESS, 68 tests, zero failures, errors or skipped tests (18:38 +03:00). Includes 51 service tests, 15 MVC/HTTP tests and 2 request-filter tests. No Java `var` declarations remain.

No external API contract or billing rule changed. The existing Postman collection was retained; it was not rerun for this internal refactor. This review and the test suite do not establish production load capacity or deployment performance.

## Subsequent readability change
At the user's request, acceptEventForBatch was subsequently removed. Its small unknown-user/ID reservation check is now visible directly in collectEvents; ValidationService retains event field and timestamp validation. All 68 tests pass.

Pricing was subsequently simplified at the user's request into a straight-line method with an explicit optional-surcharge branch; the small peakSurcharges helper was removed.
