package org.main.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "matches", schema = "raw")
public class MatchEntity {

    @Id
    @Column(name = "match_id", nullable = false)
    private String matchId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "region", nullable = false, columnDefinition = "region_route")
    private RegionRoute region;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", columnDefinition = "platform_shard")
    private PlatformShard platform;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_match_json", columnDefinition = "jsonb")
    private String rawMatchJson;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;


    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public RegionRoute getRegion() {
        return region;
    }

    public void setRegion(RegionRoute region) {
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
