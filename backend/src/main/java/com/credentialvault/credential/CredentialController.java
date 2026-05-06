package com.credentialvault.credential;

import com.credentialvault.auth.AuthenticatedUser;
import com.credentialvault.credential.dto.CredentialResponse;
import com.credentialvault.credential.dto.CreateCredentialRequest;
import com.credentialvault.credential.dto.GrantAccessRequest;
import com.credentialvault.credential.dto.UseCredentialResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/credentials")
@RequiredArgsConstructor
public class CredentialController {

    private final CredentialService credentialService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CredentialResponse create(
        @RequestBody @Valid CreateCredentialRequest request,
        @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return credentialService.create(request, actor);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CredentialResponse> list(@AuthenticationPrincipal AuthenticatedUser actor) {
        return credentialService.listForOrg(actor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(
        @PathVariable String id,
        @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        credentialService.delete(id, actor);
    }

    @PostMapping("/{id}/grant")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void grantAccess(
        @PathVariable String id,
        @RequestBody @Valid GrantAccessRequest request,
        @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        credentialService.grantAccess(id, request.teamId(), actor);
    }

    @DeleteMapping("/{id}/grant/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void revokeAccess(
        @PathVariable String id,
        @PathVariable String teamId,
        @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        credentialService.revokeAccess(id, teamId, actor);
    }

    /**
     * Available to both ADMIN and MEMBER. Permission check is enforced inside the service.
     * Source IP is extracted here so the service stays HTTP-agnostic and testable.
     */
    @GetMapping("/{id}/use")
    public UseCredentialResponse use(
        @PathVariable String id,
        @AuthenticationPrincipal AuthenticatedUser actor,
        HttpServletRequest request
    ) {
        return credentialService.use(id, actor, extractClientIp(request));
    }

    private String extractClientIp(HttpServletRequest request) {
        // X-Forwarded-For can be spoofed if not behind a trusted proxy.
        // For production, only trust this header from known proxy IPs.
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
