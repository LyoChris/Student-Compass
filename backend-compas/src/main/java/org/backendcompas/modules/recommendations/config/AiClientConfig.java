package org.backendcompas.modules.recommendations.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AiClientConfig {

    public static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    @Bean(name = "aiRestClient")
    public RestClient aiRestClient(
            @Value("${ai.service.url}") String aiServiceUrl,
            @Value("${ai.service.secret}") String aiServiceSecret
    ) {
        return RestClient.builder()
                .baseUrl(aiServiceUrl)
                .requestFactory(aiRequestFactory())
                .defaultHeader(INTERNAL_SECRET_HEADER, aiServiceSecret)
                .build();
    }

    private ClientHttpRequestFactory aiRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofMinutes(5));
        return factory;
    }
}
