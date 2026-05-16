package org.backendcompas.modules.deals.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.backendcompas.core.security.CustomUserDetails;
import org.backendcompas.modules.deals.dto.RadarCommentRequestDto;
import org.backendcompas.modules.deals.dto.RadarCommentResponseDto;
import org.backendcompas.modules.deals.dto.RadarDealCreateRequestDto;
import org.backendcompas.modules.deals.dto.RadarDealResponseDto;
import org.backendcompas.modules.deals.dto.VoteRequestDto;
import org.backendcompas.modules.deals.service.RadarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/radar")
@SecurityRequirement(name = "BearerAuth")
@Tag(
        name = "StuFi Radar",
        description = """
                Crowdsourced, geolocation-based student deals map.

                Students report short-lived deals (discounts, free events, limited offers) pinned
                to GPS coordinates. Every deal has a TTL (`expiresAt`) and a community vote score.
                Repeated downvotes auto-expire a deal and penalise the reporter's **Trust Score**.
                Upvotes reward the reporter. Students whose trust score falls below 30 are **muted**
                and can no longer post deals or comments.
                """
)
public class RadarController {

    private final RadarService radarService;

    public RadarController(RadarService radarService) {
        this.radarService = radarService;
    }

    // ── POST /api/v1/radar/deals ──────────────────────────────────────────────

    @PostMapping("/deals")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Report a new deal",
            description = """
                    Reports a new crowdsourced deal on the Radar map.

                    **Mute guard**: callers with `trust_score < 30` receive `403 Forbidden`.

                    **TTL**: `expiresAt` must be a future timestamp. Expired deals are excluded
                    from the active feed automatically.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Deal reported successfully.",
                    content = @Content(schema = @Schema(implementation = RadarDealResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation error (blank title, past expiresAt, invalid coordinates, etc.).",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiError"))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiError"))),
            @ApiResponse(responseCode = "403", description = "Caller is muted (trust score < 30).",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiError")))
    })
    public ResponseEntity<RadarDealResponseDto> createDeal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody RadarDealCreateRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(radarService.createDeal(userDetails.getUserId(), request));
    }

    // ── GET /api/v1/radar/deals ───────────────────────────────────────────────

    @GetMapping("/deals")
    @Operation(
            summary = "List active deals",
            description = """
                    Returns all ACTIVE deals whose `expiresAt` is still in the future.

                    **Geofencing**: supply all three of `lat`, `lng`, and `radiusKm` to restrict
                    results to the given radius (Haversine great-circle distance). Omit all three
                    to receive the full global active feed.

                    Deals are returned in insertion order (newest first from the DB index).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active deals returned.",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RadarDealResponseDto.class)))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiError")))
    })
    public ResponseEntity<List<RadarDealResponseDto>> getActiveDeals(
            @Parameter(description = "Centre latitude for radius filtering (-90 to 90).", example = "44.436512")
            @RequestParam(required = false) Double lat,
            @Parameter(description = "Centre longitude for radius filtering (-180 to 180).", example = "26.102431")
            @RequestParam(required = false) Double lng,
            @Parameter(description = "Search radius in kilometres.", example = "2.5")
            @RequestParam(required = false) Double radiusKm
    ) {
        return ResponseEntity.ok(radarService.getActiveDeals(lat, lng, radiusKm));
    }

    // ── GET /api/v1/radar/deals/{dealId} ─────────────────────────────────────

    @GetMapping("/deals/{dealId}")
    @Operation(
            summary = "Get deal details",
            description = "Returns full details of a single deal including all comments. Works for both ACTIVE and EXPIRED deals."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deal found.",
                    content = @Content(schema = @Schema(implementation = RadarDealResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiError"))),
            @ApiResponse(responseCode = "404", description = "Deal not found.",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiError")))
    })
    public ResponseEntity<RadarDealResponseDto> getDeal(
            @Parameter(description = "UUID of the deal.", required = true)
            @PathVariable UUID dealId
    ) {
        return ResponseEntity.ok(radarService.getDeal(dealId));
    }

    // ── POST /api/v1/radar/deals/{dealId}/vote ────────────────────────────────

    @PostMapping("/deals/{dealId}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Vote on a deal",
            description = """
                    Casts an UPVOTE or DOWNVOTE on a deal. Each student may vote exactly once per deal.

                    **Karma engine**:
                    - `UPVOTE` → deal reporter's `trust_score` +2
                    - `DOWNVOTE` → deal reporter's `trust_score` −2 (floor 0)

                    **Auto-expire**: if the deal's net vote score (upvotes − downvotes) drops to ≤ −5,
                    its status is automatically set to `EXPIRED`.

                    Concurrent votes are serialised with pessimistic row-level locking.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vote recorded."),
            @ApiResponse(responseCode = "400", description = "Missing or invalid voteType.",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiError"))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiError"))),
            @ApiResponse(responseCode = "404", description = "Deal not found.",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiError"))),
            @ApiResponse(responseCode = "409", description = "Caller has already voted on this deal.",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiError")))
    })
    public ResponseEntity<Void> voteOnDeal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "UUID of the deal to vote on.", required = true)
            @PathVariable UUID dealId,
            @Valid @RequestBody VoteRequestDto request
    ) {
        radarService.voteOnDeal(userDetails.getUserId(), dealId, request);
        return ResponseEntity.noContent().build();
    }

    // ── POST /api/v1/radar/deals/{dealId}/comments ────────────────────────────

    @PostMapping("/deals/{dealId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Post a comment",
            description = """
                    Posts a comment on a deal. Comments are returned sorted oldest-first in deal responses.

                    **Mute guard**: callers with `trust_score < 30` receive `403 Forbidden`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment posted.",
                    content = @Content(schema = @Schema(implementation = RadarCommentResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Blank or oversized content.",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiError"))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiError"))),
            @ApiResponse(responseCode = "403", description = "Caller is muted (trust score < 30).",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiError"))),
            @ApiResponse(responseCode = "404", description = "Deal not found.",
                    content = @Content(schema = @Schema(ref = "#/components/schemas/ApiError")))
    })
    public ResponseEntity<RadarCommentResponseDto> addComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "UUID of the deal to comment on.", required = true)
            @PathVariable UUID dealId,
            @Valid @RequestBody RadarCommentRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(radarService.addComment(userDetails.getUserId(), dealId, request));
    }
}
