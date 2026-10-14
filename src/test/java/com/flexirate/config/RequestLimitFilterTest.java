package com.flexirate.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class RequestLimitFilterTest {
    /**
     * HTTP 413 with BODY_LIMIT; neither reading the request body nor entering the downstream chain is allowed.
     */
    @Test
    void declaredOversizedBodyIsRejectedBeforeReadingOrDeserializing() throws Exception {
        RequestLimitFilter filter = new RequestLimitFilter(
                new BillingProperties(10, 10, 1024, false, 1), JsonMapper.builder().build());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/billing/calculate") {
            @Override
            public long getContentLengthLong() {
                return 1025;
            }

            @Override
            public jakarta.servlet.ServletInputStream getInputStream() {
                throw new AssertionError("A declared oversized body must be rejected before reading it");
            }
        };
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> fail("Oversized body must not reach JSON binding"));

        assertEquals(413, response.getStatus());
        assertTrue(response.getContentAsString().contains("BODY_LIMIT"));
    }

    /**
     * The downstream chain receives the unchanged {} body and the response remains HTTP 200.
     */
    @Test void boundedBodyRemainsReadableByDownstreamMvc() throws Exception {
        RequestLimitFilter filter = new RequestLimitFilter(new BillingProperties(10, 10, 1024, false, 1), JsonMapper.builder().build());
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request(), response, (req, res) -> {
            String body = new String(req.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals("{}", body);
        });
        assertEquals(200, response.getStatus());
    }

    /**
     * An oversized body with unknown length returns HTTP 413 and never enters the downstream chain.
     */
    @Test void unknownContentLengthCannotBypassBodyLimit() throws Exception {
        RequestLimitFilter filter = new RequestLimitFilter(new BillingProperties(10, 10, 1024, false, 1), JsonMapper.builder().build());
        MockHttpServletRequest request = new MockHttpServletRequest() {
            @Override public long getContentLengthLong() { return -1; }
        };
        request.setMethod("POST"); request.setServletPath("/api/v1/billing/calculate");
        request.setContent(new byte[1025]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> fail("Oversized body must not reach MVC"));
        assertEquals(413, response.getStatus());
    }
    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/billing/calculate");
        request.setServletPath("/api/v1/billing/calculate"); request.setContent("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return request;
    }
}
