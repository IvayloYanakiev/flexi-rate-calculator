# Flexi-Rate request, response and cURL examples

Start the application on port 8080:

```sh
java -jar target/flexi-rate-calculator-1.0.0.jar
```

You can use either:

1. **Import the whole collection:** in Postman select **Import**, then choose [flexi-rate.postman_collection.json](flexi-rate.postman_collection.json). The collection includes 16 requests, saved expected responses, and status/header/full-body assertions. Change collection variable `baseUrl` if the service runs on another port.
2. **Paste one cURL:** open [CURL-REQUESTS.md](CURL-REQUESTS.md), copy a complete command and paste it into Postman's Import text box. Each command contains an inline JSON body, so Postman does not need to find a local payload file. Importing a cURL creates the request; importing the collection also gives you assertions and saved responses.

The `--include` option displays response headers when running cURL in a terminal. In Postman, inspect the response's **Headers** tab for `X-Invalid-Event-Count`; it is deliberately absent from the JSON body. Rejected whole requests do not have this header.

| Scenario | HTTP | Invalid count | Expected behavior |
| --- | --- | --- | --- |
| [01 Valid with cycle](scenarios/01-valid-with-billing-cycle/README.md) | 200 | 0 | 10.25h: base $0.25 + peak $0.05 = **$0.30** |
| [02 Wrong JSON type](scenarios/02-malformed-event-type/README.md) | 200 | 1 | `[15]` is skipped; a following valid event is retained |
| [03 Duplicate users](scenarios/03-duplicate-users/README.md) | 400 | absent | `Duplicate user_id.`; the whole request is rejected |
| [04 No billing cycle](scenarios/04-without-billing-cycle/README.md) | 200 | 0 | Current UTC month, 10.25h and **$0.30**; dates refreshed automatically in Postman |
| [05 Invalid values/missing fields](scenarios/05-invalid-event-values/README.md) | 200 | 5 | Wrong duration, off-quarter time, bad date, missing duration and null are skipped |
| [06 Duplicate events](scenarios/06-duplicate-events/README.md) | 200 | 1 | First valid event ID wins; duplicate usage is excluded |
| [07 Unknown user](scenarios/07-unknown-user/README.md) | 200 | 1 | An event for an unlisted user is skipped |
| [08 Empty usage](scenarios/08-empty-events/README.md) | 200 | 0 | Every listed user gets a zero bill |
| [09 Invalid timezone](scenarios/09-invalid-timezone/README.md) | 400 | absent | Invalid user timezone rejects the request |
| [10 Invalid cycle](scenarios/10-invalid-billing-cycle/README.md) | 400 | absent | `2026-13` rejects the request |
| [11 Local month boundary](scenarios/11-timezone-month-boundary/README.md) | 200 | 0 | Same UTC instant is in October for Sofia and September for UTC |
| [12 Enterprise vs regular](scenarios/12-enterprise-tier-comparison/README.md) | 200 | 0 | 100 off-peak hours each: **$90 regular / $72 enterprise** |
| [13 Peak vs off-peak](scenarios/13-peak-versus-off-peak/README.md) | 200 | 0 | Paid 16:45 UTC chunk **$0.30**; paid 17:00 UTC chunk **$0.25** |
| [14 All events invalid](scenarios/14-all-events-invalid/README.md) | 200 | 3 | Successful batch with a zero bill |
| [15 Invalid request shape](scenarios/15-invalid-request-shape/README.md) | 400 | absent | `events` is an object instead of an array |
| [16 Broken JSON document](scenarios/16-malformed-json-document/README.md) | 400 | absent | Truncated JSON cannot be processed as a batch |

Each scenario folder contains:

* `request.json`: complete request body. Case 16 is intentionally invalid JSON.
* `response.json`: expected JSON response body.
* `response-headers.json`: stable expected headers; transient Date/connection/transfer headers are omitted.
* `curl.txt`: self-contained command to copy into Postman Import or a POSIX shell.
* `curl.ps1`: file-based Windows PowerShell command; run it from the repository root, for example `./examples/scenarios/02-malformed-event-type/curl.ps1`. This avoids native-shell JSON quoting issues.
* `README.md`: what the case demonstrates and its expected HTTP status/count.

For example, the wrong-type request is:

```sh
curl --include --request POST 'http://localhost:8080/api/v1/billing/calculate' --header 'Content-Type: application/json' --data-raw '{"users":[{"user_id":"alice","is_enterprise":false,"timezone":"UTC"}],"events":[{"event_id":"bad-array","user_id":"alice","timestamp":"2026-10-02T10:00:00Z","duration_minutes":[15]},{"event_id":"good","user_id":"alice","timestamp":"2026-10-02T10:00:00Z","duration_minutes":15}],"billing_cycle":"2026-10"}'
```

Expected: HTTP **200**, `X-Invalid-Event-Count: 1`, valid usage **0.25h** and total **0.00** because the valid event is within that user's free allowance. Case 01/13 demonstrate paid amounts so this zero total is not mistaken for skipped valid usage.

## Explicit cycle vs current cycle

All explicit-cycle scenarios use **2026-10**, so they are repeatable regardless of today's date. Historical usage is allowed by this service.

Case 04 omits `billing_cycle` and uses a UTC user. Its saved JSON/cURL request dates and saved response month reflect when the examples were generated. In another month, those saved event dates would be out of the current cycle: regenerate the files or use the collection's pre-request script, which refreshes its 41 timestamps automatically. Its test also computes the current expected UTC month at execution. No fixed clock or test-only application setting is required.

Case 12 is intentionally larger: proving the 100-hour enterprise threshold requires 400 valid 15-minute chunks per user. It contains 800 chronological off-peak events. Importing the collection is easier than copying that long cURL.

The original `request.json`/`response.json` directly under `examples` remain as the earlier single-case example; the new scenarios are separate.

## Reproduce verification locally

No workspace publication is needed. With Postman CLI installed:

```sh
postman collection run examples/flexi-rate.postman_collection.json --env-var baseUrl=http://localhost:8080 --no-report-events
```

The collection has four assertions per request: status, JSON content type, complete response body, and invalid-count header value/absence. A request intentionally returning 400 is a passing test when the error matches the contract. CLI v1.74.0 ran this local collection without login; the optional `--output` report export required login, so verification used console results and local Python evidence instead. No run data was published to a workspace.

You can also regenerate the fixtures and verify them directly using Python 3:

```sh
python examples/generate_examples.py --verify-base-url http://localhost:8080
```

This compares live responses to expectations derived from the agreed rules and writes [verification/http-results.json](verification/http-results.json). Re-running it updates the current-cycle fixture to the generation month. The application implementation is not changed by this script.

Postman import instructions: [official Postman documentation](https://learning.postman.com/docs/getting-started/importing-and-exporting/importing-data/).
