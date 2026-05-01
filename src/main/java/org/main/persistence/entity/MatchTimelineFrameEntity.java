package org.main.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@IdClass(MatchTimelineFrameId.class)
@Table(name = "match_timeline_frames", schema = "raw")
public class MatchTimelineFrameEntity {

    @Id
    @Column(name = "match_id", nullable = false)
    private String matchId;

    @Id
    @Column(name = "frame_no", nullable = false)
    private Integer frameNo;

    @Column(name = "timestamp_ms")
    private Long timestampMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "participant_frames", columnDefinition = "jsonb")
    private String participantFrames;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_frame_json", columnDefinition = "jsonb")
    private String rawFrameJson;

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public Integer getFrameNo() {
        return frameNo;
    }

    public void setFrameNo(Integer frameNo) {
        this.frameNo = frameNo;
    }

    public Long getTimestampMs() {
        return timestampMs;
    }

    public void setTimestampMs(Long timestampMs) {
        this.timestampMs = timestampMs;
    }

    public String getParticipantFrames() {
        return participantFrames;
    }

    public void setParticipantFrames(String participantFrames) {
        this.participantFrames = participantFrames;
    }

    public String getRawFrameJson() {
        return rawFrameJson;
    }

    public void setRawFrameJson(String rawFrameJson) {
        this.rawFrameJson = rawFrameJson;
    }
}