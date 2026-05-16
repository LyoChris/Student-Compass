package org.backendcompas.modules.deals.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RadarCommentRequest", description = "Payload for posting a comment on a Radar deal.")
public record RadarCommentRequestDto(

        @NotBlank
        @Size(max = 300)
        @Schema(description = "Comment text (max 300 characters).", example = "Still valid! Just confirmed 10 minutes ago.", requiredMode = Schema.RequiredMode.REQUIRED)
        String content
) {}
