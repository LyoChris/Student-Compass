package org.backendcompas.modules.ai.client;

import lombok.extern.slf4j.Slf4j;
import org.backendcompas.modules.ai.client.dto.AiChatRequest;
import org.backendcompas.modules.ai.client.dto.AiChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class AiClient {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    private static final String FALLBACK_REPLY =
            "Sorry, the AI coach is temporarily unavailable. Please try again later.";

    private final RestClient restClient;

    public AiClient(
            @Value("${app.ai.base-url:http://localhost:8001}") String baseUrl,
            @Value("${app.ai.internal-secret:dev-internal-secret}") String secret
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(INTERNAL_SECRET_HEADER, secret)
                .build();
    }

    public AiChatResponse chat(AiChatRequest request) {
        try {
            AiChatResponse response = restClient.post()
                    .uri("/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiChatResponse.class);

            return response != null ? response : new AiChatResponse(FALLBACK_REPLY);
        } catch (RestClientException ex) {
            log.warn("AI service unavailable: {}", ex.getMessage());
            return new AiChatResponse(FALLBACK_REPLY);
        }
    }
}
