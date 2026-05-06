package com.credentialvault.auth;

import com.credentialvault.auth.dto.LoginRequest;
import com.credentialvault.auth.dto.LoginResponse;
import com.credentialvault.auth.dto.RegisterRequest;
import com.credentialvault.auth.dto.RegisterResponse;
import com.credentialvault.model.Organization;
import com.credentialvault.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Organization org = organizationRepository.save(
            Organization.builder()
                .name(request.organizationName())
                .createdAt(LocalDateTime.now())
                .build()
        );

        User user = userRepository.save(
            User.builder()
                .orgId(org.getId())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(User.Role.ADMIN)   // first user of an org is always admin
                .createdAt(LocalDateTime.now())
                .build()
        );

        String token = jwtService.generateToken(user);
        return new RegisterResponse(user.getId(), org.getId(), token);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return new LoginResponse(jwtService.generateToken(user));
    }
}
