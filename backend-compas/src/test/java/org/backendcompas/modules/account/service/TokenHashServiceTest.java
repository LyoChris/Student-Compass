package org.backendcompas.modules.account.service;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenHashServiceTest {

    @Test
    void hashesTokenWithSha256() {
        TokenHashService service = new TokenHashService();

        String hash = service.hashToken("token");

        assertThat(hash)
                .hasSize(64)
                .isEqualTo(service.hashToken("token"));
    }

    @Test
    void wrapsMissingDigestAlgorithm() {
        try (MockedStatic<MessageDigest> mocked = Mockito.mockStatic(MessageDigest.class)) {
            mocked.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException("missing"));

            TokenHashService service = new TokenHashService();

            assertThatThrownBy(() -> service.hashToken("token"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SHA-256 digest is not available");
        }
    }
}
