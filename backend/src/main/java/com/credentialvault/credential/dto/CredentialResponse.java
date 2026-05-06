package com.credentialvault.credential.dto;

import java.time.LocalDateTime;

// Never includes encryptedPassword, iv, or keyReference — metadata only
public record CredentialResponse(
    String id,
    String name,
    String url,
    String username,
    String createdBy,
    LocalDateTime createdAt
) {}
