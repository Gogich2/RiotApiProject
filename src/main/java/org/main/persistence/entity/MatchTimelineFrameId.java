package org.main.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

public class MatchTimelineFrameId implements Serializable {

    private String matchId;

    private Integer frameNo;

    public MatchTimelineFrameId() {
    }

    public MatchTimelineFrameId(String matchId, Integer frameNo) {
        this.matchId = matchId;
        this.frameNo = frameNo;
    }

    public String getMatchId() {
        return matchId;
    }

    public Integer getFrameNo() {
        return frameNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MatchTimelineFrameId that)) {
            return false;
        }
        return Objects.equals(matchId, that.matchId)
                && Objects.equals(frameNo, that.frameNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchId, frameNo);
    }
}