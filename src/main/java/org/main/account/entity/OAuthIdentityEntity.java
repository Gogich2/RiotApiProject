package org.main.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "oauth_identity", schema = "app")
public class OAuthIdentityEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_subject_id", nullable = false, length = 128)
    private String providerSubjectId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_login_at", nullable = false)
    private OffsetDateTime lastLoginAt;

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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderSubjectId() {
        return providerSubjectId;
    }

    public void setProviderSubjectId(String providerSubjectId) {
        this.providerSubjectId = providerSubjectId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(OffsetDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
