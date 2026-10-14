package com.flexirate.config;

import com.flexirate.dto.ErrorDto;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.*;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Rejects oversized POST bodies before Spring/Jackson binds them to DTOs.
 */
@Component
public class RequestLimitFilter extends OncePerRequestFilter {
    private final int maxRequestBytes;
    private final ObjectMapper objectMapper;

    /**
     * Reads the hard body-size cap used before DTO binding begins.
     */
    public RequestLimitFilter(BillingProperties billingProperties, ObjectMapper objectMapper) {
        maxRequestBytes = billingProperties.maxRequestBytes();
        this.objectMapper = objectMapper;
    }

    /**
     * Applies body limits to POST requests; other HTTP methods use the normal MVC path.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest httpServletRequest) {
        return !httpServletRequest.getMethod().equals("POST");
    }

    /**
     * Checks declared/actual body size and replays the bounded body to the normal filter chain.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (httpServletRequest.getContentLengthLong() > maxRequestBytes) {
            error(httpServletResponse, 413, "BODY_LIMIT", "Request body limit exceeded.");
            return;
        }
        // Read one extra byte to detect over-limit chunked bodies without trusting Content-Length.
        byte[] requestBody = httpServletRequest.getInputStream().readNBytes(maxRequestBytes + 1);
        if (requestBody.length > maxRequestBytes) {
            error(httpServletResponse, 413, "BODY_LIMIT", "Request body limit exceeded.");
            return;
        }
        filterChain.doFilter(new HttpServletRequestWrapper(httpServletRequest) {
            /** Replays the bounded body to Spring's synchronous JSON message converter. */
            @Override
            public ServletInputStream getInputStream() {
                ByteArrayInputStream requestBodyInputStream = new ByteArrayInputStream(requestBody);
                return new ServletInputStream() {
                    @Override
                    public int read() {
                        return requestBodyInputStream.read();
                    }

                    @Override
                    public int read(byte[] bytes, int offset, int length) {
                        return requestBodyInputStream.read(bytes, offset, length);
                    }

                    @Override
                    public boolean isFinished() {
                        return requestBodyInputStream.available() == 0;
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setReadListener(ReadListener listener) {
                        throw new UnsupportedOperationException("Synchronous MVC only");
                    }
                };
            }

            /** Provides the character-based Servlet view of the same bounded UTF-8 body. */
            @Override
            public BufferedReader getReader() {
                return new BufferedReader(new InputStreamReader(getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            }
        }, httpServletResponse);
    }

    /**
     * Writes the common error contract for failures occurring before MVC advice can handle them.
     */
    private void error(HttpServletResponse httpServletResponse, int statusCode, String errorCode, String errorMessage) throws IOException {
        httpServletResponse.setStatus(statusCode);
        httpServletResponse.setContentType("application/json");
        objectMapper.writeValue(httpServletResponse.getOutputStream(), new ErrorDto(errorCode, errorMessage));
    }
}
