package com.credentialvault.credential;

import com.credentialvault.audit.AuditLog;
import com.credentialvault.audit.AuditService;
import com.credentialvault.auth.AuthenticatedUser;
import com.credentialvault.config.ForbiddenException;
import com.credentialvault.config.NotFoundException;
import com.credentialvault.credential.dto.CredentialResponse;
import com.credentialvault.credential.dto.CreateCredentialRequest;
import com.credentialvault.credential.dto.UseCredentialResponse;
import com.credentialvault.crypto.EncryptionService;
import com.credentialvault.crypto.KeyProvider;
import com.credentialvault.team.Team;
import com.credentialvault.team.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CredentialService {

    private final CredentialRepository credentialRepo;
    private final CredentialAccessRepository accessRepo;
    private final TeamRepository teamRepo;
    private final EncryptionService encryptionService;
    private final KeyProvider keyProvider;
    private final AuditService auditService;

    public CredentialResponse create(CreateCredentialRequest req, AuthenticatedUser actor) {
        String keyRef = keyProvider.getLatestKeyReference();
        SecretKey key = keyProvider.getKey(keyRef);
        EncryptionService.EncryptedData encrypted = encryptionService.encrypt(req.password(), key);

        Credential saved = credentialRepo.save(
            Credential.builder()
                .orgId(actor.orgId())
                .name(req.name())
                .url(req.url())
                .username(req.username())
                .encryptedPassword(encrypted.ciphertext())
                .iv(encrypted.iv())
                .keyReference(keyRef)
                .createdBy(actor.userId())
                .createdAt(LocalDateTime.now())
                .build()
        );

        auditService.log(saved.getId(), actor.userId(), actor.orgId(), null, null, AuditLog.Action.CREDENTIAL_CREATED);
        return toResponse(saved);
    }

    public List<CredentialResponse> listForOrg(AuthenticatedUser actor) {
        return credentialRepo.findByOrgId(actor.orgId()).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public void delete(String credentialId, AuthenticatedUser actor) {
        // findByIdAndOrgId ensures we never delete across org boundaries
        Credential credential = credentialRepo.findByIdAndOrgId(credentialId, actor.orgId())
            .orElseThrow(() -> new NotFoundException("Credential not found"));

        accessRepo.deleteByCredentialId(credentialId);
        credentialRepo.delete(credential);
        auditService.log(credentialId, actor.userId(), actor.orgId(), null, null, AuditLog.Action.CREDENTIAL_DELETED);
    }

    public void grantAccess(String credentialId, String teamId, AuthenticatedUser actor) {
        credentialRepo.findByIdAndOrgId(credentialId, actor.orgId())
            .orElseThrow(() -> new NotFoundException("Credential not found"));

        if (!teamRepo.existsByIdAndOrgId(teamId, actor.orgId())) {
            throw new NotFoundException("Team not found");
        }

        if (accessRepo.existsByCredentialIdAndTeamId(credentialId, teamId)) {
            return; // idempotent
        }

        accessRepo.save(
            CredentialAccess.builder()
                .credentialId(credentialId)
                .teamId(teamId)
                .orgId(actor.orgId())
                .grantedBy(actor.userId())
                .grantedAt(LocalDateTime.now())
                .build()
        );

        auditService.log(credentialId, actor.userId(), actor.orgId(), teamId, null, AuditLog.Action.ACCESS_GRANTED);
    }

    public void revokeAccess(String credentialId, String teamId, AuthenticatedUser actor) {
        credentialRepo.findByIdAndOrgId(credentialId, actor.orgId())
            .orElseThrow(() -> new NotFoundException("Credential not found"));

        accessRepo.deleteByCredentialIdAndTeamId(credentialId, teamId);
        auditService.log(credentialId, actor.userId(), actor.orgId(), teamId, null, AuditLog.Action.ACCESS_REVOKED);
    }

    /**
     * Core security boundary. ALL three checks must pass:
     *   1. Valid JWT (enforced by filter before we get here)
     *   2. credential.orgId == actor.orgId
     *   3. actor belongs to a team that has been granted access (admins bypass #3)
     *
     * Always throws ForbiddenException — never 404 — to prevent credential enumeration.
     */
    public UseCredentialResponse use(String credentialId, AuthenticatedUser actor, String sourceIp) {
        // Intentionally: same exception for "not found" and "wrong org" — no info leak
        Credential credential = credentialRepo.findById(credentialId)
            .orElseThrow(ForbiddenException::new);

        if (!credential.getOrgId().equals(actor.orgId())) {
            throw new ForbiddenException();
        }

        String grantingTeamId = null;

        if (!actor.isAdmin()) {
            List<Team> userTeams = teamRepo.findByOrgIdAndMemberUserIdsContaining(actor.orgId(), actor.userId());
            if (userTeams.isEmpty()) {
                throw new ForbiddenException();
            }

            Set<String> userTeamIds = userTeams.stream().map(Team::getId).collect(Collectors.toSet());

            grantingTeamId = accessRepo
                .findFirstByCredentialIdAndTeamIdIn(credentialId, userTeamIds)
                .map(CredentialAccess::getTeamId)
                .orElseThrow(ForbiddenException::new);
        }

        SecretKey key = keyProvider.getKey(credential.getKeyReference());
        String plaintext = encryptionService.decrypt(credential.getEncryptedPassword(), credential.getIv(), key);

        // Audit AFTER successful decryption, BEFORE returning to caller
        auditService.log(credentialId, actor.userId(), actor.orgId(), grantingTeamId, sourceIp, AuditLog.Action.CREDENTIAL_USED);

        return new UseCredentialResponse(credential.getUsername(), plaintext);
    }

    private CredentialResponse toResponse(Credential c) {
        return new CredentialResponse(c.getId(), c.getName(), c.getUrl(), c.getUsername(), c.getCreatedBy(), c.getCreatedAt());
    }
}
