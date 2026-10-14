# Integration test JSON fixtures

`validation/events` and `validation/users` contain complete requests that change one field at a time. Named tests in BillingApiIntegrationTest read these resources and compare the complete expected response and invalid-event header.

The shared response.json in each validation folder applies to its cases. Invalid timezone has its own specific error response. JSON trees are used only by tests for reading response headers; production controllers still receive DTOs.

ExamplesIntegrationTest has one named real-HTTP test per scenario in the repository examples folder, plus the original request.json/response.json pair. Maven copies the example JSON files to the test classpath under examples; there is one source of truth, with no duplicate request/response copies to maintain. The original example header expectation is provided here because the original example predates the per-scenario header files.

The omitted-cycle example uses a test-only fixed clock derived from its saved expected month, so the documented request remains repeatable. Test configuration is excluded from normal application component scanning.

Run all tests with mvn clean verify.

`contracts` contains complete expected responses for the additional API tests: free and paid bills, empty usage, invalid cycles and body/batch errors. API assertions compare all JSON fields, JSON content type and the exact invalid-event count, or verify that the count header is absent on rejected requests. Framework 405/415 cases assert their status, advertised supported method/media type and absence of the count header. Every test method has an expected-outcome comment.

`requests` now contains all remaining API request payloads, including mixed-invalid batches and paid snapshots. Broken JSON, duplicate-key and trailing-document fixtures are intentionally invalid JSON. The oversized-body fixture is a plain-text file containing exactly 8,193 spaces. Integration test classes contain resource paths and assertions, not inline JSON or generated JSON strings. Existing example payloads are reused directly where they match the test.
