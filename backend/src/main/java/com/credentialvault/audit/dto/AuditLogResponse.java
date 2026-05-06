package com.credentialvault.audit.dto;

import com.credentialvault.audit.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
    String id,
    String credentialId,
    String accessedBy,
    String teamId,
    String sourceIp,
    LocalDateTime timestamp,
    AuditLog.Action action
) {}
