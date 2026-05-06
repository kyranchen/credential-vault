package com.credentialvault.audit;

import com.credentialvault.auth.AuthenticatedUser;
import com.credentialvault.audit.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditRepository auditRepository;

    @GetMapping
    public Page<AuditLogResponse> list(
        @AuthenticationPrincipal AuthenticatedUser actor,
        @RequestParam(required = false) String credentialId,
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @PageableDefault(size = 50, sort = "timestamp") Pageable pageable
    ) {
        Page<AuditLog> page;

        if (credentialId != null) {
            page = auditRepository.findByOrgIdAndCredentialId(actor.orgId(), credentialId, pageable);
        } else if (userId != null) {
            page = auditRepository.findByOrgIdAndAccessedBy(actor.orgId(), userId, pageable);
        } else if (from != null && to != null) {
            page = auditRepository.findByOrgIdAndTimestampBetween(actor.orgId(), from, to, pageable);
        } else {
            page = auditRepository.findByOrgId(actor.orgId(), pageable);
        }

        return page.map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
            log.getId(), log.getCredentialId(), log.getAccessedBy(),
            log.getTeamId(), log.getSourceIp(), log.getTimestamp(), log.getAction()
        );
    }
}
