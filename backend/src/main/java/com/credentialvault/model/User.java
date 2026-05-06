package com.credentialvault.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;

    private String orgId;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private Role role;

    private LocalDateTime createdAt;

    public enum Role {
        ADMIN,
        MEMBER
    }
}
