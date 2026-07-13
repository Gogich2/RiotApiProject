package org.main.account.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.main.account.entity.SavedProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedProfileRepository extends JpaRepository<SavedProfileEntity, UUID> {

    List<SavedProfileEntity> findByUserIdOrderByLastViewedAtDesc(UUID userId);

    Optional<SavedProfileEntity> findByUserIdAndPuuid(UUID userId, String puuid);

    Optional<SavedProfileEntity> findByIdAndUserId(UUID id, UUID userId);

    @Query(value = """
            select saved.puuid
            from app.saved_profile saved
            where saved.last_viewed_at >= :activeSince
              and not exists (
                  select 1 from app.player_refresh_job job
                  where job.puuid = saved.puuid
                    and job.state = 'COMPLETED'
                    and job.completed_at >= :freshSince
              )
            group by saved.puuid
            order by max(saved.last_viewed_at) desc
            limit :batchSize
            """, nativeQuery = true)
    List<String> findEligibleForScheduledRefresh(
            @Param("activeSince") java.time.OffsetDateTime activeSince,
            @Param("freshSince") java.time.OffsetDateTime freshSince,
            @Param("batchSize") int batchSize
    );
}
