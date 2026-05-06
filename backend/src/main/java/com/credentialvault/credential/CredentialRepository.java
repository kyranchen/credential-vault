package com.credentialvault.credential;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CredentialRepository extends MongoRepository<Credential, String> {

    List<Credential> findByOrgId(String orgId);

    // Used to verify org ownership before deletion/update
    Optional<Credential> findByIdAndOrgId(String id, String orgId);
}
