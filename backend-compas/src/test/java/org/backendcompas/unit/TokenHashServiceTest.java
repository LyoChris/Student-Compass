package org.backendcompas.unit;

import org.backendcompas.modules.account.service.TokenHashService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHashServiceTest {

    private final TokenHashService service = new TokenHashService();

    @Test
    void hashTokenIsDeterministic() {
        String token = "some.jwt.token";
        assertThat(service.hashToken(token)).isEqualTo(service.hashToken(token));
    }

    @Test
    void hashTokenProduces64CharHexString() {
        String hash = service.hashToken("any-token-value");
        assertThat(hash).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void differentTokensProduceDifferentHashes() {
        assertThat(service.hashToken("token-a")).isNotEqualTo(service.hashToken("token-b"));
    }
}
