package vn.t3tarot.api.identity.emailverification.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import vn.t3tarot.api.identity.emailverification.config.VerificationMailProperties;
import vn.t3tarot.api.identity.emailverification.enumeration.EmailVerificationOutcome;
import vn.t3tarot.api.identity.emailverification.policy.VerificationTokenPolicy;
import vn.t3tarot.api.identity.emailverification.service.EmailVerificationFacade;
import vn.t3tarot.api.identity.shared.config.IdentityProperties;

@Hidden
@Controller
@RequestMapping(EmailVerificationPageController.PATH)
@RequiredArgsConstructor
class EmailVerificationPageController {

    static final String PATH = "/auth/email/verify-page";
    static final String CONFIRM_VIEW = "page/email-verify-confirm";
    static final String RESULT_VIEW = "page/email-verify-result";
    static final String CONTENT_SECURITY_POLICY = "default-src 'none'; style-src 'unsafe-inline' "
            + "https://fonts.googleapis.com; font-src https://fonts.gstatic.com; form-action 'self'; "
            + "base-uri 'none'; frame-ancestors 'none'";
    static final String CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy";
    static final String REFERRER_POLICY_HEADER = "Referrer-Policy";
    static final String REFERRER_POLICY = "no-referrer";

    private final EmailVerificationFacade emailVerificationFacade;
    private final VerificationMailProperties mailProperties;
    private final IdentityProperties identityProperties;

    @GetMapping
    ModelAndView open(@RequestParam(required = false) String token, HttpServletResponse response) {
        applyPageHeaders(response);
        if (!VerificationTokenPolicy.isWellFormed(token)) {
            return result(EmailVerificationOutcome.LINK_INVALID);
        }
        ModelAndView page = new ModelAndView(CONFIRM_VIEW);
        page.addObject("token", token);
        page.addObject("actionPath", PATH);
        return page;
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ModelAndView confirm(@RequestParam(required = false) String token, HttpServletResponse response) {
        applyPageHeaders(response);
        return result(emailVerificationFacade.verifyFromLink(token));
    }

    private ModelAndView result(EmailVerificationOutcome outcome) {
        ModelAndView page = new ModelAndView(RESULT_VIEW);
        page.setStatus(outcome.isSuccessful() ? HttpStatus.OK : HttpStatus.BAD_REQUEST);
        page.addObject("outcome", outcome.name());
        page.addObject("lifetime", VerificationTokenPolicy.describeLifetime(identityProperties.emailVerificationTtl()));
        page.addObject("loginUrl", mailProperties.webLoginUrl());
        page.addObject("resendUrl", mailProperties.webResendUrl());
        page.addObject("homeUrl", mailProperties.webHomeUrl());
        return page;
    }

    private static void applyPageHeaders(HttpServletResponse response) {
        response.setHeader(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY);
        response.setHeader(REFERRER_POLICY_HEADER, REFERRER_POLICY);
    }
}
