package com.credentialvault.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;

public interface AuditRepository extends MongoRepository<AuditLog, String> {

    Page<AuditLog> findByOrgId(String orgId, Pageable pageable);

    Page<AuditLog> findByOrgIdAndCredentialId(String orgId, String credentialId, Pageable pageable);

    Page<AuditLog> findByOrgIdAndAccessedBy(String orgId, String accessedBy, Pageable pageable);

    Page<AuditLog> findByOrgIdAndTimestampBetween(
        String orgId, LocalDateTime from, LocalDateTime to, Pageable pageable
    );
}
