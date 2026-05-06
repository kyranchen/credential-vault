package com.credentialvault.team.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TeamResponse(
    String id,
    String name,
    List<String> memberUserIds,
    LocalDateTime createdAt
) {}
