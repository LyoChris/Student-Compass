package org.backendcompas.modules.deals.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.backendcompas.modules.deals.model.DealStatus;
import org.backendcompas.modules.deals.model.RadarDealCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "RadarDealResponse", description = "A crowdsourced student deal shown on the Radar map.")
public record RadarDealResponseDto(

        @Schema(description = "Unique deal identifier.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "UUID of the student who reported this deal.")
        UUID reportedBy,

        @Schema(description = "Trust score of the reporting student (0–100+). Lower scores indicate less reliable reporters.", example = "82")
        int reporterTrustScore,

        @Schema(description = "Short deal title.", example = "50% off at Subway near Politehnica")
        String title,

        @Schema(description = "Extended description.", example = "Show student ID to get half-price footlong until 18:00.")
        String description,

        @Schema(description = "Thematic category.", example = "FOOD")
        RadarDealCategory category,

        @Schema(description = "GPS latitude.", example = "44.436512")
        BigDecimal latitude,

        @Schema(description = "GPS longitude.", example = "26.102431")
        BigDecimal longitude,

        @Schema(description = "When this deal expires.", example = "2026-05-17T20:00:00")
        LocalDateTime expiresAt,

        @Schema(description = "Lifecycle status of the deal.", example = "ACTIVE", allowableValues = {"ACTIVE", "EXPIRED"})
        DealStatus status,

        @Schema(description = "Net vote score (upvotes minus downvotes).", example = "7")
        int netVotes,

        @Schema(description = "Comments on this deal, sorted oldest first.")
        List<RadarCommentResponseDto> comments,

        @Schema(description = "When the deal was first reported.", example = "2026-05-17T09:30:00")
        LocalDateTime createdAt
) {}
