package vn.t3tarot.api.identity.register.controller;

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
import vn.t3tarot.api.identity.register.dto.request.RegisterRequest;
import vn.t3tarot.api.identity.register.dto.response.RegisterResponse;
import vn.t3tarot.api.identity.register.service.RegisterFacade;

@Tag(name = "auth")
@RestController
@RequestMapping("/auth/register")
@RequiredArgsConstructor
class RegisterController {

    private final RegisterFacade registerFacade;

    @Operation(summary = "Register an account with username, email and password")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RegisterResponse register(
            @RequestHeader(value = IdempotencyKey.HEADER_NAME, required = false) String idempotencyKey,
            @Valid @RequestBody RegisterRequest request) {
        IdempotencyKey.parse(idempotencyKey);
        return new RegisterResponse(registerFacade.register(request));
    }
}
