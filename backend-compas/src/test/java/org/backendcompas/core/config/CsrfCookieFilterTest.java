package org.backendcompas.core.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CsrfCookieFilterTest {

    @Test
    void readsCsrfTokenAndContinuesChain() throws Exception {
        CsrfCookieFilter filter = new CsrfCookieFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        CsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "XSRF-TOKEN", "token");
        request.setAttribute(CsrfToken.class.getName(), token);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void continuesChainWithoutToken() throws Exception {
        CsrfCookieFilter filter = new CsrfCookieFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
