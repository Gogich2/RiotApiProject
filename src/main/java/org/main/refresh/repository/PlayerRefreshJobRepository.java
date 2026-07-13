package org.main.refresh.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.main.refresh.entity.PlayerRefreshJobEntity;
import org.main.refresh.entity.RefreshSource;
import org.main.refresh.entity.RefreshState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRefreshJobRepository extends JpaRepository<PlayerRefreshJobEntity, UUID> {

    @Modifying
    @Query(value = """
            insert into app.player_refresh_job (id, puuid, source, state, requested_at)
            values (:id, :puuid, :source, 'QUEUED', :requestedAt)
            on conflict (puuid) where state in ('QUEUED', 'RUNNING') do nothing
            """, nativeQuery = true)
    int insertQueued(
            @Param("id") UUID id,
            @Param("puuid") String puuid,
            @Param("source") String source,
            @Param("requestedAt") OffsetDateTime requestedAt
    );

    Optional<PlayerRefreshJobEntity> findFirstByPuuidAndStateInOrderByRequestedAtDesc(
            String puuid,
            Collection<RefreshState> states
    );

    Optional<PlayerRefreshJobEntity> findFirstByPuuidAndSourceOrderByRequestedAtDesc(
            String puuid,
            RefreshSource source
    );

    Optional<PlayerRefreshJobEntity> findFirstByPuuidOrderByRequestedAtDesc(String puuid);
}
