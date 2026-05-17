package org.backendcompas.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void buildsOpenApiDefinition() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI api = config.openAPI();

        assertThat(api).isNotNull();
        assertThat(api.getInfo()).isNotNull();
        assertThat(api.getInfo().getTitle()).isEqualTo("Student Compass API");
        assertThat(api.getComponents()).isNotNull();
        assertThat(api.getSecurity()).isNotEmpty();
    }
}
