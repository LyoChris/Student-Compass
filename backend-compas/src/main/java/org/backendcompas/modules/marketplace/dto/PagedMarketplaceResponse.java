package org.backendcompas.modules.marketplace.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PagedMarketplaceResponse", description = "Paginated marketplace response wrapper returned by the feed endpoint.")
public class PagedMarketplaceResponse {

    @ArraySchema(
        schema = @Schema(implementation = ItemResponseDto.class),
        arraySchema = @Schema(description = "Marketplace listings for the requested page. Boosted listings are ordered first.")
    )
    private List<ItemResponseDto> content;

    @Schema(description = "Zero-based page number returned by the API.", example = "0")
    private int pageNumber;

    @Schema(description = "Maximum number of marketplace listings requested for this page.", example = "20")
    private int pageSize;

    @Schema(description = "Total number of listings matching the filters.", example = "47")
    private long totalElements;

    @Schema(description = "Total number of pages available for the current filters.", example = "3")
    private int totalPages;

    @Schema(description = "Whether this page is the last available page.", example = "false")
    private boolean isLast;
}
