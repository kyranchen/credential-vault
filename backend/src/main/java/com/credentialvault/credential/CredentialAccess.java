package com.credentialvault.credential;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "credential_access")
@CompoundIndex(name = "cred_team_idx", def = "{'credentialId': 1, 'teamId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialAccess {
    @Id
    private String id;

    private String credentialId;
    private String teamId;

    // Denormalized: avoids a join when checking access within an org
    private String orgId;

    private String grantedBy;
    private LocalDateTime grantedAt;
}
