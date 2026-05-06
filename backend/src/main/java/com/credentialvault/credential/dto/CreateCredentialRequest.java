package com.credentialvault.credential.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record CreateCredentialRequest(
    @NotBlank String name,
    @NotBlank @URL String url,
    @NotBlank String username,
    @NotBlank String password
) {}
