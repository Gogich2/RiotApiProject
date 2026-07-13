package org.main.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "saved_profile", schema = "app")
public class SavedProfileEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "puuid", nullable = false, length = 128)
    private String puuid;

    @Column(name = "personal_label", length = 80)
    private String personalLabel;

    @Column(name = "is_default", nullable = false)
    private boolean defaultProfile;

    @Column(name = "saved_at", nullable = false)
    private OffsetDateTime savedAt;

    @Column(name = "last_viewed_at", nullable = false)
    private OffsetDateTime lastViewedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getPuuid() {
        return puuid;
    }

    public void setPuuid(String puuid) {
        this.puuid = puuid;
    }

    public String getPersonalLabel() {
        return personalLabel;
    }

    public void setPersonalLabel(String personalLabel) {
        this.personalLabel = personalLabel;
    }

    public boolean isDefault() {
        return defaultProfile;
    }

    public void setDefault(boolean defaultProfile) {
        this.defaultProfile = defaultProfile;
    }

    public OffsetDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(OffsetDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public OffsetDateTime getLastViewedAt() {
        return lastViewedAt;
    }

    public void setLastViewedAt(OffsetDateTime lastViewedAt) {
        this.lastViewedAt = lastViewedAt;
    }
}
