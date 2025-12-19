package org.main.persistence.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "summoners",
        schema = "raw",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"platform", "puuid"})
        }
)
public class SummonerEntity {

    @Id
    @Column(name = "summoner_id", nullable = false)
    private String summonerId;

    @Column(name = "puuid", nullable = false)
    private String puuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private PlatformShard platform; // EUW1, EUN1, RU, TR

    @Column(name = "name")
    private String name;

    @Column(name = "summoner_level")
    private Integer summonerLevel;

    @Column(name = "profile_icon_id")
    private Integer profileIconId;

    @Column(name = "revision_date_ms")
    private Long revisionDateMs;

    @Column(name = "last_synced_at")
    private OffsetDateTime lastSyncedAt;

    // ===== getters / setters =====

    public String getSummonerId() {
        return summonerId;
    }

    public void setSummonerId(String summonerId) {
        this.summonerId = summonerId;
    }

    public String getPuuid() {
        return puuid;
    }

    public void setPuuid(String puuid) {
        this.puuid = puuid;
    }

    public PlatformShard getPlatform() {
        return platform;
    }

    public void setPlatform(PlatformShard platform) {
        this.platform = platform;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSummonerLevel() {
        return summonerLevel;
    }

    public void setSummonerLevel(Integer summonerLevel) {
        this.summonerLevel = summonerLevel;
    }

    public Integer getProfileIconId() {
        return profileIconId;
    }

    public void setProfileIconId(Integer profileIconId) {
        this.profileIconId = profileIconId;
    }

    public Long getRevisionDateMs() {
        return revisionDateMs;
    }

    public void setRevisionDateMs(Long revisionDateMs) {
        this.revisionDateMs = revisionDateMs;
    }

    public OffsetDateTime getLastSyncedAt() {
        return lastSyncedAt;
    }

    public void setLastSyncedAt(OffsetDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }
}
