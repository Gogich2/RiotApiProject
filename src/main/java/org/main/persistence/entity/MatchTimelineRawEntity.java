package org.main.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "match_timeline_raw", schema = "raw")
public class MatchTimelineRawEntity {

    @Id
    @Column(name = "match_id", nullable = false)
    private String matchId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_timeline_json", columnDefinition = "jsonb")
    private String rawTimelineJson;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getRawTimelineJson() {
        return rawTimelineJson;
    }

    public void setRawTimelineJson(String rawTimelineJson) {
        this.rawTimelineJson = rawTimelineJson;
    }

    public OffsetDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(OffsetDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}