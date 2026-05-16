package org.backendcompas.modules.profile.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = """
                The type of accommodation where the student currently lives:
                - `DORMITORY` — University dormitory (Cămin)
                - `RENT` — Rented apartment or room (Chirie)
                - `OWN_HOME` — Own home or living with parents (Acasă / Cu părinții)
                - `COMMUTER` — Commuter who travels from home each day (Navetist)
                """
)
public enum LivingArea {
    DORMITORY,
    RENT,
    OWN_HOME,
    COMMUTER
}
