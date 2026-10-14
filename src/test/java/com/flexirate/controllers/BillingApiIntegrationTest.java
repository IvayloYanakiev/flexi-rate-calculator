package com.flexirate.controllers;

import com.flexirate.FlexiRateApplication;
import com.flexirate.config.RequestLimitFilter;
import com.flexirate.dto.ErrorDto;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BillingApiIntegrationTest {
    private static ConfigurableApplicationContext context;
    private static MockMvc mvc;
    private static final String URL = "/api/v1/billing/calculate";


    /**
     * Invalid event case event-id-missing: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours,
     * 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventEventIdMissingIsSkipped() throws Exception {
        assertValidationFixture("events/event-id-missing", 200);
    }

    /**
     * Invalid event case event-id-null: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours, 0.50
     * free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventEventIdNullIsSkipped() throws Exception {
        assertValidationFixture("events/event-id-null", 200);
    }

    /**
     * Invalid event case event-id-number: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours, 0.50
     * free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventEventIdNumberIsSkipped() throws Exception {
        assertValidationFixture("events/event-id-number", 200);
    }

    /**
     * Invalid event case event-id-boolean: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours,
     * 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventEventIdBooleanIsSkipped() throws Exception {
        assertValidationFixture("events/event-id-boolean", 200);
    }

    /**
     * Invalid event case event-id-array: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours, 0.50
     * free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventEventIdArrayIsSkipped() throws Exception {
        assertValidationFixture("events/event-id-array", 200);
    }

    /**
     * Invalid event case event-id-object: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours, 0.50
     * free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventEventIdObjectIsSkipped() throws Exception {
        assertValidationFixture("events/event-id-object", 200);
    }

    /**
     * Invalid event case event-id-blank: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours, 0.50
     * free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventEventIdBlankIsSkipped() throws Exception {
        assertValidationFixture("events/event-id-blank", 200);
    }

    /**
     * Invalid event case user-id-missing: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours, 0.50
     * free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventUserIdMissingIsSkipped() throws Exception {
        assertValidationFixture("events/user-id-missing", 200);
    }

    /**
     * Invalid event case user-id-null: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours, 0.50
     * free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventUserIdNullIsSkipped() throws Exception {
        assertValidationFixture("events/user-id-null", 200);
    }

    /**
     * Invalid event case user-id-number: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours, 0.50
     * free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventUserIdNumberIsSkipped() throws Exception {
        assertValidationFixture("events/user-id-number", 200);
    }

    /**
     * Invalid event case user-id-boolean: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours, 0.50
     * free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventUserIdBooleanIsSkipped() throws Exception {
        assertValidationFixture("events/user-id-boolean", 200);
    }

    /**
     * Invalid event case user-id-array: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours, 0.50
     * free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventUserIdArrayIsSkipped() throws Exception {
        assertValidationFixture("events/user-id-array", 200);
    }

    /**
     * Invalid event case user-id-object: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours, 0.50
     * free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventUserIdObjectIsSkipped() throws Exception {
        assertValidationFixture("events/user-id-object", 200);
    }

    /**
     * Invalid event case user-id-unknown-user: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours,
     * 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventUserIdUnknownUserIsSkipped() throws Exception {
        assertValidationFixture("events/user-id-unknown-user", 200);
    }

    /**
     * Invalid event case timestamp-missing: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours,
     * 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventTimestampMissingIsSkipped() throws Exception {
        assertValidationFixture("events/timestamp-missing", 200);
    }

    /**
     * Invalid event case timestamp-null: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours, 0.50
     * free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventTimestampNullIsSkipped() throws Exception {
        assertValidationFixture("events/timestamp-null", 200);
    }

    /**
     * Invalid event case timestamp-number: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours,
     * 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventTimestampNumberIsSkipped() throws Exception {
        assertValidationFixture("events/timestamp-number", 200);
    }

    /**
     * Invalid event case timestamp-boolean: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours,
     * 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventTimestampBooleanIsSkipped() throws Exception {
        assertValidationFixture("events/timestamp-boolean", 200);
    }

    /**
     * Invalid event case timestamp-array: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours, 0.50
     * free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventTimestampArrayIsSkipped() throws Exception {
        assertValidationFixture("events/timestamp-array", 200);
    }

    /**
     * Invalid event case timestamp-object: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid neighbours,
     * 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventTimestampObjectIsSkipped() throws Exception {
        assertValidationFixture("events/timestamp-object", 200);
    }

    /**
     * Invalid event case timestamp-invalid-date: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid
     * neighbours, 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventTimestampInvalidDateIsSkipped() throws Exception {
        assertValidationFixture("events/timestamp-invalid-date", 200);
    }

    /**
     * Invalid event case timestamp-off-quarter-hour: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid
     * neighbours, 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventTimestampOffQuarterHourIsSkipped() throws Exception {
        assertValidationFixture("events/timestamp-off-quarter-hour", 200);
    }

    /**
     * Invalid event case duration-minutes-missing: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid
     * neighbours, 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventDurationMinutesMissingIsSkipped() throws Exception {
        assertValidationFixture("events/duration-minutes-missing", 200);
    }

    /**
     * Invalid event case duration-minutes-null: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid
     * neighbours, 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventDurationMinutesNullIsSkipped() throws Exception {
        assertValidationFixture("events/duration-minutes-null", 200);
    }

    /**
     * Invalid event case duration-minutes-array: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid
     * neighbours, 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventDurationMinutesArrayIsSkipped() throws Exception {
        assertValidationFixture("events/duration-minutes-array", 200);
    }

    /**
     * Invalid event case duration-minutes-object: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid
     * neighbours, 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventDurationMinutesObjectIsSkipped() throws Exception {
        assertValidationFixture("events/duration-minutes-object", 200);
    }

    /**
     * Invalid event case duration-minutes-string: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid
     * neighbours, 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventDurationMinutesStringIsSkipped() throws Exception {
        assertValidationFixture("events/duration-minutes-string", 200);
    }

    /**
     * Invalid event case duration-minutes-decimal: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid
     * neighbours, 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventDurationMinutesDecimalIsSkipped() throws Exception {
        assertValidationFixture("events/duration-minutes-decimal", 200);
    }

    /**
     * Invalid event case duration-minutes-boolean: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid
     * neighbours, 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventDurationMinutesBooleanIsSkipped() throws Exception {
        assertValidationFixture("events/duration-minutes-boolean", 200);
    }

    /**
     * Invalid event case duration-minutes-wrong-duration: HTTP 200, X-Invalid-Event-Count: 1; preserve both valid
     * neighbours, 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventDurationMinutesWrongDurationIsSkipped() throws Exception {
        assertValidationFixture("events/duration-minutes-wrong-duration", 200);
    }

    /**
     * Invalid event case duration-minutes-integer-overflow: HTTP 200, X-Invalid-Event-Count: 1; preserve both
     * valid neighbours, 0.50 free hours and total $0.00. Compare the complete response.
     */
    @Test
    void eventDurationMinutesIntegerOverflowIsSkipped() throws Exception {
        assertValidationFixture("events/duration-minutes-integer-overflow", 200);
    }

    /**
     * Invalid user case user-id-missing: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users with
     * IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userUserIdMissingRejectsRequest() throws Exception {
        assertValidationFixture("users/user-id-missing", 400);
    }

    /**
     * Invalid user case user-id-null: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users with
     * IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userUserIdNullRejectsRequest() throws Exception {
        assertValidationFixture("users/user-id-null", 400);
    }

    /**
     * Invalid user case user-id-blank: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users with
     * IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userUserIdBlankRejectsRequest() throws Exception {
        assertValidationFixture("users/user-id-blank", 400);
    }

    /**
     * Invalid user case user-id-number: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users with
     * IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userUserIdNumberRejectsRequest() throws Exception {
        assertValidationFixture("users/user-id-number", 400);
    }

    /**
     * Invalid user case user-id-boolean: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users with
     * IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userUserIdBooleanRejectsRequest() throws Exception {
        assertValidationFixture("users/user-id-boolean", 400);
    }

    /**
     * Invalid user case user-id-array: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users with
     * IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userUserIdArrayRejectsRequest() throws Exception {
        assertValidationFixture("users/user-id-array", 400);
    }

    /**
     * Invalid user case user-id-object: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users with
     * IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userUserIdObjectRejectsRequest() throws Exception {
        assertValidationFixture("users/user-id-object", 400);
    }

    /**
     * Invalid user case is-enterprise-missing: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users
     * with IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userIsEnterpriseMissingRejectsRequest() throws Exception {
        assertValidationFixture("users/is-enterprise-missing", 400);
    }

    /**
     * Invalid user case is-enterprise-null: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users
     * with IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userIsEnterpriseNullRejectsRequest() throws Exception {
        assertValidationFixture("users/is-enterprise-null", 400);
    }

    /**
     * Invalid user case is-enterprise-string: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users
     * with IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userIsEnterpriseStringRejectsRequest() throws Exception {
        assertValidationFixture("users/is-enterprise-string", 400);
    }

    /**
     * Invalid user case is-enterprise-number: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users
     * with IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userIsEnterpriseNumberRejectsRequest() throws Exception {
        assertValidationFixture("users/is-enterprise-number", 400);
    }

    /**
     * Invalid user case is-enterprise-array: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users
     * with IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userIsEnterpriseArrayRejectsRequest() throws Exception {
        assertValidationFixture("users/is-enterprise-array", 400);
    }

    /**
     * Invalid user case is-enterprise-object: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users
     * with IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userIsEnterpriseObjectRejectsRequest() throws Exception {
        assertValidationFixture("users/is-enterprise-object", 400);
    }

    /**
     * Invalid user case timezone-missing: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users with
     * IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userTimezoneMissingRejectsRequest() throws Exception {
        assertValidationFixture("users/timezone-missing", 400);
    }

    /**
     * Invalid user case timezone-null: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users with
     * IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userTimezoneNullRejectsRequest() throws Exception {
        assertValidationFixture("users/timezone-null", 400);
    }

    /**
     * Invalid user case timezone-blank: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users with
     * IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userTimezoneBlankRejectsRequest() throws Exception {
        assertValidationFixture("users/timezone-blank", 400);
    }

    /**
     * Invalid user case timezone-number: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users with
     * IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userTimezoneNumberRejectsRequest() throws Exception {
        assertValidationFixture("users/timezone-number", 400);
    }

    /**
     * Invalid user case timezone-boolean: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users with
     * IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userTimezoneBooleanRejectsRequest() throws Exception {
        assertValidationFixture("users/timezone-boolean", 400);
    }

    /**
     * Invalid user case timezone-array: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users with
     * IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userTimezoneArrayRejectsRequest() throws Exception {
        assertValidationFixture("users/timezone-array", 400);
    }

    /**
     * Invalid user case timezone-object: HTTP 400, code INVALID_REQUEST, message "Provide valid JSON, users with
     * IDs/enterprise flags/timezones, and an events array.". No X-Invalid-Event-Count header.
     */
    @Test
    void userTimezoneObjectRejectsRequest() throws Exception {
        assertValidationFixture("users/timezone-object", 400);
    }

    /**
     * Invalid user case timezone-invalid-zone: HTTP 400, code INVALID_REQUEST, message "Invalid user timezone.".
     * No X-Invalid-Event-Count header.
     */
    @Test
    void userTimezoneInvalidZoneRejectsRequest() throws Exception {
        assertValidationFixture("users/timezone-invalid-zone", 400);
    }

    /**
     * Reads complete request/response fixtures; invalid events retain their two valid neighbours.
     */
    private void assertValidationFixture(String fixture, int expectedStatus) throws Exception {
        String requestJson = readJsonResource("validation/" + fixture + ".json");
        String responsePath = expectedStatus == 200 ? "validation/events/response.json" : "validation/users/response.json";
        if (fixture.equals("users/timezone-invalid-zone")) {
            responsePath = "validation/users/invalid-timezone-response.json";
        }
        org.springframework.test.web.servlet.ResultActions result = mvc.perform(
                post(URL).contentType("application/json").content(requestJson));
        result.andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().json(readJsonResource(responsePath), org.springframework.test.json.JsonCompareMode.STRICT));
        if (expectedStatus == 200) {
            result.andExpect(header().string("X-Invalid-Event-Count", "1"));
        } else {
            result.andExpect(header().doesNotExist("X-Invalid-Event-Count"));
        }
    }

    /**
     * Checks every JSON field and the skipped-event header, in addition to each test's status assertion.
     */
    private org.springframework.test.web.servlet.ResultMatcher expectedJsonResponse(String responsePath, String invalidEventCount) {
        return result -> {
            content().contentTypeCompatibleWith("application/json").match(result);
            content().json(readJsonResource(responsePath), org.springframework.test.json.JsonCompareMode.STRICT).match(result);
            if (invalidEventCount == null) {
                header().doesNotExist("X-Invalid-Event-Count").match(result);
            } else {
                header().string("X-Invalid-Event-Count", invalidEventCount).match(result);
            }
        };
    }

    private void assertHttpResponse(HttpResponse<String> response, int expectedStatus,
                                    String responsePath, String invalidEventCount) throws Exception {
        org.junit.jupiter.api.Assertions.assertEquals(expectedStatus, response.statusCode());
        org.junit.jupiter.api.Assertions.assertTrue(response.headers().firstValue("Content-Type").orElseThrow()
                .startsWith("application/json"));
        org.skyscreamer.jsonassert.JSONAssert.assertEquals(readJsonResource(responsePath), response.body(), true);
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Optional.ofNullable(invalidEventCount),
                response.headers().firstValue("X-Invalid-Event-Count"));
    }

    private String readJsonResource(String path) throws java.io.IOException {
        return new org.springframework.core.io.ClassPathResource(path)
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    @BeforeAll
    static void start() {
        context = new SpringApplicationBuilder(FlexiRateApplication.class).run("--server.port=0",
                "--billing.max-events=50", "--billing.max-users=2", "--billing.max-request-bytes=8192");
        mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context)
                .addFilters(context.getBean(RequestLimitFilter.class)).build();
    }

    @AfterAll
    static void stop() {
        if (context != null) {
            context.close();
        }
    }

    /**
     * An empty snapshot returns HTTP 200, exactly {"users":[]}, JSON content type and X-Invalid-Event-Count: 0.
     */
    @Test
    void realHttpServerAcceptsSnapshot() throws Exception {
        int port = ((org.springframework.boot.web.server.context.WebServerApplicationContext) context).getWebServer().getPort();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + URL))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"users\":[],\"events\":[]}"))
                .build();
        try (java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient()) {
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertHttpResponse(response, 200, "contracts/empty-response.json", "0");
            org.junit.jupiter.api.Assertions.assertEquals("0", response.headers().firstValue("X-Invalid-Event-Count").orElseThrow());
        }
    }

    /**
     * HTTP 200, the complete $0.30 alice bill, and X-Invalid-Event-Count: 1. An identical retry returns the same
     * body and count.
     */
    @Test
    void realHttpPaidSnapshotSkipsMalformedEventAndCanBeRetried() throws Exception {
        // Given: 41 distinct 15-minute events at peak time = 10h15m, plus one malformed event.
        StringBuilder events = new StringBuilder("{\"duration_minutes\":[15]}");
        for (int eventIndex = 0; eventIndex < 41; eventIndex++) {
            events.append(",{\"event_id\":\"").append(eventIndex)
                    .append("\",\"user_id\":\"alice\",\"timestamp\":\"2026-10-02T12:00:00Z\",\"duration_minutes\":15}");
        }
        String requestBody = "{\"users\":[{\"user_id\":\"alice\",\"is_enterprise\":false,\"timezone\":\"UTC\"}],"
                + "\"billing_cycle\":\"2026-10\",\"events\":[" + events + "]}";

        // When: send real HTTP through Tomcat, the size filter, Jackson, validation and pricing.
        HttpResponse<String> response = postJson(requestBody);
        BigDecimal totalCost = context.getBean(ObjectMapper.class).readTree(response.body())
                .path("users").get(0).path("total_cost").decimalValue();

        // Then: 10 hours free, $0.25 base and $0.05 peak extra; the malformed event is counted.
        assertHttpResponse(response, 200, "examples/response.json", "1");
        org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode());
        org.junit.jupiter.api.Assertions.assertEquals("1", response.headers().firstValue("X-Invalid-Event-Count").orElseThrow());
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("0.30").compareTo(totalCost));

        // A complete snapshot retry must produce the same body and header, without accumulating usage.
        HttpResponse<String> repeatedResponse = postJson(requestBody);
        assertHttpResponse(repeatedResponse, 200, "examples/response.json", "1");
        org.junit.jupiter.api.Assertions.assertEquals(response.body(), repeatedResponse.body());
        org.junit.jupiter.api.Assertions.assertEquals(response.headers().firstValue("X-Invalid-Event-Count"),
                repeatedResponse.headers().firstValue("X-Invalid-Event-Count"));
    }

    /**
     * HTTP 400, code INVALID_REQUEST and message "Duplicate user_id."; no X-Invalid-Event-Count header.
     */
    @Test
    void realHttpDuplicateUsersReturnCentralErrorWithoutInvalidEventHeader() throws Exception {
        // Given: duplicate user identity makes the whole snapshot ambiguous.
        String requestBody = """
                {"users":[{"user_id":"alice","is_enterprise":false,"timezone":"UTC"},
                          {"user_id":"alice","is_enterprise":true,"timezone":"Europe/Sofia"}],
                 "events":[],"billing_cycle":"2026-10"}
                """;

        HttpResponse<String> response = postJson(requestBody);
        ErrorDto errorDto = context.getBean(ObjectMapper.class).readValue(response.body(), ErrorDto.class);

        // Then: advice produces the same error contract over the actual HTTP connection.
        assertHttpResponse(response, 400, "examples/scenarios/03-duplicate-users/response.json", null);
        org.junit.jupiter.api.Assertions.assertEquals(new ErrorDto("INVALID_REQUEST", "Duplicate user_id."), errorDto);
        org.junit.jupiter.api.Assertions.assertTrue(response.headers().firstValue("X-Invalid-Event-Count").isEmpty());
    }

    private HttpResponse<String> postJson(String requestBody) throws Exception {
        int port = ((org.springframework.boot.web.server.context.WebServerApplicationContext) context).getWebServer().getPort();
        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create("http://localhost:" + port + URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        }
    }

    /**
     * HTTP 200, one free quarter-hour and total $0.00; X-Invalid-Event-Count: 2. The count appears only in the
     * header.
     */
    @Test
    void validRequestReturnsContractAndInvalidCount() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content("""
                        {"users":[{"user_id":"u","is_enterprise":false,"timezone":"UTC"}],
                         "events":[null,{}, {"event_id":"a","user_id":"u","timestamp":"2026-10-02T00:00:00Z","duration_minutes":15}],
                         "billing_cycle":"2026-10"}
                        """))
                .andExpect(status().isOk())
                .andExpect(expectedJsonResponse("contracts/free-quarter-hour-response.json", "2")).andExpect(header().string("X-Invalid-Event-Count", "2"))
                .andExpect(jsonPath("$.invalidEventCount").doesNotExist())
                .andExpect(jsonPath("$.invalid_event_count").doesNotExist())
                .andExpect(jsonPath("$.users[0].user_id").value("u"))
                .andExpect(jsonPath("$.users[0].total_cost").value(0))
                .andExpect(jsonPath("$.users[0].tier_breakdown[0].free_hours").value(0.25))
                .andExpect(jsonPath("$.users[0].applied_surcharges").isEmpty());
    }

    /**
     * Each malformed user/request returns HTTP 400 with the complete generic INVALID_REQUEST error and no
     * invalid-event header.
     */
    @Test
    void missingAndInvalidUsersAreBadRequests() throws Exception {
        for (String body : new String[]{"{}", "{\"users\":[null],\"events\":[]}",
                "{\"users\":[{\"user_id\":\"u\",\"timezone\":\"UTC\"}],\"events\":[]}",
                "{\"users\":[{\"user_id\":\" \",\"is_enterprise\":false,\"timezone\":\"UTC\"}],\"events\":[]}"}) {
            mvc.perform(post(URL).contentType("application/json").content(body)).andExpect(status().isBadRequest())
                    .andExpect(expectedJsonResponse("validation/users/response.json", null))
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
    }

    /**
     * HTTP 200, X-Invalid-Event-Count: 9, one valid free quarter-hour and a complete zero-cost bill.
     */
    @Test
    void serviceSkipsInvalidValuesAndNonQuarterHourStartTimes() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content("""
                        {"users":[{"user_id":"u","is_enterprise":false,"timezone":"UTC"}],
                         "billing_cycle":"2026-10",
                         "events":[
                           null, {},
                           {"event_id":"five","user_id":"u","timestamp":"2026-10-02T00:00:00Z","duration_minutes":5},
                           {"event_id":"ten","user_id":"u","timestamp":"2026-10-02T00:00:00Z","duration_minutes":10},
                           {"event_id":"twenty-five","user_id":"u","timestamp":"2026-10-02T00:00:00Z","duration_minutes":25},
                           {"event_id":"thirty","user_id":"u","timestamp":"2026-10-02T00:00:00Z","duration_minutes":30},
                           {"event_id":"bad-date","user_id":"u","timestamp":"2026-02-30T00:00:00Z","duration_minutes":15},
                           {"event_id":"no-offset","user_id":"u","timestamp":"2026-10-02T00:00:00","duration_minutes":15},
                           {"event_id":"off-quarter","user_id":"u","timestamp":"2026-10-02T12:07:00Z","duration_minutes":15},
                           {"event_id":"valid","user_id":"u","timestamp":"2026-10-02T12:15:00Z","duration_minutes":15,"extra":"ignored"}
                         ]}
                        """))
                .andExpect(status().isOk())
                .andExpect(expectedJsonResponse("contracts/free-quarter-hour-response.json", "9"))
                .andExpect(header().string("X-Invalid-Event-Count", "9"))
                .andExpect(jsonPath("$.users[0].tier_breakdown[0].free_hours").value(0.25));
    }

    /**
     * Each request returns HTTP 200 and X-Invalid-Event-Count: 1; both valid neighbours remain, giving 0.50 free
     * hours and total $0.00.
     */
    @Test
    void incompatibleEventTypesAreSkippedWithoutLosingAdjacentValidEvents() throws Exception {
        for (String item : new String[]{"12", "\"bad\"", "[]", "false", "null", "{}",
                "{\"duration_minutes\":\"15\"}", "{\"duration_minutes\":15.0}",
                "{\"duration_minutes\":[15]}", "{\"duration_minutes\":[15,[15]]}",
                "{\"duration_minutes\":2147483648}", "{\"duration_minutes\":999999999999999999999999999999}",
                "{\"event_id\":12}", "{\"timestamp\":{\"bad\":true}}"}) {
            String before = "{\"event_id\":\"before\",\"user_id\":\"u\",\"timestamp\":\"2026-10-02T00:00:00Z\",\"duration_minutes\":15}";
            String after = "{\"event_id\":\"after\",\"user_id\":\"u\",\"timestamp\":\"2026-10-02T00:15:00Z\",\"duration_minutes\":15}";
            mvc.perform(post(URL).contentType("application/json")
                            .content("{\"users\":[{\"user_id\":\"u\",\"is_enterprise\":false,\"timezone\":\"UTC\"}],"
                                    + "\"billing_cycle\":\"2026-10\",\"events\":[" + before + "," + item + "," + after + "]}"))
                    .andExpect(status().isOk())
                    .andExpect(expectedJsonResponse("validation/events/response.json", "1"))
                    .andExpect(header().string("X-Invalid-Event-Count", "1"))
                    .andExpect(jsonPath("$.users[0].tier_breakdown[0].free_hours").value(0.5));
        }
    }

    /**
     * HTTP 200, the complete $0.30 bill and X-Invalid-Event-Count: 3; each bad event counts once.
     */
    @Test
    void malformedEventsDoNotChangePaidBillAndAreCountedTogether() throws Exception {
        StringBuilder events = new StringBuilder();
        for (int i = 0; i < 41; i++) {
            if (i > 0) {
                events.append(',');
            }
            events.append("{\"event_id\":\"").append(i)
                    .append("\",\"user_id\":\"u\",\"timestamp\":\"2026-10-02T12:15:00Z\",\"duration_minutes\":15}");
        }
        String body = "{\"users\":[{\"user_id\":\"u\",\"is_enterprise\":false,\"timezone\":\"UTC\"}],"
                + "\"billing_cycle\":\"2026-10\",\"events\":[{\"duration_minutes\":[15]},"
                + events + ",{\"event_id\":\"missing-fields\"},"
                + "{\"event_id\":\"bad-value\",\"user_id\":\"u\",\"timestamp\":\"2026-10-02T12:07:00Z\",\"duration_minutes\":20}]}";
        mvc.perform(post(URL).contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(expectedJsonResponse("contracts/paid-response.json", "3")).andExpect(header().string("X-Invalid-Event-Count", "3"))
                .andExpect(jsonPath("$.users[0].total_cost").value(0.30));
    }

    /**
     * Every wrong duration type yields HTTP 200 and count 1; user-1 retains 0.25 free hours and total $0.00.
     */
    @Test
    void clientArrayExampleAndWrongDurationTypesAreSkippedWithAllOtherFieldsValid() throws Exception {
        for (String duration : new String[]{"[15]", "\"15\"", "15.0", "{\"value\":15}", "null"}) {
            String malformed = "{\"event_id\":\"e1\",\"user_id\":\"user-1\","
                    + "\"timestamp\":\"2026-10-14T10:00:00Z\",\"duration_minutes\":" + duration + "}";
            String valid = "{\"event_id\":\"e2\",\"user_id\":\"user-1\","
                    + "\"timestamp\":\"2026-10-14T10:15:00Z\",\"duration_minutes\":15}";
            mvc.perform(post(URL).contentType("application/json").content(
                            "{\"users\":[{\"user_id\":\"user-1\",\"is_enterprise\":false,\"timezone\":\"UTC\"}],"
                                    + "\"billing_cycle\":\"2026-10\",\"events\":[" + malformed + "," + valid + "]}"))
                    .andExpect(status().isOk())
                    .andExpect(expectedJsonResponse("contracts/free-user-1-response.json", "1")).andExpect(header().string("X-Invalid-Event-Count", "1"))
                    .andExpect(jsonPath("$.users[0].tier_breakdown[0].free_hours").value(0.25));
        }
    }

    /**
     * HTTP 400 with the complete generic INVALID_REQUEST error and no invalid-event header; the request structure
     * cannot be billed.
     */
    @Test
    void requestShapeErrorsAreNotTreatedAsInvalidEvents() throws Exception {
        for (String body : new String[]{"{\"users\":[],\"events\":{}}",
                "{\"users\":[],\"events\":null}",
                "{\"users\":[{\"user_id\":\"u\",\"is_enterprise\":[],\"timezone\":\"UTC\"}],\"events\":[]}",
                "{\"users\":[],\"events\":[{\"duration_minutes\":[15]}"}) {
            mvc.perform(post(URL).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(expectedJsonResponse("validation/users/response.json", null)).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
    }

    /**
     * Duplicate JSON keys reject the document: HTTP 400, the generic INVALID_REQUEST error and no invalid-event
     * header.
     */
    @Test
    void duplicateKeysInAnEventRemainARequestError() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content("""
                        {"users":[],"events":[{"event_id":"a","event_id":"b"}]}
                        """)).andExpect(status().isBadRequest())
                .andExpect(expectedJsonResponse("validation/users/response.json", null));
    }

    /**
     * Broken JSON returns HTTP 400 with the complete generic INVALID_REQUEST error and no invalid-event header.
     */
    @Test
    void malformedJsonIsHandledCentrally() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content("{bad"))
                .andExpect(status().isBadRequest())
                .andExpect(expectedJsonResponse("validation/users/response.json", null)).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    /**
     * Every ambiguous, coerced or over-nested document returns HTTP 400, the generic INVALID_REQUEST error and no
     * invalid-event header.
     */
    @Test
    void coercionDuplicateJsonKeysAndTrailingDocumentsAreRejected() throws Exception {
        for (String body : new String[]{
                "{\"users\":[{\"user_id\":\"u\",\"is_enterprise\":\"false\",\"timezone\":\"UTC\"}],\"events\":[]}",
                "{\"users\":[],\"users\":[],\"events\":[]}",
                "{\"users\":[],\"events\":[]} {}",
                "{\"users\":[],\"events\":[" + "[".repeat(20) + "0" + "]".repeat(20) + "]}"}) {
            mvc.perform(post(URL).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(expectedJsonResponse("validation/users/response.json", null));
        }
    }

    /**
     * HTTP 400 and code INVALID_REQUEST: timezone message "Invalid user timezone."; cycle message "billing_cycle
     * must be YYYY-MM.". Neither has an invalid-event header.
     */
    @Test
    void invalidTimezoneAndCycleAreBadRequests() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content("""
                        {"users":[{"user_id":"u","is_enterprise":false,"timezone":"No/Zone"}],"events":[]}
                        """)).andExpect(status().isBadRequest())
                .andExpect(expectedJsonResponse("validation/users/invalid-timezone-response.json", null));
        mvc.perform(post(URL).contentType("application/json").content("""
                        {"users":[],"events":[],"billing_cycle":"2026-00"}
                        """)).andExpect(status().isBadRequest())
                .andExpect(expectedJsonResponse("contracts/invalid-cycle-response.json", null));
    }

    /**
     * HTTP 413: BATCH_LIMIT / "User or event limit exceeded." for 51 events, BODY_LIMIT / "Request body limit
     * exceeded." for 8,193 bytes. No invalid-event header.
     */
    @Test
    void batchAndBodyLimitsReturn413() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content("{\"users\":[],\"events\":[" + "null,".repeat(50) + "null]}"))
                .andExpect(status().isContentTooLarge())
                .andExpect(expectedJsonResponse("contracts/batch-limit-response.json", null)).andExpect(jsonPath("$.code").value("BATCH_LIMIT"));
        mvc.perform(post(URL).contentType("application/json").content(" ".repeat(8193)))
                .andExpect(status().isContentTooLarge())
                .andExpect(expectedJsonResponse("contracts/body-limit-response.json", null)).andExpect(jsonPath("$.code").value("BODY_LIMIT"));
    }

    /**
     * HTTP 200 and count 0; full bill has rate $1.00, base $0.25, peak extra $0.05 and total $0.30.
     */
    @Test
    void paidBillHasReconciledDtoBreakdown() throws Exception {
        StringBuilder events = new StringBuilder();
        for (int i = 0; i < 41; i++) {
            if (i > 0) {
                events.append(',');
            }
            events.append("{\"event_id\":\"").append(i)
                    .append("\",\"user_id\":\"u\",\"timestamp\":\"2026-10-02T12:15:00Z\",\"duration_minutes\":15}");
        }
        String body = "{\"users\":[{\"user_id\":\"u\",\"is_enterprise\":false,\"timezone\":\"UTC\"}],\"events\":["
                + events + "],\"billing_cycle\":\"2026-10\"}";
        mvc.perform(post(URL).contentType("application/json").content(body)).andExpect(status().isOk())
                .andExpect(expectedJsonResponse("contracts/paid-response.json", "0"))
                .andExpect(header().string("X-Invalid-Event-Count", "0"))
                .andExpect(jsonPath("$.users[0].total_cost").value(0.30))
                .andExpect(jsonPath("$.users[0].tier_breakdown[0].hourly_rate").value(1.00))
                .andExpect(jsonPath("$.users[0].tier_breakdown[0].cost").value(0.25))
                .andExpect(jsonPath("$.users[0].applied_surcharges[0].cost").value(0.05));
    }

    /**
     * Plain text returns HTTP 415 with Accept: application/json; GET returns HTTP 405 with Allow: POST. Neither
     * has an invalid-event header.
     */
    @Test
    void unsupportedMediaAndMethodHaveAppropriateStatuses() throws Exception {
        mvc.perform(post(URL).contentType("text/plain").content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().doesNotExist("X-Invalid-Event-Count"))
                .andExpect(header().string("Accept", "application/json"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(URL))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().doesNotExist("X-Invalid-Event-Count"))
                .andExpect(header().string("Allow", "POST"));
    }
}
