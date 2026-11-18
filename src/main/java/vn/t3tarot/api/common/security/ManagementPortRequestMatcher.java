package vn.t3tarot.api.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

public record ManagementPortRequestMatcher(int managementPort, int serverPort) implements RequestMatcher {

    @Override
    public boolean matches(HttpServletRequest request) {
        return isSeparatePort() && request.getLocalPort() == managementPort;
    }

    private boolean isSeparatePort() {
        return managementPort > 0 && managementPort != serverPort;
    }
}
