package org.main.account.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.main.account.entity.UserSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, UUID> {

    @Query("""
            select session from UserSessionEntity session
            where session.tokenHash = :tokenHash
              and session.revokedAt is null
              and session.expiresAt > :now
            """)
    Optional<UserSessionEntity> findActiveByTokenHash(
            @Param("tokenHash") String tokenHash,
            @Param("now") OffsetDateTime now
    );

    @Modifying
    @Query("""
            update UserSessionEntity session
            set session.revokedAt = :now
            where session.userId = :userId
              and session.revokedAt is null
            """)
    int revokeAllByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
}
