package org.backendcompas.modules.marketplace.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Supported marketplace item categories.")
public enum ItemCategory {
    @Schema(description = "Books, notes, and course materials.")
    BOOKS_NOTES,

    @Schema(description = "Electronics such as laptops, calculators, or accessories.")
    ELECTRONICS,

    @Schema(description = "Dorm appliances like kettles, lamps, or mini fridges.")
    DORM_APPLIANCES,

    @Schema(description = "Clothing and accessories.")
    CLOTHING,

    @Schema(description = "Other items not covered by specific categories.")
    OTHER
}
