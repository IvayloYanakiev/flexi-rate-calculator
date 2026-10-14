package com.flexirate.controllers;

import com.flexirate.FlexiRateApplication;
import com.flexirate.config.RequestLimitFilter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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

    /** Sends a saved request and verifies its status, entire saved response and exact count header. */
    private void assertApiFixture(String requestPath, int expectedStatus,
                                  String responsePath, String invalidEventCount) throws Exception {
        mvc.perform(post(URL).contentType("application/json").content(readJsonResource(requestPath)))
                .andExpect(status().is(expectedStatus))
                .andExpect(expectedJsonResponse(responsePath, invalidEventCount));
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
        HttpResponse<String> response = postJson(readJsonResource("requests/empty-snapshot.json"));
        assertHttpResponse(response, 200, "contracts/empty-response.json", "0");
    }

    /**
     * HTTP 200, the complete $0.30 alice bill, and X-Invalid-Event-Count: 1. An identical retry returns the same
     * body and count.
     */
    @Test
    void realHttpPaidSnapshotSkipsMalformedEventAndCanBeRetried() throws Exception {
        // Use the documented paid snapshot, including its one invalid event.
        String requestBody = readJsonResource("examples/request.json");
        HttpResponse<String> response = postJson(requestBody);
        assertHttpResponse(response, 200, "examples/response.json", "1");

        // Retrying the same complete snapshot must preserve the complete bill and exact count.
        HttpResponse<String> repeatedResponse = postJson(requestBody);
        assertHttpResponse(repeatedResponse, 200, "examples/response.json", "1");
        org.junit.jupiter.api.Assertions.assertEquals(response.body(), repeatedResponse.body());
    }

    /**
     * HTTP 400, code INVALID_REQUEST and message "Duplicate user_id."; no X-Invalid-Event-Count header.
     */
    @Test
    void realHttpDuplicateUsersReturnCentralErrorWithoutInvalidEventHeader() throws Exception {
        HttpResponse<String> response = postJson(readJsonResource("examples/scenarios/03-duplicate-users/request.json"));
        assertHttpResponse(response, 400, "examples/scenarios/03-duplicate-users/response.json", null);
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
        assertApiFixture("requests/valid-with-two-invalid-events.json", 200,
                "contracts/free-quarter-hour-response.json", "2");
    }

    /**
     * Each malformed user/request returns HTTP 400 with the complete generic INVALID_REQUEST error and no
     * invalid-event header.
     */
    @Test
    void missingAndInvalidUsersAreBadRequests() throws Exception {
        for (String requestPath : new String[]{"requests/missing-request-fields.json",
                "requests/null-user.json",
                "validation/users/is-enterprise-missing.json",
                "validation/users/user-id-blank.json"}) {
            assertApiFixture(requestPath, 400, "validation/users/response.json", null);
        }
    }

    /**
     * HTTP 200, X-Invalid-Event-Count: 9, one valid free quarter-hour and a complete zero-cost bill.
     */
    @Test
    void serviceSkipsInvalidValuesAndNonQuarterHourStartTimes() throws Exception {
        assertApiFixture("requests/invalid-values-and-quarter-hours.json", 200,
                "contracts/free-quarter-hour-response.json", "9");
    }

    /**
     * Each request returns HTTP 200 and X-Invalid-Event-Count: 1; both valid neighbours remain, giving 0.50 free
     * hours and total $0.00.
     */
    @Test
    void incompatibleEventTypesAreSkippedWithoutLosingAdjacentValidEvents() throws Exception {
        for (String requestPath : new String[]{"requests/invalid-item-number.json",
                "requests/invalid-item-string.json",
                "requests/invalid-item-array.json",
                "requests/invalid-item-boolean.json",
                "requests/invalid-item-null.json",
                "requests/invalid-item-missing-fields.json",
                "requests/invalid-item-duration-string.json",
                "requests/invalid-item-duration-decimal.json",
                "requests/invalid-item-duration-array.json",
                "requests/invalid-item-duration-nested-array.json",
                "requests/invalid-item-duration-overflow.json",
                "requests/invalid-item-duration-large-number.json",
                "requests/invalid-item-id-number.json",
                "requests/invalid-item-timestamp-object.json"}) {
            assertApiFixture(requestPath, 200, "validation/events/response.json", "1");
        }
    }

    /**
     * HTTP 200, the complete $0.30 bill and X-Invalid-Event-Count: 3; each bad event counts once.
     */
    @Test
    void malformedEventsDoNotChangePaidBillAndAreCountedTogether() throws Exception {
        assertApiFixture("requests/paid-with-three-invalid-events.json", 200,
                "contracts/paid-response.json", "3");
    }

    /**
     * Every wrong duration type yields HTTP 200 and count 1; user-1 retains 0.25 free hours and total $0.00.
     */
    @Test
    void clientArrayExampleAndWrongDurationTypesAreSkippedWithAllOtherFieldsValid() throws Exception {
        for (String requestPath : new String[]{"requests/client-duration-array.json",
                "requests/client-duration-string.json",
                "requests/client-duration-decimal.json",
                "requests/client-duration-object.json",
                "requests/client-duration-null.json"}) {
            assertApiFixture(requestPath, 200, "contracts/free-user-1-response.json", "1");
        }
    }

    /**
     * HTTP 400 with the complete generic INVALID_REQUEST error and no invalid-event header; the request structure
     * cannot be billed.
     */
    @Test
    void requestShapeErrorsAreNotTreatedAsInvalidEvents() throws Exception {
        for (String requestPath : new String[]{"requests/events-object.json",
                "requests/events-null.json",
                "validation/users/is-enterprise-array.json",
                "requests/truncated-event-document.json"}) {
            assertApiFixture(requestPath, 400, "validation/users/response.json", null);
        }
    }

    /**
     * Duplicate JSON keys reject the document: HTTP 400, the generic INVALID_REQUEST error and no invalid-event
     * header.
     */
    @Test
    void duplicateKeysInAnEventRemainARequestError() throws Exception {
        assertApiFixture("requests/duplicate-event-json-key.json", 400,
                "validation/users/response.json", null);
    }

    /**
     * Broken JSON returns HTTP 400 with the complete generic INVALID_REQUEST error and no invalid-event header.
     */
    @Test
    void malformedJsonIsHandledCentrally() throws Exception {
        assertApiFixture("requests/malformed-document.json", 400,
                "validation/users/response.json", null);
    }

    /**
     * Every ambiguous, coerced or over-nested document returns HTTP 400, the generic INVALID_REQUEST error and no
     * invalid-event header.
     */
    @Test
    void coercionDuplicateJsonKeysAndTrailingDocumentsAreRejected() throws Exception {
        for (String requestPath : new String[]{"validation/users/is-enterprise-string.json",
                "requests/duplicate-request-json-key.json",
                "requests/trailing-document.json",
                "requests/excessive-json-depth.json"}) {
            assertApiFixture(requestPath, 400, "validation/users/response.json", null);
        }
    }

    /**
     * HTTP 400 and code INVALID_REQUEST: timezone message "Invalid user timezone."; cycle message "billing_cycle must be YYYY-MM.". Neither has an invalid-event header.
     */
    @Test
    void invalidTimezoneAndCycleAreBadRequests() throws Exception {
        assertApiFixture("validation/users/timezone-invalid-zone.json", 400,
                "validation/users/invalid-timezone-response.json", null);
        assertApiFixture("requests/invalid-cycle.json", 400,
                "contracts/invalid-cycle-response.json", null);
    }

    /**
     * HTTP 413: BATCH_LIMIT / "User or event limit exceeded." for 51 events, BODY_LIMIT / "Request body limit
     * exceeded." for 8,193 bytes. No invalid-event header.
     */
    @Test
    void batchAndBodyLimitsReturn413() throws Exception {
        assertApiFixture("requests/too-many-events.json", 413,
                "contracts/batch-limit-response.json", null);
        assertApiFixture("requests/oversized-body.txt", 413,
                "contracts/body-limit-response.json", null);
    }

    /**
     * HTTP 200 and count 0; full bill has rate $1.00, base $0.25, peak extra $0.05 and total $0.30.
     */
    @Test
    void paidBillHasReconciledDtoBreakdown() throws Exception {
        assertApiFixture("requests/paid-valid-snapshot.json", 200,
                "contracts/paid-response.json", "0");
    }

    /**
     * Plain text returns HTTP 415 with Accept: application/json; GET returns HTTP 405 with Allow: POST. Neither
     * has an invalid-event header.
     */
    @Test
    void unsupportedMediaAndMethodHaveAppropriateStatuses() throws Exception {
        mvc.perform(post(URL).contentType("text/plain").content(readJsonResource("requests/empty-snapshot.json")))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(header().doesNotExist("X-Invalid-Event-Count"))
                .andExpect(header().string("Accept", "application/json"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(URL))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().doesNotExist("X-Invalid-Event-Count"))
                .andExpect(header().string("Allow", "POST"));
    }
}
