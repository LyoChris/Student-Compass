package org.backendcompas.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class SpaCsrfTokenRequestHandlerTest {

    @Test
    void handlesTokenAndResolvesFromHeaderWhenPresent() {
        SpaCsrfTokenRequestHandler handler = new SpaCsrfTokenRequestHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "XSRF-TOKEN", "token");
        Supplier<CsrfToken> supplier = () -> token;

        request.addHeader("X-XSRF-TOKEN", "token");

        handler.handle(request, response, supplier);
        String resolved = handler.resolveCsrfTokenValue(request, token);

        assertThat(resolved).isEqualTo("token");
    }

    @Test
    void resolvesFromRequestWhenHeaderMissing() {
        SpaCsrfTokenRequestHandler handler = new SpaCsrfTokenRequestHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "XSRF-TOKEN", "token");
        Supplier<CsrfToken> supplier = () -> token;

        handler.handle(request, response, supplier);
        CsrfToken xorToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        request.setParameter(token.getParameterName(), xorToken.getToken());

        String resolved = handler.resolveCsrfTokenValue(request, token);

        assertThat(resolved).isNotNull();
    }
}
