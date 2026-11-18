package vn.t3tarot.api.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ManagementPortRequestMatcherTest {

    @Test
    void matchesOnlyRequestsArrivingOnManagementPort() {
        ManagementPortRequestMatcher matcher = new ManagementPortRequestMatcher(8081, 8080);

        assertThat(matcher.matches(requestOnPort(8081))).isTrue();
        assertThat(matcher.matches(requestOnPort(8080))).isFalse();
    }

    @Test
    void neverMatchesWhenManagementPortEqualsServerPort() {
        ManagementPortRequestMatcher matcher = new ManagementPortRequestMatcher(8080, 8080);

        assertThat(matcher.matches(requestOnPort(8080))).isFalse();
    }

    @Test
    void neverMatchesWhenManagementPortIsNotConfigured() {
        ManagementPortRequestMatcher matcher = new ManagementPortRequestMatcher(-1, 8080);

        assertThat(matcher.matches(requestOnPort(-1))).isFalse();
        assertThat(matcher.matches(requestOnPort(8080))).isFalse();
    }

    private static MockHttpServletRequest requestOnPort(int port) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setLocalPort(port);
        return request;
    }
}
