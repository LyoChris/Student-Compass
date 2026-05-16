package org.backendcompas.modules.deals.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.backendcompas.modules.deals.model.VoteType;

@Schema(name = "VoteRequest", description = "Payload for casting an UPVOTE or DOWNVOTE on a Radar deal.")
public record VoteRequestDto(

        @NotNull
        @Schema(description = "Vote direction.", example = "UPVOTE", allowableValues = {"UPVOTE", "DOWNVOTE"}, requiredMode = Schema.RequiredMode.REQUIRED)
        VoteType voteType
) {}
