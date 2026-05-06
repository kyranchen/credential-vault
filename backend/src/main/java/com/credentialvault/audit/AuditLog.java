package com.credentialvault.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "audit_logs")
@CompoundIndexes({
    @CompoundIndex(name = "org_time_idx", def = "{'orgId': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "cred_time_idx", def = "{'credentialId': 1, 'timestamp': -1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    @Id
    private String id;

    private String credentialId;
    private String accessedBy;

    @Indexed
    private String orgId;

    private String teamId;  // null for direct admin access
    private String sourceIp;
    private LocalDateTime timestamp;
    private Action action;

    public enum Action {
        CREDENTIAL_USED,
        CREDENTIAL_CREATED,
        CREDENTIAL_DELETED,
        ACCESS_GRANTED,
        ACCESS_REVOKED
    }
}
