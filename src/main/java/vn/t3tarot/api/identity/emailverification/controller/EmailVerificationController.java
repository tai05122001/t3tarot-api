package vn.t3tarot.api.identity.emailverification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.t3tarot.api.common.idempotency.IdempotencyKey;
import vn.t3tarot.api.identity.emailverification.dto.request.ResendEmailVerificationRequest;
import vn.t3tarot.api.identity.emailverification.dto.request.VerifyEmailRequest;
import vn.t3tarot.api.identity.emailverification.service.EmailVerificationFacade;

@Tag(name = "auth")
@RestController
@RequestMapping("/auth/email")
@RequiredArgsConstructor
class EmailVerificationController {

    private final EmailVerificationFacade emailVerificationFacade;

    @Operation(summary = "Confirm an email address with the emailed token")
    @PostMapping("/verify")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void verify(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationFacade.verify(request.token());
    }

    @Operation(summary = "Send a new verification email when the address is registered and unverified")
    @PostMapping("/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void resend(
            @RequestHeader(value = IdempotencyKey.HEADER_NAME, required = false) String idempotencyKey,
            @Valid @RequestBody ResendEmailVerificationRequest request) {
        IdempotencyKey.parse(idempotencyKey);
        emailVerificationFacade.resend(request.email());
    }
}
