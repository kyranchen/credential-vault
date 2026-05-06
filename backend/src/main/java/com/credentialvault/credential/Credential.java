package com.credentialvault.credential;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Credential {
    @Id
    private String id;

    @Indexed
    private String orgId;

    private String name;
    private String url;
    private String username;

    // AES-256-GCM ciphertext + 128-bit auth tag, base64-encoded
    private String encryptedPassword;

    // 96-bit IV, base64-encoded — unique per encryption operation
    private String iv;

    // Pointer to the key in key provider (supports rotation)
    private String keyReference;

    private String createdBy;
    private LocalDateTime createdAt;
}
