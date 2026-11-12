package com.rtms.backend.identity.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.config.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TurnstileVerifierTest {
    @Test
    void acceptsSuccessfulVerification() throws Exception {
        TurnstileVerifier verifier = verifier(response(200, "{\"success\":true}"));

        verifier.verify("valid-token", "192.0.2.1");
    }

    @Test
    void rejectsUnsuccessfulVerification() throws Exception {
        TurnstileVerifier verifier = verifier(response(200, "{\"success\":false}"));

        ApiException exception = assertThrows(ApiException.class,
                () -> verifier.verify("invalid-token", null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("CAPTCHA_FAILED", exception.getErrorCode());
    }

    @Test
    void rejectsBlankTokenWithoutSendingRequest() throws Exception {
        TurnstileVerifier verifier = verifier(null);

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("  ", null));

        assertEquals("CAPTCHA_FAILED", exception.getErrorCode());
    }

    @Test
    void mapsSenderIoFailureToUnavailable() {
        TurnstileVerifier verifier = new TurnstileVerifier(new ObjectMapper(), true, "secret", "url",
                HttpClient.newHttpClient()) {
            @Override
            HttpResponse<String> send(String token, String remoteIp) throws IOException {
                throw new IOException("network down");
            }
        };

        ApiException exception = assertThrows(ApiException.class, () -> verifier.verify("token", null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertEquals("CAPTCHA_UNAVAILABLE", exception.getErrorCode());
    }

    @Test
    void mapsNonSuccessfulHttpStatusToUnavailable() throws Exception {
        TurnstileVerifier verifier = verifier(response(503, "{}"));

        ApiException exception = assertThrows(ApiException.class,
                () -> verifier.verify("token", null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertEquals("CAPTCHA_UNAVAILABLE", exception.getErrorCode());
    }

    @Test
    void disabledVerifierReturnsWithoutSendingRequest() {
        TurnstileVerifier verifier = new TurnstileVerifier(new ObjectMapper(), false, "", "",
                HttpClient.newHttpClient());

        verifier.verify(null, null);
    }

    private TurnstileVerifier verifier(HttpResponse<String> response) throws Exception {
        return new TurnstileVerifier(new ObjectMapper(), true, "secret", "url", HttpClient.newHttpClient()) {
            @Override
            HttpResponse<String> send(String token, String remoteIp) throws IOException {
                return response;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
}
