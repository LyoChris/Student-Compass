package org.backendcompas.modules.deals.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class RadarVoteId implements Serializable {

    private UUID userId;
    private UUID deal;

    public RadarVoteId() {}

    public RadarVoteId(UUID userId, UUID deal) {
        this.userId = userId;
        this.deal = deal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RadarVoteId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(deal, that.deal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, deal);
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getDeal() { return deal; }
    public void setDeal(UUID deal) { this.deal = deal; }
}
