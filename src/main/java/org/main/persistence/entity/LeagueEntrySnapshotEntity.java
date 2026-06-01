package org.main.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "league_entry_snapshots", schema = "raw")
public class LeagueEntrySnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "puuid", nullable = false)
    private String puuid;

    @Column(name = "summoner_id")
    private String summonerId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, columnDefinition = "public.platform_shard")
    private PlatformShard platform;

    @Column(name = "queue_type", nullable = false)
    private String queueType;

    @Column(name = "tier")
    private String tier;

    @Column(name = "rank_value")
    private String rankValue;

    @Column(name = "league_points")
    private Integer leaguePoints;

    @Column(name = "wins")
    private Integer wins;

    @Column(name = "losses")
    private Integer losses;

    @Column(name = "hot_streak")
    private Boolean hotStreak;

    @Column(name = "veteran")
    private Boolean veteran;

    @Column(name = "fresh_blood")
    private Boolean freshBlood;

    @Column(name = "inactive")
    private Boolean inactive;

    @Column(name = "synced_at", nullable = false)
    private OffsetDateTime syncedAt;

    public Long getId() {
        return id;
    }

    public String getPuuid() {
        return puuid;
    }

    public void setPuuid(String puuid) {
        this.puuid = puuid;
    }

    public String getSummonerId() {
        return summonerId;
    }

    public void setSummonerId(String summonerId) {
        this.summonerId = summonerId;
    }

    public PlatformShard getPlatform() {
        return platform;
    }

    public void setPlatform(PlatformShard platform) {
        this.platform = platform;
    }

    public String getQueueType() {
        return queueType;
    }

    public void setQueueType(String queueType) {
        this.queueType = queueType;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getRankValue() {
        return rankValue;
    }

    public void setRankValue(String rankValue) {
        this.rankValue = rankValue;
    }

    public Integer getLeaguePoints() {
        return leaguePoints;
    }

    public void setLeaguePoints(Integer leaguePoints) {
        this.leaguePoints = leaguePoints;
    }

    public Integer getWins() {
        return wins;
    }

    public void setWins(Integer wins) {
        this.wins = wins;
    }

    public Integer getLosses() {
        return losses;
    }

    public void setLosses(Integer losses) {
        this.losses = losses;
    }

    public Boolean getHotStreak() {
        return hotStreak;
    }

    public void setHotStreak(Boolean hotStreak) {
        this.hotStreak = hotStreak;
    }

    public Boolean getVeteran() {
        return veteran;
    }

    public void setVeteran(Boolean veteran) {
        this.veteran = veteran;
    }

    public Boolean getFreshBlood() {
        return freshBlood;
    }

    public void setFreshBlood(Boolean freshBlood) {
        this.freshBlood = freshBlood;
    }

    public Boolean getInactive() {
        return inactive;
    }

    public void setInactive(Boolean inactive) {
        this.inactive = inactive;
    }

    public OffsetDateTime getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(OffsetDateTime syncedAt) {
        this.syncedAt = syncedAt;
    }
}