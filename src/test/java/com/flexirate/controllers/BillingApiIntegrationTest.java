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

    @BeforeAll static void start() {
        context = new SpringApplicationBuilder(FlexiRateApplication.class).run("--server.port=0",
                "--billing.max-events=50", "--billing.max-users=2", "--billing.max-request-bytes=8192");
        mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context)
                .addFilters(context.getBean(RequestLimitFilter.class)).build();
    }
    @AfterAll static void stop() { if (context != null) { context.close(); } }

    @Test void realHttpServerAcceptsSnapshot() throws Exception {
        int port = ((org.springframework.boot.web.server.context.WebServerApplicationContext) context).getWebServer().getPort();
        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + URL))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"users\":[],\"events\":[]}"))
                .build();
        try (java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient()) {
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode());
            org.junit.jupiter.api.Assertions.assertEquals("0", response.headers().firstValue("X-Invalid-Event-Count").orElseThrow());
        }
    }

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
        org.junit.jupiter.api.Assertions.assertEquals(200, response.statusCode());
        org.junit.jupiter.api.Assertions.assertEquals("1", response.headers().firstValue("X-Invalid-Event-Count").orElseThrow());
        org.junit.jupiter.api.Assertions.assertEquals(0, new BigDecimal("0.30").compareTo(totalCost));

        // A complete snapshot retry must produce the same body and header, without accumulating usage.
        HttpResponse<String> repeatedResponse = postJson(requestBody);
        org.junit.jupiter.api.Assertions.assertEquals(200, repeatedResponse.statusCode());
        org.junit.jupiter.api.Assertions.assertEquals(response.body(), repeatedResponse.body());
        org.junit.jupiter.api.Assertions.assertEquals(response.headers().firstValue("X-Invalid-Event-Count"),
                repeatedResponse.headers().firstValue("X-Invalid-Event-Count"));
    }

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
        org.junit.jupiter.api.Assertions.assertEquals(400, response.statusCode());
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

    @Test void validRequestReturnsContractAndInvalidCount() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content("""
                {"users":[{"user_id":"u","is_enterprise":false,"timezone":"UTC"}],
                 "events":[null,{}, {"event_id":"a","user_id":"u","timestamp":"2026-10-02T00:00:00Z","duration_minutes":15}],
                 "billing_cycle":"2026-10"}
                """))
                .andExpect(status().isOk()).andExpect(header().string("X-Invalid-Event-Count", "2"))
                .andExpect(jsonPath("$.invalidEventCount").doesNotExist())
                .andExpect(jsonPath("$.invalid_event_count").doesNotExist())
                .andExpect(jsonPath("$.users[0].user_id").value("u"))
                .andExpect(jsonPath("$.users[0].total_cost").value(0))
                .andExpect(jsonPath("$.users[0].tier_breakdown[0].free_hours").value(0.25))
                .andExpect(jsonPath("$.users[0].applied_surcharges").isEmpty());
    }
    @Test void missingAndInvalidUsersAreBadRequests() throws Exception {
        for (String body : new String[]{"{}", "{\"users\":[null],\"events\":[]}",
                "{\"users\":[{\"user_id\":\"u\",\"timezone\":\"UTC\"}],\"events\":[]}",
                "{\"users\":[{\"user_id\":\" \",\"is_enterprise\":false,\"timezone\":\"UTC\"}],\"events\":[]}"}) {
            mvc.perform(post(URL).contentType("application/json").content(body)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
    }
    @Test void serviceSkipsInvalidValuesAndNonQuarterHourStartTimes() throws Exception {
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
                .andExpect(header().string("X-Invalid-Event-Count", "9"))
                .andExpect(jsonPath("$.users[0].tier_breakdown[0].free_hours").value(0.25));
    }
    @Test void incompatibleEventTypesAreSkippedWithoutLosingAdjacentValidEvents() throws Exception {
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
                    .andExpect(header().string("X-Invalid-Event-Count", "1"))
                    .andExpect(jsonPath("$.users[0].tier_breakdown[0].free_hours").value(0.5));
        }
    }
    @Test void malformedEventsDoNotChangePaidBillAndAreCountedTogether() throws Exception {
        StringBuilder events = new StringBuilder();
        for (int i = 0; i < 41; i++) {
            if (i > 0) { events.append(','); }
            events.append("{\"event_id\":\"").append(i)
                    .append("\",\"user_id\":\"u\",\"timestamp\":\"2026-10-02T12:15:00Z\",\"duration_minutes\":15}");
        }
        String body = "{\"users\":[{\"user_id\":\"u\",\"is_enterprise\":false,\"timezone\":\"UTC\"}],"
                + "\"billing_cycle\":\"2026-10\",\"events\":[{\"duration_minutes\":[15]},"
                + events + ",{\"event_id\":\"missing-fields\"},"
                + "{\"event_id\":\"bad-value\",\"user_id\":\"u\",\"timestamp\":\"2026-10-02T12:07:00Z\",\"duration_minutes\":20}]}";
        mvc.perform(post(URL).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(header().string("X-Invalid-Event-Count", "3"))
                .andExpect(jsonPath("$.users[0].total_cost").value(0.30));
    }
    @Test void clientArrayExampleAndWrongDurationTypesAreSkippedWithAllOtherFieldsValid() throws Exception {
        for (String duration : new String[]{"[15]", "\"15\"", "15.0", "{\"value\":15}", "null"}) {
            String malformed = "{\"event_id\":\"e1\",\"user_id\":\"user-1\","
                    + "\"timestamp\":\"2026-10-14T10:00:00Z\",\"duration_minutes\":" + duration + "}";
            String valid = "{\"event_id\":\"e2\",\"user_id\":\"user-1\","
                    + "\"timestamp\":\"2026-10-14T10:15:00Z\",\"duration_minutes\":15}";
            mvc.perform(post(URL).contentType("application/json").content(
                    "{\"users\":[{\"user_id\":\"user-1\",\"is_enterprise\":false,\"timezone\":\"UTC\"}],"
                            + "\"billing_cycle\":\"2026-10\",\"events\":[" + malformed + "," + valid + "]}"))
                    .andExpect(status().isOk()).andExpect(header().string("X-Invalid-Event-Count", "1"))
                    .andExpect(jsonPath("$.users[0].tier_breakdown[0].free_hours").value(0.25));
        }
    }
    @Test void requestShapeErrorsAreNotTreatedAsInvalidEvents() throws Exception {
        for (String body : new String[]{"{\"users\":[],\"events\":{}}",
                "{\"users\":[],\"events\":null}",
                "{\"users\":[{\"user_id\":\"u\",\"is_enterprise\":[],\"timezone\":\"UTC\"}],\"events\":[]}",
                "{\"users\":[],\"events\":[{\"duration_minutes\":[15]}"}) {
            mvc.perform(post(URL).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        }
    }
    @Test void duplicateKeysInAnEventRemainARequestError() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content("""
                {"users":[],"events":[{"event_id":"a","event_id":"b"}]}
                """)).andExpect(status().isBadRequest());
    }
    @Test void malformedJsonIsHandledCentrally() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content("{bad"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
    @Test void coercionDuplicateJsonKeysAndTrailingDocumentsAreRejected() throws Exception {
        for (String body : new String[]{
                "{\"users\":[{\"user_id\":\"u\",\"is_enterprise\":\"false\",\"timezone\":\"UTC\"}],\"events\":[]}",
                "{\"users\":[],\"users\":[],\"events\":[]}",
                "{\"users\":[],\"events\":[]} {}",
                "{\"users\":[],\"events\":[" + "[".repeat(20) + "0" + "]".repeat(20) + "]}"}) {
            mvc.perform(post(URL).contentType("application/json").content(body))
                    .andExpect(status().isBadRequest());
        }
    }
    @Test void invalidTimezoneAndCycleAreBadRequests() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content("""
                {"users":[{"user_id":"u","is_enterprise":false,"timezone":"No/Zone"}],"events":[]}
                """)).andExpect(status().isBadRequest());
        mvc.perform(post(URL).contentType("application/json").content("""
                {"users":[],"events":[],"billing_cycle":"2026-00"}
                """)).andExpect(status().isBadRequest());
    }
    @Test void batchAndBodyLimitsReturn413() throws Exception {
        mvc.perform(post(URL).contentType("application/json").content("{\"users\":[],\"events\":[" + "null,".repeat(50) + "null]}"))
                .andExpect(status().isContentTooLarge()).andExpect(jsonPath("$.code").value("BATCH_LIMIT"));
        mvc.perform(post(URL).contentType("application/json").content(" ".repeat(8193)))
                .andExpect(status().isContentTooLarge()).andExpect(jsonPath("$.code").value("BODY_LIMIT"));
    }
    @Test void paidBillHasReconciledDtoBreakdown() throws Exception {
        StringBuilder events = new StringBuilder();
        for (int i = 0; i < 41; i++) {
            if (i > 0) { events.append(','); }
            events.append("{\"event_id\":\"").append(i)
                    .append("\",\"user_id\":\"u\",\"timestamp\":\"2026-10-02T12:15:00Z\",\"duration_minutes\":15}");
        }
        String body = "{\"users\":[{\"user_id\":\"u\",\"is_enterprise\":false,\"timezone\":\"UTC\"}],\"events\":["
                + events + "],\"billing_cycle\":\"2026-10\"}";
        mvc.perform(post(URL).contentType("application/json").content(body)).andExpect(status().isOk())
                .andExpect(header().string("X-Invalid-Event-Count", "0"))
                .andExpect(jsonPath("$.users[0].total_cost").value(0.30))
                .andExpect(jsonPath("$.users[0].tier_breakdown[0].hourly_rate").value(1.00))
                .andExpect(jsonPath("$.users[0].tier_breakdown[0].cost").value(0.25))
                .andExpect(jsonPath("$.users[0].applied_surcharges[0].cost").value(0.05));
    }
    @Test void unsupportedMediaAndMethodHaveAppropriateStatuses() throws Exception {
        mvc.perform(post(URL).contentType("text/plain").content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(URL))
                .andExpect(status().isMethodNotAllowed());
    }
}
