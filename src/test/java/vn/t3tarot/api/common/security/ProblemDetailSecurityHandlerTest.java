package vn.t3tarot.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

class ProblemDetailSecurityHandlerTest {

    private HandlerExceptionResolver resolver;
    private ProblemDetailSecurityHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        resolver = mock(HandlerExceptionResolver.class);
        handler = new ProblemDetailSecurityHandler(resolver);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void delegatesAuthenticationFailureToResolverAndAddsBearerChallenge() {
        BadCredentialsException failure = new BadCredentialsException("bad");
        when(resolver.resolveException(any(), any(), isNull(), any())).thenReturn(new ModelAndView());

        handler.commence(request, response, failure);

        verify(resolver).resolveException(request, response, null, failure);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo(ProblemDetailSecurityHandler.BEARER_CHALLENGE);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void delegatesAccessDeniedToResolver() {
        AccessDeniedException denied = new AccessDeniedException("denied");
        when(resolver.resolveException(any(), any(), isNull(), any())).thenReturn(new ModelAndView());

        handler.handle(request, response, denied);

        verify(resolver).resolveException(request, response, null, denied);
        assertThat(response.getHeader("WWW-Authenticate")).isNull();
    }

    @Test
    void fallsBackTo401WhenResolverDoesNotHandleAuthenticationFailure() {
        when(resolver.resolveException(any(), any(), isNull(), any())).thenReturn(null);

        handler.commence(request, response, new BadCredentialsException("bad"));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void fallsBackTo403WhenResolverDoesNotHandleAccessDenied() {
        when(resolver.resolveException(any(), any(), isNull(), any())).thenReturn(null);

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
    }
}
