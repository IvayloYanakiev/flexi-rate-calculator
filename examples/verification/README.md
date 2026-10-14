# Live verification of example requests

Verified on 2026-10-14 against the real packaged Java service at `http://localhost:55742` (an ephemeral test port). The service was stopped afterward; examples default to `http://localhost:8080` for normal use.

* `mvn -B clean verify`: **BUILD SUCCESS**, 67 tests, 0 failures/errors/skipped, exit 0. Finished at 17:53:59 +03:00; Maven build duration 7.492 seconds.
* `python examples/generate_examples.py --verify-base-url http://localhost:55742`: **16 requests, 64 assertions passed**, exit 0. Checks status, JSON content type, invalid-count value/absence and complete expected body. Machine-readable evidence: [http-results.json](http-results.json).
* Postman CLI **1.74.0**: **16 requests, 64 assertions, 1 pre-request script; 0 failures**, exit 0. This independently executed the saved collection against the actual application, not a mock.
* Static checks confirmed that all 16 inline cURL bodies match their request files, all expected response/header samples parse as JSON, and the collection contains 16 requests with 64 assertions. Case 16's request is intentionally malformed JSON.

Postman command:

```sh
postman collection run examples/flexi-rate.postman_collection.json --env-var baseUrl=http://localhost:55742 --no-report-events
```

The final collection run reported 16 requests, 16 test scripts, 1 pre-request script and 64 assertions executed, all with 0 failures. Run duration was 1467 ms. These timings are observations from a local functional check, not a load-test result.

Trying the CLI's `--output` export first required Postman login and exited 1 before running requests. Removing that optional export allowed the local collection run to complete successfully without login. No workspace binding, publication or credentials were needed; cloud event reporting was disabled. No JSON export from Postman is claimed; the JSON evidence here comes from the separate local HTTP verifier.

The current-cycle example contains a pre-request script that rebuilds its timestamps for the current UTC month; the response sample is the generation-month snapshot. All other examples have fixed explicit billing cycles for repeatability.
