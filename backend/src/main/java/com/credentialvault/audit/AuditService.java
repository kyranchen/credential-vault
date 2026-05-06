package com.credentialvault.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditRepository auditRepository;

    public void log(
        String credentialId,
        String accessedBy,
        String orgId,
        String teamId,
        String sourceIp,
        AuditLog.Action action
    ) {
        try {
            auditRepository.save(
                AuditLog.builder()
                    .credentialId(credentialId)
                    .accessedBy(accessedBy)
                    .orgId(orgId)
                    .teamId(teamId)
                    .sourceIp(sourceIp)
                    .timestamp(LocalDateTime.now())
                    .action(action)
                    .build()
            );
        } catch (Exception e) {
            // Log the failure but don't propagate — a failed audit write must not block
            // the credential use response. Emit an alert here in production (e.g. PagerDuty).
            log.error("AUDIT WRITE FAILED for credentialId={} action={}: {}", credentialId, action, e.getMessage());
        }
    }
}
