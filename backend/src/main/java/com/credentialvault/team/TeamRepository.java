package com.credentialvault.team;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TeamRepository extends MongoRepository<Team, String> {

    List<Team> findByOrgId(String orgId);

    // Used in permission check: find all teams in an org that contain a given user
    List<Team> findByOrgIdAndMemberUserIdsContaining(String orgId, String userId);

    boolean existsByIdAndOrgId(String id, String orgId);
}
