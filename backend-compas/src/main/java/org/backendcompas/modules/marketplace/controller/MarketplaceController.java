package org.backendcompas.modules.marketplace.controller;

import java.math.BigDecimal;
import java.util.UUID;

import org.backendcompas.core.security.CustomUserDetails;
import org.backendcompas.modules.marketplace.dto.CreateItemRequestDto;
import org.backendcompas.modules.marketplace.dto.ItemResponseDto;
import org.backendcompas.modules.marketplace.dto.PagedMarketplaceResponse;
import org.backendcompas.modules.marketplace.dto.UpdateItemRequestDto;
import org.backendcompas.modules.marketplace.model.ItemCategory;
import org.backendcompas.modules.marketplace.model.ItemCondition;
import org.backendcompas.modules.marketplace.model.ItemStatus;
import org.backendcompas.modules.marketplace.service.MarketplaceService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping(value = "/api/v1/marketplace", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(
    name = "Marketplace",
    description = "Student marketplace listings with strict enum-backed categories, conditions, statuses, dynamic filtering, pagination, and boosted-first sorting."
)
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    public MarketplaceController(MarketplaceService marketplaceService) {
        this.marketplaceService = marketplaceService;
    }

    @Operation(
        summary = "Create a marketplace listing",
        description = "Creates a new student marketplace listing. The backend assigns status ACTIVE and isBoosted false regardless of client input."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Listing created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ItemResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation error in the create payload",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ItemResponseDto> createItem(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Marketplace listing details supplied by the student seller.",
            content = @Content(
                schema = @Schema(implementation = CreateItemRequestDto.class),
                examples = @ExampleObject(
                    name = "Math notes listing",
                    value = """
                        {
                          "sellerId": "0fcb4ce8-9a62-4f4f-8a28-76c5c5e8d4e3",
                          "title": "Math 1 Course Notes",
                          "description": "Complete Math 1 lecture notes with solved seminar exercises and exam recap pages.",
                          "price": 35.00,
                          "category": "BOOKS_NOTES",
                          "itemCondition": "GOOD",
                          "tags": ["math", "year-1", "exam-prep"],
                          "imageUrls": ["https://res.cloudinary.com/stufi/image/upload/v1715862000/marketplace/math-notes-cover.jpg"]
                        }
                        """
                )
            )
        )
        @Valid @RequestBody CreateItemRequestDto request
    ) {
        ItemResponseDto response = marketplaceService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Search marketplace listings",
        description = "Returns ACTIVE marketplace listings using optional Criteria API filters. The primary sort is always isBoosted DESC, so boosted items stay on top. Client sort parameters are applied only as secondary ordering.",
        parameters = {
            @Parameter(name = "page", in = ParameterIn.QUERY, description = "Zero-based page index.", example = "0", schema = @Schema(type = "integer", minimum = "0")),
            @Parameter(name = "size", in = ParameterIn.QUERY, description = "Page size requested by the client.", example = "20", schema = @Schema(type = "integer", minimum = "1")),
            @Parameter(name = "sort", in = ParameterIn.QUERY, description = "Secondary sort applied after isBoosted DESC. Example values: price,asc or createdAt,desc.", example = "price,asc", schema = @Schema(type = "string"))
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listings fetched successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedMarketplaceResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid filter, enum, pagination, or sort parameter",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping
    public ResponseEntity<PagedMarketplaceResponse> searchActiveItems(
        @Parameter(
            name = "search",
            in = ParameterIn.QUERY,
            description = "Optional case-insensitive partial match against listing title or description.",
            example = "math notes",
            schema = @Schema(type = "string", maxLength = 100)
        )
        @Size(max = 100) @RequestParam(required = false) String search,

        @Parameter(
            name = "category",
            in = ParameterIn.QUERY,
            description = "Optional strict category filter.",
            example = "BOOKS_NOTES",
            schema = @Schema(implementation = ItemCategory.class, allowableValues = {"BOOKS_NOTES", "ELECTRONICS", "DORM_APPLIANCES", "CLOTHING", "OTHER"})
        )
        @RequestParam(required = false) ItemCategory category,

        @Parameter(
            name = "condition",
            in = ParameterIn.QUERY,
            description = "Optional strict item condition filter.",
            example = "GOOD",
            schema = @Schema(implementation = ItemCondition.class, allowableValues = {"NEW", "LIKE_NEW", "GOOD", "FAIR"})
        )
        @RequestParam(required = false) ItemCondition condition,

        @Parameter(
            name = "minPrice",
            in = ParameterIn.QUERY,
            description = "Optional minimum price in RON, inclusive.",
            example = "20.00",
            schema = @Schema(type = "number", format = "decimal", minimum = "0")
        )
        @PositiveOrZero @RequestParam(required = false) BigDecimal minPrice,

        @Parameter(
            name = "maxPrice",
            in = ParameterIn.QUERY,
            description = "Optional maximum price in RON, inclusive.",
            example = "200.00",
            schema = @Schema(type = "number", format = "decimal", minimum = "0")
        )
        @PositiveOrZero @RequestParam(required = false) BigDecimal maxPrice,

        @ParameterObject
        @PageableDefault(size = 20)
        Pageable pageable
    ) {
        return ResponseEntity.ok(
            marketplaceService.searchActiveItems(search, category, condition, minPrice, maxPrice, pageable)
        );
    }

    @Operation(
        summary = "Get marketplace listing details",
        description = "Fetches a single marketplace listing by UUID, including tags, Cloudinary image URLs, enum values, boost state, and audit timestamps."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listing found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ItemResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Listing not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponseDto> getItem(
        @Parameter(
            name = "id",
            in = ParameterIn.PATH,
            description = "Marketplace item UUID.",
            required = true,
            example = "9f6c0c59-8e42-4d55-9f40-8c1f7b4e9b34",
            schema = @Schema(type = "string", format = "uuid")
        )
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(marketplaceService.getItem(id));
    }

    @Operation(
        summary = "Get my listings",
        description = """
            Returns all listings owned by the authenticated user, regardless of status (ACTIVE, RESERVED, SOLD, INACTIVE).
            Results are ordered newest-first. Supports pagination via `page` and `size` query parameters.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listings fetched successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PagedMarketplaceResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @GetMapping("/me")
    public ResponseEntity<PagedMarketplaceResponse> getMyItems(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @ParameterObject @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(marketplaceService.getMyItems(userDetails.getUserId(), pageable));
    }

    @Operation(
        summary = "Update a marketplace listing",
        description = """
            Partially updates mutable listing fields. Null fields are ignored; provided tags or imageUrls replace the existing collection.

            **Ownership required:** The authenticated user must be the seller of this listing. Returns 403 if not.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listing updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ItemResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Validation error in the update payload",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "403", description = "Authenticated user is not the owner of this listing",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "404", description = "Listing not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ItemResponseDto> updateItem(
        @Parameter(
            name = "id",
            in = ParameterIn.PATH,
            description = "Marketplace item UUID.",
            required = true,
            example = "9f6c0c59-8e42-4d55-9f40-8c1f7b4e9b34",
            schema = @Schema(type = "string", format = "uuid")
        )
        @PathVariable UUID id,

        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Fields to update on the marketplace listing.",
            content = @Content(
                schema = @Schema(implementation = UpdateItemRequestDto.class),
                examples = @ExampleObject(
                    name = "Update price and photos",
                    value = """
                        {
                          "title": "Math 1 Course Notes + Exam Recap",
                          "price": 40.00,
                          "itemCondition": "LIKE_NEW",
                          "imageUrls": ["https://res.cloudinary.com/stufi/image/upload/v1715862100/marketplace/math-notes-page-1.jpg"]
                        }
                        """
                )
            )
        )
        @Valid @RequestBody UpdateItemRequestDto request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(marketplaceService.updateItem(id, request, userDetails.getUserId()));
    }

    @Operation(
        summary = "Change marketplace listing status",
        description = """
            Changes the lifecycle status of a listing using the strict ItemStatus enum. Use RESERVED for an item held for a buyer, SOLD after purchase, INACTIVE to hide it, or ACTIVE to relist it.

            **Ownership required:** The authenticated user must be the seller of this listing. Returns 403 if not.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status changed successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ItemResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status enum value",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "403", description = "Authenticated user is not the owner of this listing",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "404", description = "Listing not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ItemResponseDto> changeStatus(
        @Parameter(
            name = "id",
            in = ParameterIn.PATH,
            description = "Marketplace item UUID.",
            required = true,
            example = "9f6c0c59-8e42-4d55-9f40-8c1f7b4e9b34",
            schema = @Schema(type = "string", format = "uuid")
        )
        @PathVariable UUID id,

        @Parameter(
            name = "status",
            in = ParameterIn.QUERY,
            description = "New strict lifecycle status.",
            required = true,
            example = "RESERVED",
            schema = @Schema(implementation = ItemStatus.class, allowableValues = {"ACTIVE", "RESERVED", "SOLD", "INACTIVE"})
        )
        @RequestParam ItemStatus status,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(marketplaceService.changeStatus(id, status, userDetails.getUserId()));
    }

    @Operation(
        summary = "Delete a marketplace listing",
        description = """
            Permanently removes a listing and all its associated tags and image URLs from the database (hard delete — ON DELETE CASCADE handles child rows).

            **Ownership required:** The authenticated user must be the seller of this listing.
            Attempting to delete another user's listing returns `403 Forbidden`.
            There is no soft-delete or recovery path — the record is gone immediately.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Listing permanently deleted. No response body is returned.",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "JWT token is missing, expired, or malformed.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"status":401,"error":"Unauthorized","message":"Full authentication is required to access this resource"}
                    """))),
        @ApiResponse(responseCode = "403", description = "The authenticated user is not the seller of this listing.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"status":403,"error":"Forbidden","message":"You are not the owner of this listing"}
                    """))),
        @ApiResponse(responseCode = "404", description = "No listing exists with the supplied UUID.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = """
                    {"status":404,"error":"Not Found","message":"Marketplace item not found"}
                    """)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteItem(
        @Parameter(
            name = "id",
            in = ParameterIn.PATH,
            description = "UUID of the marketplace listing to delete.",
            required = true,
            example = "9f6c0c59-8e42-4d55-9f40-8c1f7b4e9b34",
            schema = @Schema(type = "string", format = "uuid")
        )
        @PathVariable UUID id,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        marketplaceService.deleteItem(id, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Boost a marketplace listing",
        description = """
            Marks a listing as boosted after a premium visibility action. Boosted listings are always sorted before non-boosted listings in the GET marketplace feed.

            **Ownership required:** The authenticated user must be the seller of this listing. Returns 403 if not.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listing boosted successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ItemResponseDto.class))),
        @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "403", description = "Authenticated user is not the owner of this listing",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
        @ApiResponse(responseCode = "404", description = "Listing not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    })
    @PatchMapping("/{id}/boost")
    public ResponseEntity<ItemResponseDto> boostItem(
        @Parameter(
            name = "id",
            in = ParameterIn.PATH,
            description = "Marketplace item UUID.",
            required = true,
            example = "9f6c0c59-8e42-4d55-9f40-8c1f7b4e9b34",
            schema = @Schema(type = "string", format = "uuid")
        )
        @PathVariable UUID id,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(marketplaceService.boostItem(id, userDetails.getUserId()));
    }
}
