package com.credentialvault.team;

import com.credentialvault.auth.AuthenticatedUser;
import com.credentialvault.team.dto.AddMemberRequest;
import com.credentialvault.team.dto.CreateTeamRequest;
import com.credentialvault.team.dto.TeamResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TeamResponse create(
        @RequestBody @Valid CreateTeamRequest request,
        @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return teamService.create(request, actor);
    }

    @GetMapping
    public List<TeamResponse> list(@AuthenticationPrincipal AuthenticatedUser actor) {
        return teamService.listForOrg(actor);
    }

    @PostMapping("/{id}/members")
    public TeamResponse addMember(
        @PathVariable String id,
        @RequestBody @Valid AddMemberRequest request,
        @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        return teamService.addMember(id, request.userId(), actor);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
        @PathVariable String id,
        @PathVariable String userId,
        @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        teamService.removeMember(id, userId, actor);
    }
}
