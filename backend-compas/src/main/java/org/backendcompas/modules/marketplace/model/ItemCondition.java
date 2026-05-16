package org.backendcompas.modules.marketplace.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Condition of a marketplace item.")
public enum ItemCondition {
    @Schema(description = "Unused or unopened item.")
    NEW,

    @Schema(description = "Used but looks nearly new.")
    LIKE_NEW,

    @Schema(description = "Used with normal wear.")
    GOOD,

    @Schema(description = "Heavily used with visible wear.")
    FAIR
}
