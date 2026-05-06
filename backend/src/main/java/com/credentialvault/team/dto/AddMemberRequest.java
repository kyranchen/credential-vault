package com.credentialvault.team.dto;

import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(@NotBlank String userId) {}
