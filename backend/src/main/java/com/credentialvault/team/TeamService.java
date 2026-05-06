package com.credentialvault.team;

import com.credentialvault.auth.AuthenticatedUser;
import com.credentialvault.auth.UserRepository;
import com.credentialvault.config.ForbiddenException;
import com.credentialvault.config.NotFoundException;
import com.credentialvault.team.dto.CreateTeamRequest;
import com.credentialvault.team.dto.TeamResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepo;
    private final UserRepository userRepo;

    public TeamResponse create(CreateTeamRequest req, AuthenticatedUser actor) {
        Team saved = teamRepo.save(
            Team.builder()
                .orgId(actor.orgId())
                .name(req.name())
                .memberUserIds(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .build()
        );
        return toResponse(saved);
    }

    public List<TeamResponse> listForOrg(AuthenticatedUser actor) {
        return teamRepo.findByOrgId(actor.orgId()).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public TeamResponse addMember(String teamId, String userId, AuthenticatedUser actor) {
        Team team = getTeamInOrg(teamId, actor.orgId());

        // Verify the target user belongs to the same org
        userRepo.findById(userId)
            .filter(u -> u.getOrgId().equals(actor.orgId()))
            .orElseThrow(() -> new NotFoundException("User not found in org"));

        if (!team.getMemberUserIds().contains(userId)) {
            team.getMemberUserIds().add(userId);
            teamRepo.save(team);
        }
        return toResponse(team);
    }

    public void removeMember(String teamId, String userId, AuthenticatedUser actor) {
        Team team = getTeamInOrg(teamId, actor.orgId());
        team.getMemberUserIds().remove(userId);
        teamRepo.save(team);
    }

    private Team getTeamInOrg(String teamId, String orgId) {
        Team team = teamRepo.findById(teamId)
            .orElseThrow(() -> new NotFoundException("Team not found"));
        if (!team.getOrgId().equals(orgId)) {
            throw new ForbiddenException();
        }
        return team;
    }

    private TeamResponse toResponse(Team t) {
        return new TeamResponse(t.getId(), t.getName(), t.getMemberUserIds(), t.getCreatedAt());
    }
}
