package org.main.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "match_timeline_events", schema = "raw")
public class MatchTimelineEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "match_id", nullable = false)
    private String matchId;

    @Column(name = "frame_no")
    private Integer frameNo;

    @Column(name = "ts_ms")
    private Long tsMs;

    @Column(name = "type")
    private String type;

    @Column(name = "participant_id")
    private Short participantId;

    @Column(name = "killer_id")
    private Short killerId;

    @Column(name = "victim_id")
    private Short victimId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "assisting_participant_ids", columnDefinition = "jsonb")
    private String assistingParticipantIds;

    @Column(name = "item_id")
    private Integer itemId;

    @Column(name = "skill_slot")
    private Short skillSlot;

    @Column(name = "level_up_type")
    private String levelUpType;

    @Column(name = "ward_type")
    private String wardType;

    @Column(name = "building_type")
    private String buildingType;

    @Column(name = "lane_type")
    private String laneType;

    @Column(name = "bounty")
    private Integer bounty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "position", columnDefinition = "jsonb")
    private String position;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "other_json", columnDefinition = "jsonb")
    private String otherJson;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

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

    public Long getTsMs() {
        return tsMs;
    }

    public void setTsMs(Long tsMs) {
        this.tsMs = tsMs;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Short getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Short participantId) {
        this.participantId = participantId;
    }

    public Short getKillerId() {
        return killerId;
    }

    public void setKillerId(Short killerId) {
        this.killerId = killerId;
    }

    public Short getVictimId() {
        return victimId;
    }

    public void setVictimId(Short victimId) {
        this.victimId = victimId;
    }

    public String getAssistingParticipantIds() {
        return assistingParticipantIds;
    }

    public void setAssistingParticipantIds(String assistingParticipantIds) {
        this.assistingParticipantIds = assistingParticipantIds;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Short getSkillSlot() {
        return skillSlot;
    }

    public void setSkillSlot(Short skillSlot) {
        this.skillSlot = skillSlot;
    }

    public String getLevelUpType() {
        return levelUpType;
    }

    public void setLevelUpType(String levelUpType) {
        this.levelUpType = levelUpType;
    }

    public String getWardType() {
        return wardType;
    }

    public void setWardType(String wardType) {
        this.wardType = wardType;
    }

    public String getBuildingType() {
        return buildingType;
    }

    public void setBuildingType(String buildingType) {
        this.buildingType = buildingType;
    }

    public String getLaneType() {
        return laneType;
    }

    public void setLaneType(String laneType) {
        this.laneType = laneType;
    }

    public Integer getBounty() {
        return bounty;
    }

    public void setBounty(Integer bounty) {
        this.bounty = bounty;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getOtherJson() {
        return otherJson;
    }

    public void setOtherJson(String otherJson) {
        this.otherJson = otherJson;
    }
}