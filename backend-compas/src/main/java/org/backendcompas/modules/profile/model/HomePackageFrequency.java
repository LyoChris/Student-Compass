package org.backendcompas.modules.profile.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = """
                How often the student receives a care package from home:
                - `WEEKLY` — Every week
                - `BI_WEEKLY` — Every two weeks
                - `MONTHLY` — Once a month
                - `NONE` — Does not receive packages from home
                """
)
public enum HomePackageFrequency {
    WEEKLY,
    BI_WEEKLY,
    MONTHLY,
    NONE
}
