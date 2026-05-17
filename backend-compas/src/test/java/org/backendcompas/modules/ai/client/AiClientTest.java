package org.backendcompas.modules.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.backendcompas.modules.ai.client.dto.AiChatContext;
import org.backendcompas.modules.ai.client.dto.AiChatRequest;
import org.backendcompas.modules.ai.client.dto.AiChatResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class AiClientTest {

    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        
        server.createContext("/chat", exchange -> {
            // Verify secret header
            String secret = exchange.getRequestHeaders().getFirst("X-Internal-Secret");
            if (!"test-secret".equals(secret)) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }
            
            // Check if we want to simulate a failure
            try {
                AiChatRequest req = new ObjectMapper().readValue(exchange.getRequestBody(), AiChatRequest.class);
                if ("FAIL".equals(req.message())) {
                    exchange.sendResponseHeaders(500, -1);
                    return;
                }
            } catch (Exception e) {
                // ignore
            }

            String response = "{\"reply\":\"Hello from mock AI\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterAll
    static void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void chatReturnsSuccessfulResponse() {
        AiClient aiClient = new AiClient(baseUrl, "test-secret");
        
        AiChatRequest request = new AiChatRequest(
                new AiChatContext(null, null),
                Collections.emptyList(),
                "Hello"
        );
        
        AiChatResponse response = aiClient.chat(request);
        
        assertThat(response).isNotNull();
        assertThat(response.reply()).isEqualTo("Hello from mock AI");
    }

    @Test
    void chatReturnsFallbackOnServerError() {
        AiClient aiClient = new AiClient(baseUrl, "test-secret");
        
        AiChatRequest request = new AiChatRequest(
                new AiChatContext(null, null),
                Collections.emptyList(),
                "FAIL"
        );
        
        AiChatResponse response = aiClient.chat(request);
        
        assertThat(response).isNotNull();
        assertThat(response.reply()).isEqualTo("Sorry, the AI coach is temporarily unavailable. Please try again later.");
    }
    
    @Test
    void chatReturnsFallbackOnConnectionError() {
        // Point to a port where nothing is running
        AiClient aiClient = new AiClient("http://localhost:1", "test-secret");
        
        AiChatRequest request = new AiChatRequest(
                new AiChatContext(null, null),
                Collections.emptyList(),
                "Hello"
        );
        
        AiChatResponse response = aiClient.chat(request);
        
        assertThat(response).isNotNull();
        assertThat(response.reply()).isEqualTo("Sorry, the AI coach is temporarily unavailable. Please try again later.");
    }
}
