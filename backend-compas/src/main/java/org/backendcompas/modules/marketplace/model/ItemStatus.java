package org.backendcompas.modules.marketplace.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of a marketplace item.")
public enum ItemStatus {
    @Schema(description = "Item is visible and available for purchase.")
    ACTIVE,

    @Schema(description = "Item is temporarily reserved for a buyer.")
    RESERVED,

    @Schema(description = "Item has been sold.")
    SOLD,

    @Schema(description = "Item is not visible in the marketplace.")
    INACTIVE
}
