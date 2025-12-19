package org.main.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "matches", schema = "raw")
public class MatchEntity {

    @Id
    @Column(name = "match_id", nullable = false)
    private String matchId;

    @Column(name = "region", nullable = false)
    private String region; // europe

    @Enumerated(EnumType.STRING)
    @Column(name = "platform")
    private PlatformShard platform; // EUW1, EUN1, etc

    @Column(name = "raw_match_json", columnDefinition = "jsonb")
    private String rawMatchJson;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    // ===== getters / setters =====

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public PlatformShard getPlatform() {
        return platform;
    }

    public void setPlatform(PlatformShard platform) {
        this.platform = platform;
    }

    public String getRawMatchJson() {
        return rawMatchJson;
    }

    public void setRawMatchJson(String rawMatchJson) {
        this.rawMatchJson = rawMatchJson;
    }

    public OffsetDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(OffsetDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}
