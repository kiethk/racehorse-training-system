package com.rtms.backend.identity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rtms.backend.config.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class TurnstileVerifier {
    private static final Logger log = LoggerFactory.getLogger(TurnstileVerifier.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final boolean enabled;
    private final String secretKey;
    private final String verifyUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public TurnstileVerifier(
            ObjectMapper objectMapper,
            @Value("${rtms.captcha.enabled:true}") boolean enabled,
            @Value("${rtms.captcha.turnstile.secret-key}") String secretKey,
            @Value("${rtms.captcha.turnstile.verify-url}") String verifyUrl) {
        this(objectMapper, enabled, secretKey, verifyUrl, HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build());
    }

    TurnstileVerifier(ObjectMapper objectMapper, boolean enabled, String secretKey, String verifyUrl,
            HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.secretKey = secretKey;
        this.verifyUrl = verifyUrl;
        this.httpClient = httpClient;
    }

    public void verify(String token, String remoteIp) {
        if (!enabled) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw failed();
        }

        try {
            HttpResponse<String> response = send(token, remoteIp);
            if (response.statusCode() != 200) {
                throw unavailable();
            }
            JsonNode result = objectMapper.readTree(response.body());
            if (!result.path("success").asBoolean(false)) {
                throw failed();
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw unavailable();
        }
    }

    HttpResponse<String> send(String token, String remoteIp) throws IOException, InterruptedException {
        String form = "secret=" + encode(secretKey) + "&response=" + encode(token);
        if (remoteIp != null && !remoteIp.isBlank()) {
            form += "&remoteip=" + encode(remoteIp);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(verifyUrl))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static ApiException failed() {
        return new ApiException(HttpStatus.BAD_REQUEST, "CAPTCHA_FAILED",
                "Please complete the security check and try again");
    }

    private static ApiException unavailable() {
        log.warn("Turnstile verification service is unavailable");
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CAPTCHA_UNAVAILABLE",
                "The security check is unavailable. Please try again shortly");
    }
}
