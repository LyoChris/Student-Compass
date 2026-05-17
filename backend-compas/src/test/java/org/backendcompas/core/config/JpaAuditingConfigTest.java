package org.backendcompas.core.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JpaAuditingConfigTest {

    @Test
    void constructsConfig() {
        JpaAuditingConfig config = new JpaAuditingConfig();
        assertThat(config).isNotNull();
    }
}
