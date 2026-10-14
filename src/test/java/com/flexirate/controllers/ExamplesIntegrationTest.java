package com.flexirate.controllers;

import com.flexirate.FlexiRateApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/** Runs each documented request against the real HTTP server and compares its saved JSON response. */
class ExamplesIntegrationTest {
    private static ConfigurableApplicationContext applicationContext;
    private static HttpClient httpClient;
    private static URI billingEndpoint;

    @BeforeAll
    static void startApplication() {
        applicationContext = new SpringApplicationBuilder(FlexiRateApplication.class, ExampleClockConfiguration.class)
                .run("--server.port=0");
        int port = ((WebServerApplicationContext) applicationContext).getWebServer().getPort();
        billingEndpoint = URI.create("http://localhost:" + port + "/api/v1/billing/calculate");
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @AfterAll
    static void stopApplication() {
        if (httpClient != null) {
            httpClient.close();
        }
        if (applicationContext != null) {
            applicationContext.close();
        }
    }

    /**
     * HTTP 200; X-Invalid-Event-Count: 0; expected totals alice: $0.30. Compare every field in the saved bill
     * JSON.
     */
    @Test
    void validWithBillingCycleMatchesExample() throws Exception {
        assertExample("examples/scenarios/01-valid-with-billing-cycle", 200);
    }

    /**
     * HTTP 200; X-Invalid-Event-Count: 1; expected totals alice: $0.00. Compare every field in the saved bill
     * JSON.
     */
    @Test
    void malformedEventTypeMatchesExample() throws Exception {
        assertExample("examples/scenarios/02-malformed-event-type", 200);
    }

    /**
     * HTTP 400; code INVALID_REQUEST; message "Duplicate user_id."; no X-Invalid-Event-Count header. Compare the
     * entire saved error JSON.
     */
    @Test
    void duplicateUsersMatchesExample() throws Exception {
        assertExample("examples/scenarios/03-duplicate-users", 400);
    }

    /**
     * HTTP 200; X-Invalid-Event-Count: 0; expected totals alice: $0.30. Compare every field in the saved bill
     * JSON.
     */
    @Test
    void withoutBillingCycleMatchesExample() throws Exception {
        assertExample("examples/scenarios/04-without-billing-cycle", 200);
    }

    /**
     * HTTP 200; X-Invalid-Event-Count: 5; expected totals alice: $0.00. Compare every field in the saved bill
     * JSON.
     */
    @Test
    void invalidEventValuesMatchesExample() throws Exception {
        assertExample("examples/scenarios/05-invalid-event-values", 200);
    }

    /**
     * HTTP 200; X-Invalid-Event-Count: 1; expected totals alice: $0.00. Compare every field in the saved bill
     * JSON.
     */
    @Test
    void duplicateEventsMatchesExample() throws Exception {
        assertExample("examples/scenarios/06-duplicate-events", 200);
    }

    /**
     * HTTP 200; X-Invalid-Event-Count: 1; expected totals alice: $0.00. Compare every field in the saved bill
     * JSON.
     */
    @Test
    void unknownUserMatchesExample() throws Exception {
        assertExample("examples/scenarios/07-unknown-user", 200);
    }

    /**
     * HTTP 200; X-Invalid-Event-Count: 0; expected totals alice: $0.00, bob: $0.00. Compare every field in the
     * saved bill JSON.
     */
    @Test
    void emptyEventsMatchesExample() throws Exception {
        assertExample("examples/scenarios/08-empty-events", 200);
    }

    /**
     * HTTP 400; code INVALID_REQUEST; message "Invalid user timezone."; no X-Invalid-Event-Count header. Compare
     * the entire saved error JSON.
     */
    @Test
    void invalidTimezoneMatchesExample() throws Exception {
        assertExample("examples/scenarios/09-invalid-timezone", 400);
    }

    /**
     * HTTP 400; code INVALID_REQUEST; message "billing_cycle must be YYYY-MM."; no X-Invalid-Event-Count header.
     * Compare the entire saved error JSON.
     */
    @Test
    void invalidBillingCycleMatchesExample() throws Exception {
        assertExample("examples/scenarios/10-invalid-billing-cycle", 400);
    }

    /**
     * HTTP 200; X-Invalid-Event-Count: 0; expected totals sofia: $0.00, utc: $0.00. Compare every field in the
     * saved bill JSON.
     */
    @Test
    void timezoneMonthBoundaryMatchesExample() throws Exception {
        assertExample("examples/scenarios/11-timezone-month-boundary", 200);
    }

    /**
     * HTTP 200; X-Invalid-Event-Count: 0; expected totals regular: $90.00, enterprise: $72.00. Compare every field
     * in the saved bill JSON.
     */
    @Test
    void enterpriseTierComparisonMatchesExample() throws Exception {
        assertExample("examples/scenarios/12-enterprise-tier-comparison", 200);
    }

    /**
     * HTTP 200; X-Invalid-Event-Count: 0; expected totals peak: $0.30, off-peak: $0.25. Compare every field in the
     * saved bill JSON.
     */
    @Test
    void peakVersusOffPeakMatchesExample() throws Exception {
        assertExample("examples/scenarios/13-peak-versus-off-peak", 200);
    }

    /**
     * HTTP 200; X-Invalid-Event-Count: 3; expected totals alice: $0.00. Compare every field in the saved bill
     * JSON.
     */
    @Test
    void allEventsInvalidMatchesExample() throws Exception {
        assertExample("examples/scenarios/14-all-events-invalid", 200);
    }

    /**
     * HTTP 400; code INVALID_REQUEST; message "Provide valid JSON, users with IDs/enterprise flags/timezones, and
     * an events array."; no X-Invalid-Event-Count header. Compare the entire saved error JSON.
     */
    @Test
    void invalidRequestShapeMatchesExample() throws Exception {
        assertExample("examples/scenarios/15-invalid-request-shape", 400);
    }

    /**
     * HTTP 400; code INVALID_REQUEST; message "Provide valid JSON, users with IDs/enterprise flags/timezones, and
     * an events array."; no X-Invalid-Event-Count header. Compare the entire saved error JSON.
     */
    @Test
    void malformedJsonDocumentMatchesExample() throws Exception {
        assertExample("examples/scenarios/16-malformed-json-document", 400);
    }

    /**
     * HTTP 200; X-Invalid-Event-Count: 1; expected totals alice: $0.30. Compare every field in the saved bill
     * JSON.
     */
    @Test
    void originalPaidExampleMatchesResponse() throws Exception {
        assertExample("examples", 200);
    }

    private void assertExample(String folder, int expectedStatus) throws Exception {
        // Given: use the exact documented request, expected response and stable headers.
        String requestJson = readResource(folder + "/request.json");
        String expectedResponseJson = readResource(folder + "/response.json");
        JsonNode expectedHeaders = JsonMapper.builder().build().readTree(readResource(folder + "/response-headers.json"));

        // When: send the JSON through Tomcat, the request filter, DTO binding and billing services.
        HttpRequest httpRequest = HttpRequest.newBuilder(billingEndpoint)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        // Then: check the whole JSON contract. Numeric 0.30 and 0.3 are equal JSON amounts.
        assertEquals(expectedStatus, response.statusCode(), folder);
        JSONAssert.assertEquals(expectedResponseJson, response.body(), true);
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow()
                .startsWith(expectedHeaders.path("Content-Type").asText()), folder);
        if (expectedHeaders.has("X-Invalid-Event-Count")) {
            assertEquals(expectedHeaders.path("X-Invalid-Event-Count").asText(),
                    response.headers().firstValue("X-Invalid-Event-Count").orElseThrow(), folder);
        } else {
            assertTrue(response.headers().firstValue("X-Invalid-Event-Count").isEmpty(), folder);
        }
    }

    private static String readResource(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ExampleClockConfiguration {
        /** Keep the omitted-cycle example in its saved month even when these tests run years later. */
        @Bean
        @Primary
        Clock exampleClock() throws IOException {
            JsonNode expectedResponse = JsonMapper.builder().build().readTree(
                    readResource("examples/scenarios/04-without-billing-cycle/response.json"));
            YearMonth billingCycle = YearMonth.parse(expectedResponse.path("users").get(0).path("billing_cycle").asText());
            return Clock.fixed(billingCycle.atDay(2).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }
}
