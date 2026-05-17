package org.backendcompas.modules.deals.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "RadarCommentResponse", description = "A comment posted on a Radar deal.")
public record RadarCommentResponseDto(

        @Schema(description = "Unique comment identifier.", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID id,

        @Schema(description = "UUID of the student who posted the comment.")
        UUID authorId,

        @Schema(description = "Comment text.", example = "Still valid! Just confirmed 10 minutes ago.")
        String content,

        @Schema(description = "When the comment was posted.", example = "2026-05-17T10:05:00")
        LocalDateTime createdAt
) {}
