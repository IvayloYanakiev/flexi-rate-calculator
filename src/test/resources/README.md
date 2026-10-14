# Integration test JSON fixtures

`validation/events` and `validation/users` contain complete requests that change one field at a time. Named tests in BillingApiIntegrationTest read these resources and compare the complete expected response and invalid-event header.

The shared response.json in each validation folder applies to its cases. Invalid timezone has its own specific error response. JSON trees are used only by tests for reading response headers; production controllers still receive DTOs.

ExamplesIntegrationTest has one named real-HTTP test per scenario in the repository examples folder, plus the original request.json/response.json pair. Maven copies the example JSON files to the test classpath under examples; there is one source of truth, with no duplicate request/response copies to maintain. The original example header expectation is provided here because the original example predates the per-scenario header files.

The omitted-cycle example uses a test-only fixed clock derived from its saved expected month, so the documented request remains repeatable. Test configuration is excluded from normal application component scanning.

Run all tests with mvn clean verify.
