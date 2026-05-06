package com.credentialvault.credential;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CredentialAccessRepository extends MongoRepository<CredentialAccess, String> {

    List<CredentialAccess> findByCredentialId(String credentialId);

    // Core permission check: does this credential have a grant for any of the user's teams?
    Optional<CredentialAccess> findFirstByCredentialIdAndTeamIdIn(String credentialId, Collection<String> teamIds);

    boolean existsByCredentialIdAndTeamId(String credentialId, String teamId);

    void deleteByCredentialIdAndTeamId(String credentialId, String teamId);

    void deleteByCredentialId(String credentialId);
}
