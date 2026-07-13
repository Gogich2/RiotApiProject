package org.main.refresh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "player_refresh_job", schema = "app")
public class PlayerRefreshJobEntity {

    @Id
    private UUID id;

    @Column(name = "puuid", nullable = false, length = 128)
    private String puuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private RefreshSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 16)
    private RefreshState state;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "retry_after")
    private OffsetDateTime retryAfter;

    @Column(name = "failure_category", length = 64)
    private String failureCategory;

    @Column(name = "user_message", length = 240)
    private String userMessage;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPuuid() {
        return puuid;
    }

    public void setPuuid(String puuid) {
        this.puuid = puuid;
    }

    public RefreshSource getSource() {
        return source;
    }

    public void setSource(RefreshSource source) {
        this.source = source;
    }

    public RefreshState getState() {
        return state;
    }

    public void setState(RefreshState state) {
        this.state = state;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(OffsetDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public OffsetDateTime getRetryAfter() {
        return retryAfter;
    }

    public void setRetryAfter(OffsetDateTime retryAfter) {
        this.retryAfter = retryAfter;
    }

    public String getFailureCategory() {
        return failureCategory;
    }

    public void setFailureCategory(String failureCategory) {
        this.failureCategory = failureCategory;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }
}
