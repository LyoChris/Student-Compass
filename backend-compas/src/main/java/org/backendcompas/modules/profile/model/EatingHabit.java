package org.backendcompas.modules.profile.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = """
                The student's primary eating habit:
                - `COOKING` — Prepares meals at home
                - `CANTEEN` — Eats at the university or workplace canteen
                - `DELIVERY` — Orders food via delivery apps
                - `EATING_OUT` — Dines at restaurants or fast-food
                """
)
public enum EatingHabit {
    COOKING,
    CANTEEN,
    DELIVERY,
    EATING_OUT
}
