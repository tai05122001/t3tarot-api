package vn.t3tarot.api.identity.shared.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import vn.t3tarot.api.identity.shared.entity.ClientInfo;

public final class ClientInfoResolver {

    private ClientInfoResolver() {
    }

    public static ClientInfo resolve(HttpServletRequest request) {
        return new ClientInfo(request.getHeader(HttpHeaders.USER_AGENT), request.getRemoteAddr());
    }
}
