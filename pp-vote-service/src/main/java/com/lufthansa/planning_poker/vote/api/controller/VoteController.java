package com.lufthansa.planning_poker.vote.api.controller;

import com.lufthansa.planning_poker.vote.application.dto.request.CastVoteRequest;
import com.lufthansa.planning_poker.vote.application.dto.response.VoteResponse;
import com.lufthansa.planning_poker.vote.application.dto.response.VoteResultsResponse;
import com.lufthansa.planning_poker.vote.application.service.VoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Votes", description = "Voting endpoints")
public class VoteController {

    private final VoteService voteService;

    @PostMapping("/votes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cast a vote", description = "Cast or update your vote for a story")
    public VoteResponse castVote(
            @Valid @RequestBody CastVoteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return voteService.castVote(
            request,
            jwt.getSubject(),
            jwt.getClaimAsString("preferred_username")
        );
    }

    @GetMapping("/votes/stories/{storyId}/my-vote")
    @Operation(summary = "Get my vote for a story")
    public VoteResponse getMyVote(
            @PathVariable UUID storyId,
            @AuthenticationPrincipal Jwt jwt) {
        return voteService.getMyVote(storyId, jwt.getSubject());
    }

    @GetMapping("/votes/stories/{storyId}/count")
    @Operation(summary = "Get vote count for a story")
    public int getVoteCount(@PathVariable UUID storyId) {
        return voteService.getVoteCount(storyId);
    }

    @PostMapping("/voting/stories/{storyId}/reveal")
    @Operation(summary = "Reveal votes", description = "Reveal all votes for a story (moderator only)")
    public VoteResultsResponse revealVotes(
            @PathVariable UUID storyId,
            @RequestParam UUID roomId,
            @AuthenticationPrincipal Jwt jwt) {
        return voteService.revealVotes(
            storyId,
            roomId,
            jwt.getSubject(),
            jwt.getClaimAsString("preferred_username")
        );
    }

    @PostMapping("/voting/stories/{storyId}/finish")
    @Operation(summary = "Finish voting", description = "Finish voting and lock results (moderator only)")
    public VoteResultsResponse finishVoting(
            @PathVariable UUID storyId,
            @RequestParam UUID roomId,
            @RequestParam String finalEstimate,
            @RequestParam(required = false) String storyTitle,
            @AuthenticationPrincipal Jwt jwt) {
        return voteService.finishVoting(
            storyId,
            roomId,
            finalEstimate,
            storyTitle != null ? storyTitle : "Story",
            jwt.getSubject(),
            jwt.getClaimAsString("preferred_username")
        );
    }

    @PostMapping("/voting/stories/{storyId}/reset")
    @Operation(summary = "Reset votes", description = "Clear all votes for a story (moderator only)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetVotes(
            @PathVariable UUID storyId,
            @RequestParam UUID roomId) {
        voteService.resetVotes(storyId, roomId);
    }
}
