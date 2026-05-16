package org.backendcompas.modules.deals.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.backendcompas.modules.deals.model.RadarDealCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "RadarDealCreateRequest", description = "Payload for reporting a new student deal on the Radar map.")
public record RadarDealCreateRequestDto(

        @NotBlank
        @Size(max = 120)
        @Schema(description = "Short title visible on the map pin.", example = "50% off at Subway near Politehnica", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Size(max = 500)
        @Schema(description = "Optional extended description.", example = "Show student ID to get half-price footlong until 18:00.")
        String description,

        @NotNull
        @Schema(description = "Thematic category of the deal.", example = "FOOD", allowableValues = {"FOOD", "HOME", "SOCIAL", "TECH", "OTHER"}, requiredMode = Schema.RequiredMode.REQUIRED)
        RadarDealCategory category,

        @NotNull
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        @Schema(description = "GPS latitude of the deal location.", example = "44.436512", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal latitude,

        @NotNull
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        @Schema(description = "GPS longitude of the deal location.", example = "26.102431", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal longitude,

        @NotNull
        @Future
        @Schema(description = "When this deal expires (ISO-8601 datetime). Must be a future timestamp.", example = "2026-05-17T20:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime expiresAt
) {}
