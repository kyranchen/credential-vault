package com.credentialvault.integration;

import com.credentialvault.audit.AuditLog;
import com.credentialvault.audit.AuditRepository;
import com.credentialvault.auth.JwtService;
import com.credentialvault.auth.OrganizationRepository;
import com.credentialvault.auth.UserRepository;
import com.credentialvault.credential.Credential;
import com.credentialvault.credential.CredentialAccess;
import com.credentialvault.credential.CredentialAccessRepository;
import com.credentialvault.credential.CredentialRepository;
import com.credentialvault.crypto.EncryptionService;
import com.credentialvault.crypto.KeyProvider;
import com.credentialvault.model.Organization;
import com.credentialvault.model.User;
import com.credentialvault.team.Team;
import com.credentialvault.team.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class CredentialAccessIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0");

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepo;
    @Autowired OrganizationRepository orgRepo;
    @Autowired TeamRepository teamRepo;
    @Autowired CredentialRepository credentialRepo;
    @Autowired CredentialAccessRepository accessRepo;
    @Autowired AuditRepository auditRepo;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired EncryptionService encryptionService;
    @Autowired KeyProvider keyProvider;

    // Org A fixtures
    private Organization orgA;
    private User adminA;
    private User memberTeamA;
    private User memberTeamB;
    private Team teamA;
    private Team teamB;
    private Credential credGrantedToTeamA;

    // Org B fixtures
    private Organization orgB;
    private User memberOrgB;

    @BeforeEach
    void setUp() {
        userRepo.deleteAll();
        orgRepo.deleteAll();
        teamRepo.deleteAll();
        credentialRepo.deleteAll();
        accessRepo.deleteAll();
        auditRepo.deleteAll();

        orgA = orgRepo.save(org("Org A"));
        orgB = orgRepo.save(org("Org B"));

        adminA = userRepo.save(user(orgA.getId(), "admin@a.com", User.Role.ADMIN));
        memberTeamA = userRepo.save(user(orgA.getId(), "member-a@a.com", User.Role.MEMBER));
        memberTeamB = userRepo.save(user(orgA.getId(), "member-b@a.com", User.Role.MEMBER));
        memberOrgB = userRepo.save(user(orgB.getId(), "member@b.com", User.Role.MEMBER));

        teamA = teamRepo.save(team(orgA.getId(), "Team A", List.of(memberTeamA.getId())));
        teamB = teamRepo.save(team(orgA.getId(), "Team B", List.of(memberTeamB.getId())));

        credGrantedToTeamA = credentialRepo.save(encryptedCredential(orgA.getId(), "Company Twitter"));
        accessRepo.save(access(credGrantedToTeamA.getId(), teamA.getId(), orgA.getId(), adminA.getId()));
    }

    // ── Permission boundary tests ─────────────────────────────────────────────

    @Test
    void teamA_member_can_use_credential_granted_to_teamA() throws Exception {
        mockMvc.perform(get("/api/credentials/{id}/use", credGrantedToTeamA.getId())
                .header("Authorization", "Bearer " + tokenFor(memberTeamA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("acme_twitter"))
            .andExpect(jsonPath("$.password").value("plaintext-password"));
    }

    @Test
    void teamB_member_cannot_use_credential_granted_only_to_teamA() throws Exception {
        mockMvc.perform(get("/api/credentials/{id}/use", credGrantedToTeamA.getId())
                .header("Authorization", "Bearer " + tokenFor(memberTeamB)))
            .andExpect(status().isForbidden());
    }

    @Test
    void admin_can_use_any_credential_in_their_org() throws Exception {
        mockMvc.perform(get("/api/credentials/{id}/use", credGrantedToTeamA.getId())
                .header("Authorization", "Bearer " + tokenFor(adminA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.password").value("plaintext-password"));
    }

    @Test
    void cross_org_access_is_forbidden() throws Exception {
        // memberOrgB has a valid JWT but targets a credential belonging to orgA
        mockMvc.perform(get("/api/credentials/{id}/use", credGrantedToTeamA.getId())
                .header("Authorization", "Bearer " + tokenFor(memberOrgB)))
            .andExpect(status().isForbidden());
    }

    @Test
    void cross_org_access_returns_403_not_404() throws Exception {
        // Even though the credential exists, org mismatch must yield 403, not 404
        mockMvc.perform(get("/api/credentials/{id}/use", credGrantedToTeamA.getId())
                .header("Authorization", "Bearer " + tokenFor(memberOrgB)))
            .andExpect(status().isForbidden())
            .andExpect(status().is(403));
    }

    @Test
    void unauthenticated_request_is_rejected() throws Exception {
        mockMvc.perform(get("/api/credentials/{id}/use", credGrantedToTeamA.getId()))
            .andExpect(status().isForbidden());
    }

    @Test
    void nonexistent_credential_returns_403_not_404() throws Exception {
        // Prevents enumeration: attacker cannot determine whether an ID exists
        mockMvc.perform(get("/api/credentials/{id}/use", "nonexistent-id")
                .header("Authorization", "Bearer " + tokenFor(memberTeamA)))
            .andExpect(status().isForbidden())
            .andExpect(status().is(403));
    }

    // ── Audit log tests ───────────────────────────────────────────────────────

    @Test
    void successful_credential_use_writes_audit_entry() throws Exception {
        mockMvc.perform(get("/api/credentials/{id}/use", credGrantedToTeamA.getId())
                .header("Authorization", "Bearer " + tokenFor(memberTeamA)))
            .andExpect(status().isOk());

        List<AuditLog> logs = auditRepo.findAll();
        assertThat(logs).hasSize(1);
        AuditLog entry = logs.get(0);
        assertThat(entry.getAction()).isEqualTo(AuditLog.Action.CREDENTIAL_USED);
        assertThat(entry.getAccessedBy()).isEqualTo(memberTeamA.getId());
        assertThat(entry.getCredentialId()).isEqualTo(credGrantedToTeamA.getId());
        assertThat(entry.getTeamId()).isEqualTo(teamA.getId());
    }

    @Test
    void failed_access_does_not_write_audit_entry() throws Exception {
        mockMvc.perform(get("/api/credentials/{id}/use", credGrantedToTeamA.getId())
                .header("Authorization", "Bearer " + tokenFor(memberTeamB)))
            .andExpect(status().isForbidden());

        assertThat(auditRepo.count()).isZero();
    }

    // ── Admin management boundary tests ───────────────────────────────────────

    @Test
    void member_cannot_create_credential() throws Exception {
        mockMvc.perform(post("/api/credentials")
                .header("Authorization", "Bearer " + tokenFor(memberTeamA))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Test","url":"https://test.com","username":"u","password":"p"}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void admin_from_orgA_cannot_see_orgB_credentials() throws Exception {
        // Create a credential for orgB
        Credential orgBCred = credentialRepo.save(encryptedCredential(orgB.getId(), "Org B Slack"));
        User adminB = userRepo.save(user(orgB.getId(), "admin@b.com", User.Role.ADMIN));

        // Org A admin lists credentials — must not see orgB's credential
        String response = mockMvc.perform(get("/api/credentials")
                .header("Authorization", "Bearer " + tokenFor(adminA)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(response).doesNotContain(orgBCred.getId());
        assertThat(response).doesNotContain("Org B Slack");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String tokenFor(User user) {
        return jwtService.generateToken(user);
    }

    private Organization org(String name) {
        return Organization.builder().name(name).createdAt(LocalDateTime.now()).build();
    }

    private User user(String orgId, String email, User.Role role) {
        return User.builder()
            .orgId(orgId)
            .email(email)
            .passwordHash(passwordEncoder.encode("password"))
            .role(role)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private Team team(String orgId, String name, List<String> memberIds) {
        return Team.builder()
            .orgId(orgId)
            .name(name)
            .memberUserIds(memberIds)
            .createdAt(LocalDateTime.now())
            .build();
    }

    private Credential encryptedCredential(String orgId, String name) {
        String keyRef = keyProvider.getLatestKeyReference();
        EncryptionService.EncryptedData enc = encryptionService.encrypt("plaintext-password", keyProvider.getKey(keyRef));
        return Credential.builder()
            .orgId(orgId)
            .name(name)
            .url("https://twitter.com")
            .username("acme_twitter")
            .encryptedPassword(enc.ciphertext())
            .iv(enc.iv())
            .keyReference(keyRef)
            .createdBy("system")
            .createdAt(LocalDateTime.now())
            .build();
    }

    private CredentialAccess access(String credentialId, String teamId, String orgId, String grantedBy) {
        return CredentialAccess.builder()
            .credentialId(credentialId)
            .teamId(teamId)
            .orgId(orgId)
            .grantedBy(grantedBy)
            .grantedAt(LocalDateTime.now())
            .build();
    }
}
