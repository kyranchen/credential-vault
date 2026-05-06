package com.credentialvault.credential.dto;

import jakarta.validation.constraints.NotBlank;

public record GrantAccessRequest(@NotBlank String teamId) {}
